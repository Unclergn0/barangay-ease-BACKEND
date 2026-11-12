
package com.muntinlupa.barangay_ease.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ComplaintClassifierService {

    private static final Logger log =
            LoggerFactory.getLogger(ComplaintClassifierService.class);

    private final LlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public ComplaintClassifierService(
            LlmGateway llmGateway,
            ObjectMapper objectMapper) {

        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * Classify both the resident-provided title and description.
     */
    public Map<String, Object> classify(String title, String description) {

        StringBuilder combined = new StringBuilder();

        if (title != null && !title.isBlank()) {
            combined.append("Title: ").append(title.trim());
        }

        if (description != null && !description.isBlank()) {
            if (combined.length() > 0) {
                combined.append("\n");
            }
            combined.append("Description: ").append(description.trim());
        }

        return classify(combined.toString());
    }

    /**
     * Backward-compatible entry point.
     */
    public Map<String, Object> classify(String text) {

        String complaint = text == null ? "" : text.trim();

        Map<String, Object> result = new LinkedHashMap<>();

        if (complaint.isBlank()) {
            result.put("topic", "Other");
            result.put("category", "mild");
            result.put("priority", "mild");
            result.put("message",
                    "Please describe your concern so barangay personnel can assist you.");
            result.put("route", "BARANGAY_PERSONNEL");
            result.put("aiUsed", false);
            result.put("aiFallback", false);
            return result;
        }

        // Independent emergency screening.
        // This works even when the AI provider is unavailable.
        EmergencyKeywordScreen.Result screen =
                EmergencyKeywordScreen.screen(complaint);

        String safetyPriority = getSafetyPriority(screen);

        // Safe default if AI is unavailable or fails.
        result.put("topic", "Other");
        result.put("category", safetyPriority);
        result.put("priority", safetyPriority);
        result.put("reason", "Pending barangay personnel assessment.");
        result.put("route", getRoute(safetyPriority));
        result.put("message",
                getResidentMessage(screen, safetyPriority, "Other"));
        result.put("aiUsed", false);

        // AI is optional. The complaint can still be processed without it.
        if (!llmGateway.isConfigured()) {
            result.put("aiFallback", true);
            return result;
        }

        try {

            String systemPrompt = """
                    You are a preliminary complaint classification assistant
                    for a Philippine barangay service system.

                    Residents may write in:
                    - English
                    - Filipino / Tagalog
                    - Taglish
                    - Informal language, spelling variations, or mixed languages

                    Understand the meaning of the complaint, not just exact keywords.

                    CLASSIFICATION TASK:

                    1. Identify the main complaint topic.
                    2. Assign a priority.
                    3. Give a short explanation.
                    4. Suggest what barangay personnel should initially check.

                    ALLOWED TOPICS:
                    - Noise Complaint
                    - Waste Management
                    - Road and Infrastructure
                    - Public Safety
                    - Health and Sanitation
                    - Animal Concern
                    - Water and Drainage
                    - Neighbor Dispute
                    - Public Disturbance
                    - Other

                    ALLOWED PRIORITIES:

                    mild:
                    Ordinary concerns that do not indicate immediate danger
                    or urgent harm.

                    immediate:
                    Urgent concerns requiring prompt attention, but without
                    clear evidence of an immediate life-threatening emergency.

                    critical:
                    Reports involving serious violence, sexual assault,
                    serious abuse, imminent threats, self-harm, suicide,
                    or immediate danger to life or safety.

                    IMPORTANT SAFETY RULES:

                    - Never downgrade a serious emergency.
                    - Do not assume an allegation is proven.
                    - Do not invent facts, names, suspects, or evidence.
                    - Do not provide a legal or medical diagnosis.
                    - A historical event is not automatically an ongoing emergency.
                    - Consider the actual meaning and context of Tagalog and Taglish.
                    - If the report is unclear, use Other and mild unless the text
                      indicates an urgent safety concern.
                    - Do not treat a normal disagreement as a critical emergency.
                    - Ordinary noise or videoke complaints should normally be mild
                      unless the report describes a separate safety threat.
                    - Do not tell someone in immediate danger to wait for an AI response.

                    OUTPUT:
                    Return ONLY valid JSON. Do not use Markdown fences.

                    Required JSON structure:
                    {
                      "topic": "one allowed topic",
                      "priority": "mild, immediate, or critical",
                      "reason": "short explanation",
                      "staffNote": "short preliminary action for barangay personnel"
                    }
                    """;

            String aiResponse = llmGateway.complete(
                    systemPrompt,
                    complaint,
                    0.1
            );

            JsonNode json = parseJson(aiResponse);

            if (json == null) {
                log.warn("Complaint AI returned an invalid JSON response.");
                result.put("aiFallback", true);
                return result;
            }

            String topic = readText(json, "topic");
            String aiPriority = normalizePriority(
                    readText(json, "priority"));

            String reason = readText(json, "reason");
            String staffNote = readText(json, "staffNote");

            if (topic == null || aiPriority == null) {
                log.warn("Complaint AI response is missing required fields.");
                result.put("aiFallback", true);
                return result;
            }

            // AI may escalate the safety-screen result.
            // AI must never downgrade the independent safety result.
            String finalPriority = higherPriority(
                    safetyPriority,
                    aiPriority
            );

            String normalizedTopic = normalizeTopic(topic);

            result.put("topic", normalizedTopic);
            result.put("category", finalPriority);
            result.put("priority", finalPriority);

            result.put(
                    "reason",
                    reason == null
                            ? "Complaint classified for preliminary review."
                            : reason
            );

            result.put(
                    "staffNote",
                    staffNote == null
                            ? "Please review the complaint details."
                            : staffNote
            );

            result.put("route", getRoute(finalPriority));
            result.put("message",
                    getResidentMessage(screen, finalPriority, normalizedTopic));

            result.put("aiUsed", true);
            result.put("aiFallback", false);

        } catch (Exception error) {

            log.error(
                    "Complaint AI classification failed. Using independent safety screening. Reason: {}",
                    error.getMessage()
            );

            // Keep the independently screened priority and message.
            result.put("aiUsed", false);
            result.put("aiFallback", true);
        }

        return result;
    }

    // =====================================================
    // RESIDENT MESSAGE
    // =====================================================

    private String getResidentMessage(
            EmergencyKeywordScreen.Result screen,
            String priority,
            String topic) {

        // Use the specific emergency message when keyword screening
        // actually detected an incident at the matching priority.
        if (screen != null && screen.flagged()) {

            if ("critical".equals(priority) && screen.critical()) {
                return SafetyMessages.forScreen(screen);
            }

            if ("immediate".equals(priority) && screen.immediate()) {
                return SafetyMessages.forScreen(screen);
            }
        }

        // Generic messages for AI escalation without a matching
        // emergency keyword-screen result.
        if ("critical".equals(priority)) {
            return "Your complaint has been classified as critical and requires urgent human review. If anyone is in immediate danger, contact 911 or emergency responders now. Do not wait for an online response.";
        }

        if ("immediate".equals(priority)) {
            return "Your complaint requires prompt attention from barangay personnel. If anyone is in immediate danger, contact 911 or emergency responders now.";
        }

        return "Your complaint has been classified as "
                + topic
                + " and will be forwarded to barangay personnel for appropriate action.";
    }

    // =====================================================
    // PARSE AI JSON
    // =====================================================

    private JsonNode parseJson(String response) throws Exception {

        if (response == null || response.isBlank()) {
            return null;
        }

        String cleaned = response.trim();

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst(
                    "^```(?:json)?\\s*",
                    "");

            cleaned = cleaned.replaceFirst(
                    "\\s*```$",
                    "");
        }

        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace < 0 || lastBrace <= firstBrace) {
            return null;
        }

        String jsonText = cleaned.substring(
                firstBrace,
                lastBrace + 1
        );

        JsonNode node = objectMapper.readTree(jsonText);

        return node != null && node.isObject()
                ? node
                : null;
    }

    // =====================================================
    // READ JSON STRING
    // =====================================================

    private String readText(JsonNode json, String field) {

        JsonNode value = json.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText("").trim();

        return text.isBlank() ? null : text;
    }

    // =====================================================
    // NORMALIZE PRIORITY
    // =====================================================

    private String normalizePriority(String value) {

        if (value == null) {
            return null;
        }

        String priority = value.trim().toLowerCase(Locale.ROOT);

        if (Set.of("mild", "immediate", "critical").contains(priority)) {
            return priority;
        }

        return null;
    }

    // =====================================================
    // NORMALIZE TOPIC
    // =====================================================

    private String normalizeTopic(String value) {

        if (value == null || value.isBlank()) {
            return "Other";
        }

        String topic = value.trim();

        Set<String> allowedTopics = Set.of(
                "Noise Complaint",
                "Waste Management",
                "Road and Infrastructure",
                "Public Safety",
                "Health and Sanitation",
                "Animal Concern",
                "Water and Drainage",
                "Neighbor Dispute",
                "Public Disturbance",
                "Other"
        );

        for (String allowed : allowedTopics) {
            if (allowed.equalsIgnoreCase(topic)) {
                return allowed;
            }
        }

        return "Other";
    }

    // =====================================================
    // SAFETY PRIORITY
    // =====================================================

    private String getSafetyPriority(
            EmergencyKeywordScreen.Result screen) {

        if (screen == null) {
            return "mild";
        }

        if (screen.level() == EmergencyKeywordScreen.Level.CRITICAL
                || screen.selfHarm()) {
            return "critical";
        }

        if (screen.level() == EmergencyKeywordScreen.Level.IMMEDIATE) {
            return "immediate";
        }

        return "mild";
    }

    // =====================================================
    // PRIORITY ESCALATION
    // =====================================================

    private String higherPriority(
            String safetyPriority,
            String aiPriority) {

        int safety = priorityLevel(safetyPriority);
        int ai = priorityLevel(aiPriority);

        return ai > safety
                ? aiPriority
                : safetyPriority;
    }

    private int priorityLevel(String priority) {

        if ("critical".equals(priority)) {
            return 3;
        }

        if ("immediate".equals(priority)) {
            return 2;
        }

        return 1;
    }

    // =====================================================
    // ROUTING
    // =====================================================

    private String getRoute(String priority) {

        if ("critical".equals(priority)) {
            return "AI_INVESTIGATION_AND_HUMAN_REVIEW";
        }

        if ("immediate".equals(priority)) {
            return "PRIORITY_BARANGAY_PERSONNEL";
        }

        return "BARANGAY_PERSONNEL";
    }
}
