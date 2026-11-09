package com.muntinlupa.barangay_ease.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmergencyKeywordScreenTest {

    @Test
    void ordinaryLateNightNoiseIsNotAutomaticallyAnEmergency() {
        EmergencyKeywordScreen.Result result = EmergencyKeywordScreen.screen(
                "Maingay ang kapitbahay at nagvi-videoke hanggang hatinggabi.");

        assertEquals(EmergencyKeywordScreen.Level.NONE, result.level());
        assertFalse(result.selfHarm());
    }

    @Test
    void fireIsFlaggedAsImmediate() {
        EmergencyKeywordScreen.Result result = EmergencyKeywordScreen.screen(
                "May sunog sa katabing bahay.");

        assertEquals(EmergencyKeywordScreen.Level.IMMEDIATE, result.level());
    }

    @Test
    void seriousViolenceIsFlaggedAsCritical() {
        EmergencyKeywordScreen.Result result = EmergencyKeywordScreen.screen(
                "May binubugbog sa loob ng bahay ngayon.");

        assertEquals(EmergencyKeywordScreen.Level.CRITICAL, result.level());
        assertTrue(result.flagged());
    }
}
