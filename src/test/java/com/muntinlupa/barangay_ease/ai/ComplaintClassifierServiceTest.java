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
}
