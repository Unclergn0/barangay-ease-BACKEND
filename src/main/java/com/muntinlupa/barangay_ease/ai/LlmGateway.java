
package com.muntinlupa.barangay_ease.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmGateway {

    private static final Logger log =
            LoggerFactory.getLogger(LlmGateway.class);

    private static final String GEMINI = "gemini";
    private static final String GROQ = "groq";

    private static final long AUTH_COOLDOWN_MILLIS = 600_000L;
    private static final long CONFIG_COOLDOWN_MILLIS = 60_000L;
    private static final long MAX_RETRY_AFTER_SECONDS = 3600L;

    // Represents one message in the conversation.
    // Supported roles: user and assistant.
    public record ChatTurn(String role, String content) {}

    private static final class Provider {

        final String name;
        final String url;
        final String apiKey;
        final String model;

        volatile long cooldownUntilMillis = 0;

        Provider(
                String name,
                String url,
                String apiKey,
                String model
        ) {
            this.name = name;
            this.url = url == null ? "" : url.trim();
            this.apiKey = normalizeKey(apiKey);
            this.model = model == null ? "" : model.trim();
        }

        boolean isConfigured() {
            return !apiKey.isBlank()
                    && !model.isBlank()
                    && !url.isBlank();
        }

        boolean isAvailable() {
            return isConfigured()
                    && System.currentTimeMillis() >= cooldownUntilMillis;
        }
    }

    private static final class ProviderHttpException extends Exception {

        final int status;
        final String retryAfter;
        final String detail;

        ProviderHttpException(
                int status,
                String retryAfter,
                String detail
        ) {
            super("HTTP " + status + ": " + detail);

            this.status = status;
            this.retryAfter = retryAfter;
            this.detail = detail;
        }
    }

    private final List<Provider> providers = new ArrayList<>();

    private final ObjectMapper mapper;
    private final HttpClient client;
    private final Duration timeout;
    private final long cooldownMillis;

    public LlmGateway(

            ObjectMapper mapper,

            @Value("${app.gemini.url:https://generativelanguage.googleapis.com/v1beta/models}")
            String geminiBaseUrl,

            @Value("${app.gemini.api-key:}")
            String geminiKey,

            @Value("${app.gemini.model:}")
            String geminiModel,

            @Value("${app.groq.url:https://api.groq.com/openai/v1/chat/completions}")
            String groqUrl,

            @Value("${app.groq.api-key:}")
            String groqKey,

            @Value("${app.groq.model:}")
            String groqModel,

            @Value("${app.gemini.connect-timeout-seconds:15}")
            long connectSeconds,

            @Value("${app.gemini.request-timeout-seconds:60}")
            long requestSeconds,

            @Value("${app.ai.cooldown-seconds:120}")
            long cooldownSeconds
    ) {

        this.mapper = mapper;

        this.timeout = Duration.ofSeconds(
                Math.max(5, requestSeconds)
        );

        this.cooldownMillis =
                Math.max(30, cooldownSeconds) * 1000L;

        this.client = HttpClient.newBuilder()
                .connectTimeout(
                        Duration.ofSeconds(
                                Math.max(5, connectSeconds)
                        )
                )
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        String normalizedGeminiBaseUrl =
                removeTrailingSlashes(geminiBaseUrl);

        String normalizedGeminiModel =
                geminiModel == null ? "" : geminiModel.trim();

        String geminiUrl = normalizedGeminiBaseUrl.isBlank()
                || normalizedGeminiModel.isBlank()
                ? ""
                : normalizedGeminiBaseUrl
                    + "/"
                    + normalizedGeminiModel
                    + ":generateContent";

        providers.add(
                new Provider(
                        GEMINI,
                        geminiUrl,
                        geminiKey,
                        geminiModel
                )
        );

        providers.add(
                new Provider(
                        GROQ,
                        groqUrl,
                        groqKey,
                        groqModel
                )
        );
    }

    private static String normalizeKey(String key) {

        if (key == null) {
            return "";
        }

        String value = key.trim();

        if (value.length() >= 2) {

            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);

            if ((first == '"' && last == '"')
                    || (first == '\'' && last == '\'')) {

                value = value.substring(
                        1,
                        value.length() - 1
                ).trim();
            }
        }

        return value;
    }

    private static String removeTrailingSlashes(String value) {

        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("/+$", "");
    }

    @PostConstruct
    public void reportConfiguration() {

        for (Provider provider : providers) {

            if (provider.isConfigured()) {

                log.info(
                        "AI provider '{}' configured. Model: {}",
                        provider.name,
                        provider.model
                );

            } else {

                log.warn(
                        "AI provider '{}' is not fully configured.",
                        provider.name
                );
            }
        }
    }

    public boolean isConfigured() {

        return providers.stream()
                .anyMatch(Provider::isConfigured);
    }

    // =====================================================
    // BACKWARD-COMPATIBLE METHOD
    // =====================================================

    // Existing code can still call complete() with one message.
    public String complete(
            String systemPrompt,
            String userMessage,
            double temperature
    ) {

        return complete(
                systemPrompt,
                List.of(
                        new ChatTurn(
                                "user",
                                userMessage == null ? "" : userMessage
                        )
                ),
                temperature
        );
    }

    // =====================================================
    // CONVERSATION HISTORY SUPPORT
    // =====================================================

    public String complete(
            String systemPrompt,
            List<ChatTurn> conversationHistory,
            double temperature
    ) {

        if (!isConfigured()) {

            throw new AiUnavailableException(
                    "No AI provider is properly configured. "
                            + "Check API keys and model settings.",
                    true
            );
        }

        String safeSystemPrompt =
                systemPrompt == null ? "" : systemPrompt;

        double safeTemperature =
                Math.max(0.0, Math.min(2.0, temperature));

        List<ChatTurn> safeHistory = conversationHistory == null
                ? List.of()
                : conversationHistory.stream()
                        .filter(turn -> turn != null
                                && turn.content() != null
                                && !turn.content().isBlank())
                        .filter(turn -> "user".equalsIgnoreCase(turn.role())
                                || "assistant".equalsIgnoreCase(turn.role()))
                        .map(turn -> new ChatTurn(
                                turn.role().toLowerCase(),
                                turn.content()
                        ))
                        .toList();

        if (safeHistory.isEmpty()) {

            throw new IllegalArgumentException(
                    "Conversation history cannot be empty."
            );
        }

        for (Provider provider : providers) {

            if (!provider.isAvailable()) {
                continue;
            }

            try {

                log.info(
                        "Sending AI request to provider: {} with {} history messages",
                        provider.name,
                        safeHistory.size()
                );

                String answer;

                if (GEMINI.equals(provider.name)) {

                    answer = callGemini(
                            provider,
                            safeSystemPrompt,
                            safeHistory,
                            safeTemperature
                    );

                } else if (GROQ.equals(provider.name)) {

                    answer = callGroq(
                            provider,
                            safeSystemPrompt,
                            safeHistory,
                            safeTemperature
                    );

                } else {

                    continue;
                }

                if (answer != null && !answer.isBlank()) {

                    log.info(
                            "AI response successfully received from {}.",
                            provider.name
                    );

                    return answer.trim();
                }

                log.warn(
                        "Provider '{}' returned an empty response.",
                        provider.name
                );

            } catch (ProviderHttpException e) {

                handleHttpFailure(provider, e);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new AiUnavailableException(
                        "AI request was interrupted.",
                        false
                );

            } catch (Exception e) {

                log.error(
                        "Provider '{}' request failed: {}",
                        provider.name,
                        safeLogMessage(e.getMessage())
                );
            }

            log.warn(
                    "Provider '{}' unavailable. Trying next provider.",
                    provider.name
            );
        }

        throw new AiUnavailableException(
                "All configured AI providers failed. "
                        + "Check API credentials, model configuration, "
                        + "quota and network connection.",
                false
        );
    }

    // =====================================================
    // GEMINI API
    // =====================================================

    private String callGemini(
            Provider provider,
            String systemPrompt,
            List<ChatTurn> conversationHistory,
            double temperature
    ) throws Exception {

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put(
                "systemInstruction",
                Map.of(
                        "parts",
                        List.of(
                                Map.of("text", systemPrompt)
                        )
                )
        );

        List<Map<String, Object>> contents = new ArrayList<>();

        for (ChatTurn turn : conversationHistory) {

            String role = "assistant".equalsIgnoreCase(turn.role())
                    ? "model"
                    : "user";

            contents.add(
                    Map.of(
                            "role", role,
                            "parts",
                            List.of(
                                    Map.of("text", turn.content())
                            )
                    )
            );
        }

        payload.put("contents", contents);

        payload.put(
                "generationConfig",
                Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", 1200
                )
        );

        String json = mapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(provider.url))
                .timeout(timeout)
                .header("x-goog-api-key", provider.apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        checkResponse(response);

        JsonNode root = mapper.readTree(response.body());

        StringBuilder answer = new StringBuilder();

        for (JsonNode candidate : root.path("candidates")) {

            for (JsonNode part :
                    candidate.path("content").path("parts")) {

                if (part.hasNonNull("text")) {
                    answer.append(part.path("text").asText());
                }
            }
        }

        return answer.toString();
    }

    // =====================================================
    // GROQ API
    // =====================================================

    private String callGroq(
            Provider provider,
            String systemPrompt,
            List<ChatTurn> conversationHistory,
            double temperature
    ) throws Exception {

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("model", provider.model);
        payload.put("temperature", temperature);
        payload.put("max_tokens", 1200);

        List<Map<String, Object>> messages = new ArrayList<>();

        messages.add(
                Map.of(
                        "role", "system",
                        "content", systemPrompt
                )
        );

        for (ChatTurn turn : conversationHistory) {

            messages.add(
                    Map.of(
                            "role", turn.role().toLowerCase(),
                            "content", turn.content()
                    )
            );
        }

        payload.put("messages", messages);

        String json = mapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(provider.url))
                .timeout(timeout)
                .header(
                        "Authorization",
                        "Bearer " + provider.apiKey
                )
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        checkResponse(response);

        JsonNode root = mapper.readTree(response.body());

        return root.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText("");
    }

    // =====================================================
    // HTTP RESPONSE VALIDATION
    // =====================================================

    private void checkResponse(
            HttpResponse<String> response
    ) throws ProviderHttpException {

        int status = response.statusCode();

        if (status >= 200 && status < 300) {
            return;
        }

        String detail = extractError(response.body());

        throw new ProviderHttpException(
                status,
                response.headers()
                        .firstValue("retry-after")
                        .orElse(null),
                detail
        );
    }

    private String extractError(String body) {

        if (body == null || body.isBlank()) {
            return "Empty provider error response.";
        }

        try {

            JsonNode root = mapper.readTree(body);
            JsonNode error = root.path("error");

            String message = error.path("message").asText("");

            if (message.isBlank()) {
                message = error.path("status").asText("");
            }

            if (message.isBlank()) {
                message = root.path("message").asText("");
            }

            if (!message.isBlank()) {

                String cleaned = message.replaceAll(
                        "[\\r\\n\\t]",
                        " "
                );

                return cleaned.length() > 250
                        ? cleaned.substring(0, 250)
                        : cleaned;
            }

        } catch (Exception ignored) {
            // Do not expose raw provider response.
        }

        return "Provider rejected the request. "
                + "Check provider configuration.";
    }

    private String safeLogMessage(String message) {

        if (message == null || message.isBlank()) {
            return "Unknown error.";
        }

        String cleaned = message.replaceAll(
                "[\\r\\n\\t]",
                " "
        );

        return cleaned.length() > 250
                ? cleaned.substring(0, 250)
                : cleaned;
    }

    // =====================================================
    // PROVIDER FAILURE HANDLING
    // =====================================================

    private void handleHttpFailure(
            Provider provider,
            ProviderHttpException e
    ) {

        int status = e.status;

        if (status == 401) {

            provider.cooldownUntilMillis =
                    System.currentTimeMillis()
                            + AUTH_COOLDOWN_MILLIS;

            log.error(
                    "Provider '{}' rejected its API key (401). Detail: {}",
                    provider.name,
                    e.detail
            );

        } else if (status == 403) {

            provider.cooldownUntilMillis =
                    System.currentTimeMillis()
                            + AUTH_COOLDOWN_MILLIS;

            log.error(
                    "Provider '{}' denied API access (403). Detail: {}",
                    provider.name,
                    e.detail
            );

        } else if (status == 429) {

            long cooldown = getRetryAfterMillis(
                    e.retryAfter
            );

            provider.cooldownUntilMillis =
                    System.currentTimeMillis() + cooldown;

            log.warn(
                    "Provider '{}' rate limited (429). Retry after {} seconds.",
                    provider.name,
                    cooldown / 1000
            );

        } else if (status == 400 || status == 404) {

            provider.cooldownUntilMillis =
                    System.currentTimeMillis()
                            + CONFIG_COOLDOWN_MILLIS;

            log.error(
                    "Provider '{}' configuration/request error ({}). Detail: {}",
                    provider.name,
                    status,
                    e.detail
            );

        } else {

            log.error(
                    "Provider '{}' returned HTTP {}. Detail: {}",
                    provider.name,
                    status,
                    e.detail
            );
        }
    }

    private long getRetryAfterMillis(String retryAfter) {

        if (retryAfter == null || retryAfter.isBlank()) {
            return cooldownMillis;
        }

        try {

            long seconds = Long.parseLong(retryAfter.trim());

            seconds = Math.max(1, seconds);

            seconds = Math.min(
                    seconds,
                    MAX_RETRY_AFTER_SECONDS
            );

            return seconds * 1000L;

        } catch (NumberFormatException ignored) {

            return cooldownMillis;
        }
    }
}
