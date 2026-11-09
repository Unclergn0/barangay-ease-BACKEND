
package com.muntinlupa.barangay_ease.ai;

import java.util.regex.Pattern;

public final class EmergencyKeywordScreen {

    public enum Level {
        NONE,
        IMMEDIATE,
        CRITICAL
    }

    public record Result(Level level, boolean selfHarm) {

        public boolean flagged() {
            return level != Level.NONE || selfHarm;
        }
    }

    private static final int FLAGS =
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static Pattern words(String... alternatives) {

        return Pattern.compile(
                "(?<![\\p{L}\\p{N}])(?:"
                        + String.join("|", alternatives)
                        + ")(?![\\p{L}\\p{N}])",
                FLAGS);
    }

    // =====================================================
    // CRITICAL SAFETY CONCERNS
    // =====================================================

    private static final Pattern CRITICAL = words(
            "rap(?:e|ed|es|ing|ist)",
            "ni-?rape",
            "gin[a-z]*hasa",
            "panggagahasa",
            "gahasa",
            "hinalay",
            "hinahalay",
            "panghahalay",
            "sexual(?:ly)?\\s+(?:assault\\w*|abus\\w*|harass\\w*)",
            "molest\\w*",
            "minolestiya",
            "child\\s+abuse",
            "inabuso",
            "inaabuso",
            "pang-?aabuso",
            "domestic\\s+violence",
            "vawc",
            "binubugbog",
            "binugbog",
            "pambubugbog",
            "sinasaktan\\s+ako\\s+ng\\s+(?:asawa|mister|partner|boyfriend|live-?in|tatay|nanay)",
            "murder\\w*",
            "homicide",
            "pinatay",
            "papatayin",
            "pagpatay",
            "kill(?:ed|ing)?\\s+(?:me|him|her|them|someone)",
            "binaril",
            "pamamaril",
            "shot\\s+dead",
            "gun\\s?shots?",
            "shooting\\s+incident",
            "mass\\s+shooting",
            "sinaksak",
            "pananaksak",
            "stabb(?:ed|ing)",
            "kidnap\\w*",
            "dinukot",
            "hostage",
            "trafficking",
            "armed\\s+robbery",
            "holdap",
            "hold-?up",
            "hinoldap",
            "robbery"
    );

    // =====================================================
    // IMMEDIATE SAFETY CONCERNS
    // =====================================================

    private static final Pattern IMMEDIATE = words(
            "sunog",
            "nasusunog",
            "nasunog",
            "nagliliyab",
            "fire",
            "house\\s+fire",
            "heart\\s+attack",
            "inatake",
            "atake\\s+sa\\s+puso",
            "stroke",
            "nahimatay",
            "hinimatay",
            "walang\\s+malay",
            "unconscious",
            "collapsed",
            "(?:hindi|di)\\s+(?:na\\s+)?(?:humihinga|makahinga)",
            "not\\s+breathing",
            "can'?t\\s+breathe",
            "cardiac\\s+arrest",
            "seizures?",
            "kombulsyon",
            "nagco-?convulse",
            "dumudugo",
            "bleeding",
            "duguan",
            "naaksidente",
            "aksidente",
            "accident",
            "nabangga",
            "nasagasaan",
            "nalulunod",
            "drowning",
            "nalunod",
            "emergency",
            "ambulansya",
            "ambulance",
            "gas\\s+leak",
            "tagas\\s+ng\\s+gas",
            "nakuryente",
            "electrocuted",
            "baha",
            "flood\\w*",
            "landslide",
            "guho",
            "lindol",
            "earthquake",
            "may\\s+(?:baril|kutsilyo|patalim)",
            "may\\s+hawak\\s+na\\s+(?:baril|kutsilyo|patalim)",
            "armado",
            "armed",
            "bomb\\s+threat"
    );

    // =====================================================
    // SELF-HARM
    // =====================================================

    private static final Pattern SELF_HARM = words(
            "suicid\\w*",
            "magpapakamatay",
            "magpakamatay",
            "nagpakamatay",
            "pagpapakamatay",
            "kill\\s+myself",
            "end\\s+my\\s+life",
            "ayoko\\s+nang\\s+mabuhay",
            "gusto\\s+ko\\s+nang\\s+mamatay",
            "self-?harm",
            "saktan\\s+ang\\s+sarili"
    );

    private EmergencyKeywordScreen() {
    }

    // =====================================================
    // SCREEN COMPLAINT
    // =====================================================

    public static Result screen(String text) {

        if (text == null || text.isBlank()) {
            return new Result(Level.NONE, false);
        }

        String normalized = text
                .replace('\u2019', '\'')
                .replaceAll("\\s+", " ");

        boolean selfHarm =
                SELF_HARM.matcher(normalized).find();

        // Self-harm must be critical.
        if (selfHarm) {
            return new Result(Level.CRITICAL, true);
        }

        if (CRITICAL.matcher(normalized).find()) {
            return new Result(Level.CRITICAL, false);
        }

        if (IMMEDIATE.matcher(normalized).find()) {
            return new Result(Level.IMMEDIATE, false);
        }

        return new Result(Level.NONE, false);
    }
}
