package com.muntinlupa.barangay_ease.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class KnowledgeBase {

    private static final Logger log =
            LoggerFactory.getLogger(KnowledgeBase.class);

    private final JsonNode knowledgeData;

    public KnowledgeBase() {

        JsonNode loadedData;

        try {
            ObjectMapper mapper = new ObjectMapper();

            ClassPathResource resource =
                    new ClassPathResource(
                            "knowledge/barangay-knowledge.json"
                    );

            if (resource.exists()) {

                try (InputStream inputStream =
                             resource.getInputStream()) {

                    loadedData = mapper.readTree(inputStream);
                }

                log.info("Barangay knowledge database loaded successfully.");

            } else {

                log.error("barangay-knowledge.json was not found.");

                loadedData = mapper.createObjectNode();
            }

        } catch (Exception e) {

            log.error(
                    "Could not load barangay knowledge database.",
                    e
            );

            loadedData = new ObjectMapper().createObjectNode();
        }

        this.knowledgeData = loadedData;
    }

    public JsonNode getKnowledgeData() {
        return knowledgeData;
    }

    public JsonNode getKnowledgeForBarangay(String barangay) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode result = mapper.createObjectNode();

        if (knowledgeData == null || knowledgeData.isEmpty()) {
            return result;
        }

        // General system information
        copyField(result, "system_info");
        copyField(result, "global_documents_catalog");
        copyField(result, "city_wide_services");
        copyField(result, "city_wide_emergency_contacts");
        copyField(result, "website_online_service_request_process");

        // Find the exact barangay profile
        JsonNode profiles = knowledgeData.path("barangay_profiles");

        if (profiles.isArray()) {

            for (JsonNode profile : profiles) {

                String profileName =
                        profile.path("name").asText("");

                if (normalize(profileName).equals(normalize(barangay))) {

                    result.set("resident_barangay", profile.deepCopy());

                    return result;
                }
            }
        }

        // Keep a clear record when no matching profile exists
        result.put("requested_barangay", barangay);
        result.put(
                "barangay_profile_status",
                "NO_MATCHING_PROFILE"
        );

        return result;
    }

    private void copyField(
            ObjectNode destination,
            String fieldName
    ) {

        JsonNode value = knowledgeData.get(fieldName);

        if (value != null) {
            destination.set(fieldName, value.deepCopy());
        }
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }
}