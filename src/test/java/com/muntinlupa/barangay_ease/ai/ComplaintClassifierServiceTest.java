package com.muntinlupa.barangay_ease.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComplaintClassifierServiceTest {

    @Test
    void aiCannotEscalateOrdinaryVideokeNoiseToCritical() {
        LlmGateway gateway = mock(LlmGateway.class);
        when(gateway.isConfigured()).thenReturn(true);
        when(gateway.complete(anyString(), anyString(), anyDouble())).thenReturn(
                "{\"topic\":\"Public Disturbance\",\"priority\":\"critical\",\"reason\":\"Late night noise\",\"staffNote\":\"Check the area\"}");

        ComplaintClassifierService classifier = new ComplaintClassifierService(gateway, new ObjectMapper());
        Map<String, Object> result = classifier.classify(
                "Maingay ang kapitbahay at nagvi-videoke hanggang hatinggabi.");

        assertEquals("mild", result.get("priority"));
        assertEquals("Noise Complaint", result.get("topic"));
    }

    @Test
    void noiseReportWithViolenceContextIsNotDowngraded() {
        LlmGateway gateway = mock(LlmGateway.class);
        when(gateway.isConfigured()).thenReturn(true);
        when(gateway.complete(anyString(), anyString(), anyDouble())).thenReturn(
                "{\"topic\":\"Public Safety\",\"priority\":\"critical\",\"reason\":\"Violence reported\",\"staffNote\":\"Urgent review\"}");

        ComplaintClassifierService classifier = new ComplaintClassifierService(gateway, new ObjectMapper());
        Map<String, Object> result = classifier.classify(
                "Maingay ang kapitbahay at may suntukan sa labas.");

        assertEquals("critical", result.get("priority"));
    }
    @Test
    void criticalKeywordInTitleIsDetectedWhenDescriptionIsGeneric() {
        LlmGateway gateway = mock(LlmGateway.class);
        when(gateway.isConfigured()).thenReturn(false);

        ComplaintClassifierService classifier =
                new ComplaintClassifierService(gateway, new ObjectMapper());

        Map<String, Object> result = classifier.classify(
                "rape",
                "please help me");

        assertEquals("critical", result.get("priority"));
        assertEquals("AI_INVESTIGATION_AND_HUMAN_REVIEW", result.get("route"));
        assertEquals(false, result.get("aiUsed"));
        assertEquals(true, result.get("aiFallback"));
    }

    @Test
    void mildTitleAndDescriptionStayInNormalWorkflowWithoutAi() {
        LlmGateway gateway = mock(LlmGateway.class);
        when(gateway.isConfigured()).thenReturn(false);

        ComplaintClassifierService classifier =
                new ComplaintClassifierService(gateway, new ObjectMapper());

        Map<String, Object> result = classifier.classify(
                "Tambak na basura",
                "Hindi nakokolekta ang basura sa phase 2.");

        assertEquals("mild", result.get("priority"));
        assertEquals("BARANGAY_PERSONNEL", result.get("route"));
    }

    @Test
    void immediateKeywordInTitleIsNotRoutedAsCritical() {
        LlmGateway gateway = mock(LlmGateway.class);
        when(gateway.isConfigured()).thenReturn(false);

        ComplaintClassifierService classifier =
                new ComplaintClassifierService(gateway, new ObjectMapper());

        Map<String, Object> result = classifier.classify(
                "Sunog",
                "May nasusunog na bahay sa aming street.");

        assertEquals("immediate", result.get("priority"));
        assertEquals("PRIORITY_BARANGAY_PERSONNEL", result.get("route"));
    }

}
