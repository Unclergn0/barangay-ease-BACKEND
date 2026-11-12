
package com.muntinlupa.barangay_ease.ai;

import java.util.regex.Pattern;

public final class EmergencyKeywordScreen {

    public enum Level {
        NONE,
        IMMEDIATE,
        CRITICAL
    }

    public enum IncidentType {
        NONE,

        // Critical incidents
        STABBING,
        SHOOTING,
        MURDER,
        RAPE,
        SEXUAL_ASSAULT,
        CHILD_ABUSE,
        DOMESTIC_VIOLENCE,
        PHYSICAL_ASSAULT,
        KIDNAPPING,
        HOSTAGE,
        HUMAN_TRAFFICKING,
        ARMED_ROBBERY,
        ROBBERY,
        SELF_HARM,

        // Immediate emergencies
        FIRE,
        HEART_ATTACK,
        STROKE,
        UNCONSCIOUSNESS,
        BREATHING_DIFFICULTY,
        SEIZURE,
        BLEEDING,
        ACCIDENT,
        DROWNING,
        GAS_LEAK,
        ELECTROCUTION,
        FLOOD,
        LANDSLIDE,
        EARTHQUAKE,
        WEAPON_THREAT,
        BOMB_THREAT
    }

    public record Result(Level level, IncidentType incidentType) {

        public boolean flagged() {
            return level != Level.NONE;
        }

        public boolean critical() {
            return level == Level.CRITICAL;
        }

        public boolean immediate() {
            return level == Level.IMMEDIATE;
        }
    }

    private static final int FLAGS =
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static Pattern words(String... alternatives) {
        return Pattern.compile(
                "(?<![\\p{L}\\p{N}])(?:"
                        + String.join("|", alternatives)
                        + ")(?![\\p{L}\\p{N}])",
                FLAGS
        );
    }

    // =====================================================
    // CRITICAL INCIDENTS
    // =====================================================

    private static final Pattern STABBING = words(
            "sinaksak",
            "nasaksak",
            "sinaksakan",
            "nanaksak",
            "nanunaksak",
            "pananaksak",
            "sinaksak\\s+ng",
            "stab",
            "stabs",
            "stabbed",
            "stabbing",
            "na-?stab(?:bed|bing)?",
            "knife\\s+attack",
            "knife\\s+wound",
            "stab\\s+wound"
    );

    private static final Pattern SHOOTING = words(
            "binaril",
            "binarel",
            "binabaril",
            "binarilan",
            "nabaril",
            "pamamaril",
            "pagbaril",
            "barilan",
            "nagpaputok\\s+ng\\s+baril",
            "gunshots?",
            "gun\\s+shots?",
            "shooting",
            "shooter",
            "shot\\s+at",
            "shot\\s+dead",
            "shot\\s+someone",
            "shot\\s+him",
            "shot\\s+her",
            "shot\\s+me",
            "na-?shot",
            "na-?shoot",
            "someone\\s+was\\s+shot",
            "person\\s+was\\s+shot"
    );

    private static final Pattern MURDER = words(
            "murder",
            "murdered",
            "murdering",
            "murders",
            "homicide",
            "pinatay",
            "pumatay",
            "pagpatay",
            "pagpapatay",
            "pinapatay",
            "papatayin",
            "patayin",
            "napatay",
            "may\\s+pumatay",
            "killing\\s+incident",
            "killed\\s+someone",
            "someone\\s+was\\s+killed",
            "found\\s+dead"
    );

    private static final Pattern RAPE = words(
            "rape",
            "raped",
            "raping",
            "rapist",
            "ni-?rape",
            "ginahasa",
            "ginagahasa",
            "panggagahasa",
            "gahasa",
            "naghahasa"
    );

    private static final Pattern SEXUAL_ASSAULT = words(
            "sexual\\s+assault",
            "sexually\\s+assaulted",
            "sexual\\s+abuse",
            "sexual\\s+harassment",
            "sexually\\s+harassed",
            "hinalay",
            "hinahalay",
            "panghahalay",
            "molest\\w*",
            "minolestiya",
            "hinipuan",
            "hinahipuan"
    );

    private static final Pattern CHILD_ABUSE = words(
            "child\\s+abuse",
            "child\\s+neglect",
            "abused\\s+child",
            "abused\\s+the\\s+child",
            "inabuso\\s+ang\\s+bata",
            "inaabuso\\s+ang\\s+bata",
            "binubugbog\\s+ang\\s+bata",
            "sinaktan\\s+ang\\s+bata",
            "child\\s+exploitation",
            "pang-?aabuso\\s+sa\\s+bata"
    );

    private static final Pattern DOMESTIC_VIOLENCE = words(
            "domestic\\s+violence",
            "domestic\\s+abuse",
            "intimate\\s+partner\\s+violence",
            "vawc",
            "binubugbog\\s+ako\\s+ng\\s+(?:asawa|mister|partner|boyfriend|live-?in)",
            "binugbog\\s+ako\\s+ng\\s+(?:asawa|mister|partner|boyfriend|live-?in)",
            "sinasaktan\\s+ako\\s+ng\\s+(?:asawa|mister|partner|boyfriend|live-?in)",
            "inaabuso\\s+ako\\s+ng\\s+(?:asawa|mister|partner|boyfriend|live-?in)",
            "sinaktan\\s+ako\\s+ng\\s+(?:asawa|mister|partner|boyfriend|live-?in)"
    );

    private static final Pattern PHYSICAL_ASSAULT = words(
            "binubugbog",
            "binugbog",
            "pambubugbog",
            "binugbog\\s+ako",
            "sinuntok",
            "sinusuntok",
            "sinipa",
            "sinisipa",
            "sinaktan",
            "sinasaktan",
            "physical\\s+assault",
            "physically\\s+attacked",
            "assaulted",
            "attacked\\s+me",
            "beaten\\s+up",
            "beating\\s+someone"
    );

    private static final Pattern KIDNAPPING = words(
            "kidnap\\w*",
            "kidnapped",
            "kidnapping",
            "dinukot",
            "dinudukot",
            "dinukot\\s+ang",
            "dinakip\\s+at\\s+dinala",
            "sapilitang\\s+kinuha",
            "sapilitang\\s+dinala"
    );

    private static final Pattern HOSTAGE = words(
            "hostage",
            "hostages",
            "hostage-taking",
            "hostage\\s+situation",
            "ginawang\\s+bihag",
            "ginawang\\s+hostage",
            "binihag",
            "bihag"
    );

    private static final Pattern HUMAN_TRAFFICKING = words(
            "human\\s+trafficking",
            "trafficking\\s+in\\s+persons",
            "sex\\s+trafficking",
            "labor\\s+trafficking",
            "trafficked",
            "trafficking",
            "illegal\\s+recruitment",
            "sapilitang\\s+pinagtatrabaho",
            "ipinagbili\\s+ang\\s+tao"
    );

    private static final Pattern ARMED_ROBBERY = words(
            "armed\\s+robbery",
            "holdap",
            "hold-?up",
            "hinoldap",
            "nanloob\\s+na\\s+may\\s+baril",
            "robbery\\s+with\\s+a\\s+weapon",
            "robbed\\s+at\\s+gunpoint",
            "gunpoint\\s+robbery"
    );

    private static final Pattern ROBBERY = words(
            "robbery",
            "robbed",
            "robber",
            "stealing",
            "stole\\s+my",
            "ninakawan",
            "nanakawan",
            "pagnanakaw",
            "nagnakaw",
            "nanloob"
    );

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

    // =====================================================
    // IMMEDIATE EMERGENCIES
    // =====================================================

    private static final Pattern FIRE = words(
            "sunog",
            "nasusunog",
            "nasunog",
            "nagliliyab",
            "fire",
            "house\\s+fire",
            "building\\s+fire",
            "burning\\s+building"
    );

    private static final Pattern HEART_ATTACK = words(
            "heart\\s+attack",
            "cardiac\\s+arrest",
            "inatake\\s+sa\\s+puso",
            "atake\\s+sa\\s+puso",
            "atake\\s+sa\\s+heart"
    );

    private static final Pattern STROKE = words(
            "stroke",
            "na-?stroke",
            "brain\\s+attack",
            "na-stroke"
    );

    private static final Pattern UNCONSCIOUSNESS = words(
            "nahimatay",
            "hinimatay",
            "walang\\s+malay",
            "unconscious",
            "collapsed",
            "passed\\s+out",
            "nawalan\\s+ng\\s+malay"
    );

    private static final Pattern BREATHING_DIFFICULTY = words(
            "hindi\\s+(?:na\\s+)?humihinga",
            "di\\s+(?:na\\s+)?humihinga",
            "hirap\\s+huminga",
            "hindi\\s+makahinga",
            "di\\s+makahinga",
            "not\\s+breathing",
            "difficulty\\s+breathing",
            "trouble\\s+breathing",
            "can'?t\\s+breathe",
            "cannot\\s+breathe",
            "shortness\\s+of\\s+breath"
    );

    private static final Pattern SEIZURE = words(
            "seizure",
            "seizures",
            "kombulsyon",
            "nagko-?convulse",
            "convulsion",
            "convulsing"
    );

    private static final Pattern BLEEDING = words(
            "dumudugo",
            "nagdurugo",
            "matinding\\s+pagdurugo",
            "malakas\\s+ang\\s+dugo",
            "bleeding",
            "heavy\\s+bleeding",
            "severe\\s+bleeding",
            "hemorrhage"
    );

    private static final Pattern ACCIDENT = words(
            "naaksidente",
            "aksidente",
            "accident",
            "nabangga",
            "nasagasaan",
            "vehicular\\s+accident",
            "car\\s+crash",
            "motorcycle\\s+crash",
            "road\\s+accident",
            "collision"
    );

    private static final Pattern DROWNING = words(
            "nalulunod",
            "nalunod",
            "drowning",
            "drowned",
            "lumulubog\\s+sa\\s tubig",
            "nalulunod\\s+sa\\s+tubig"
    );

    private static final Pattern GAS_LEAK = words(
            "gas\\s+leak",
            "leaking\\s+gas",
            "tagas\\s+ng\\s+gas",
            "amoy\\s+gas",
            "sumisingaw\\s+na\\s+gas"
    );

    private static final Pattern ELECTROCUTION = words(
            "nakuryente",
            "nakakuryente",
            "electrocuted",
            "electrocution",
            "electric\\s+shock",
            "tinamaan\\s+ng\\s+kuryente"
    );

    private static final Pattern FLOOD = words(
            "baha",
            "binaha",
            "bumabaha",
            "flood",
            "flooding",
            "flash\\s+flood"
    );

    private static final Pattern LANDSLIDE = words(
            "landslide",
            "mudslide",
            "guho",
            "pagguho\\s+ng\\s+lupa",
            "gumuho\\s+ang\\s+lupa"
    );

    private static final Pattern EARTHQUAKE = words(
            "lindol",
            "earthquake",
            "aftershock"
    );

    private static final Pattern WEAPON_THREAT = words(
            "may\\s+hawak\\s+na\\s+(?:baril|kutsilyo|patalim)",
            "may\\s+(?:baril|kutsilyo|patalim)",
            "armed\\s+person",
            "person\\s+with\\s+a\\s+gun",
            "person\\s+with\\s+a\\s+knife",
            "threatened\\s+me\\s+with\\s+a\\s+weapon",
            "tinakot\\s+ako\\s+ng\\s+(?:baril|kutsilyo)",
            "armado"
    );

    private static final Pattern BOMB_THREAT = words(
            "bomb\\s+threat",
            "may\\s+bomba",
            "may\\s+nagbanta\\s+ng\\s+bomba",
            "bomba",
            "explosive\\s+threat"
    );

    private EmergencyKeywordScreen() {
    }

    // =====================================================
    // SCREEN COMPLAINT
    // One incident type and one response are returned.
    // =====================================================

    public static Result screen(String text) {

        if (text == null || text.isBlank()) {
            return new Result(Level.NONE, IncidentType.NONE);
        }

        String normalized = text
                .toLowerCase()
                .replace('\u2019', '\'')
                .replaceAll("\\s+", " ")
                .trim();

        // Critical incidents take priority over immediate incidents.

        if (STABBING.matcher(normalized).find())
            return critical(IncidentType.STABBING);

        if (SHOOTING.matcher(normalized).find())
            return critical(IncidentType.SHOOTING);

        if (MURDER.matcher(normalized).find())
            return critical(IncidentType.MURDER);

        if (RAPE.matcher(normalized).find())
            return critical(IncidentType.RAPE);

        if (SEXUAL_ASSAULT.matcher(normalized).find())
            return critical(IncidentType.SEXUAL_ASSAULT);

        if (CHILD_ABUSE.matcher(normalized).find())
            return critical(IncidentType.CHILD_ABUSE);

        if (DOMESTIC_VIOLENCE.matcher(normalized).find())
            return critical(IncidentType.DOMESTIC_VIOLENCE);

        if (SELF_HARM.matcher(normalized).find())
            return critical(IncidentType.SELF_HARM);

        if (HOSTAGE.matcher(normalized).find())
            return critical(IncidentType.HOSTAGE);

        if (KIDNAPPING.matcher(normalized).find())
            return critical(IncidentType.KIDNAPPING);

        if (HUMAN_TRAFFICKING.matcher(normalized).find())
            return critical(IncidentType.HUMAN_TRAFFICKING);

        if (ARMED_ROBBERY.matcher(normalized).find())
            return critical(IncidentType.ARMED_ROBBERY);

        if (ROBBERY.matcher(normalized).find())
            return critical(IncidentType.ROBBERY);

        if (PHYSICAL_ASSAULT.matcher(normalized).find())
            return critical(IncidentType.PHYSICAL_ASSAULT);

        // Immediate emergencies

        if (BOMB_THREAT.matcher(normalized).find())
            return immediate(IncidentType.BOMB_THREAT);

        if (WEAPON_THREAT.matcher(normalized).find())
            return immediate(IncidentType.WEAPON_THREAT);

        if (HEART_ATTACK.matcher(normalized).find())
            return immediate(IncidentType.HEART_ATTACK);

        if (BREATHING_DIFFICULTY.matcher(normalized).find())
            return immediate(IncidentType.BREATHING_DIFFICULTY);

        if (STROKE.matcher(normalized).find())
            return immediate(IncidentType.STROKE);

        if (UNCONSCIOUSNESS.matcher(normalized).find())
            return immediate(IncidentType.UNCONSCIOUSNESS);

        if (SEIZURE.matcher(normalized).find())
            return immediate(IncidentType.SEIZURE);

        if (BLEEDING.matcher(normalized).find())
            return immediate(IncidentType.BLEEDING);

        if (DROWNING.matcher(normalized).find())
            return immediate(IncidentType.DROWNING);

        if (FIRE.matcher(normalized).find())
            return immediate(IncidentType.FIRE);

        if (GAS_LEAK.matcher(normalized).find())
            return immediate(IncidentType.GAS_LEAK);

        if (ELECTROCUTION.matcher(normalized).find())
            return immediate(IncidentType.ELECTROCUTION);

        if (ACCIDENT.matcher(normalized).find())
            return immediate(IncidentType.ACCIDENT);

        if (LANDSLIDE.matcher(normalized).find())
            return immediate(IncidentType.LANDSLIDE);

        if (FLOOD.matcher(normalized).find())
            return immediate(IncidentType.FLOOD);

        if (EARTHQUAKE.matcher(normalized).find())
            return immediate(IncidentType.EARTHQUAKE);

        return new Result(Level.NONE, IncidentType.NONE);
    }

    private static Result critical(IncidentType type) {
        return new Result(Level.CRITICAL, type);
    }

    private static Result immediate(IncidentType type) {
        return new Result(Level.IMMEDIATE, type);
    }
}
