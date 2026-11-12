
package com.muntinlupa.barangay_ease.ai;

public final class SafetyMessages {

    private SafetyMessages() {
    }

    // =====================================================
    // GENERIC COMPATIBILITY MESSAGES
    // =====================================================

    public static final String CRITICAL =
            "This complaint has been classified as critical and requires urgent human review. "
          + "If anyone is in immediate danger, call 911 or emergency responders now. "
          + "Do not wait for an online response.";

    public static final String IMMEDIATE =
            "This complaint requires prompt attention from barangay personnel. "
          + "If anyone is in immediate danger, call 911 or emergency responders now.";

    // =====================================================
    // CRITICAL INCIDENT RESPONSES
    // =====================================================

    public static final String STABBING =
            "EMERGENCY: This report involves a stabbing or a person who has been stabbed. "
          + "Call 911 immediately. Move away from the attacker and stay in a safe place. "
          + "Barangay personnel and rescue responders should be alerted. Include the exact "
          + "incident location, street, house number, purok, or nearby landmark. Do not wait "
          + "for an online response. "
          + "EMERGENCY: Ang ulat na ito ay may kinalaman sa pananaksak o taong nasaksak. "
          + "Tumawag agad sa 911. Lumayo sa nanaksak at pumunta sa ligtas na lugar. "
          + "Dapat maabisuhan ang barangay personnel at rescue responders. Isama ang eksaktong "
          + "lokasyon ng insidente. Huwag hintayin ang online response.";

    public static final String SHOOTING =
            "EMERGENCY: This report involves a shooting or a person who has been shot. "
          + "Call 911 immediately. Stay away from the shooter and move to a secure location "
          + "if safe to do so. Do not approach the weapon or attacker. Include the exact "
          + "incident location. Do not wait for an online response. "
          + "EMERGENCY: Ang ulat na ito ay may kinalaman sa pamamaril o taong nabaril. "
          + "Tumawag agad sa 911. Lumayo sa taong may baril at pumunta sa ligtas na lugar "
          + "kung ligtas itong gawin. Huwag lumapit sa baril o salarin. Isama ang eksaktong "
          + "lokasyon ng insidente. Huwag hintayin ang online response.";

    public static final String MURDER =
            "EMERGENCY: This report may involve murder or homicide. Call 911 and contact "
          + "the PNP immediately. Do not approach a suspected attacker or disturb the scene. "
          + "If someone may still be alive or injured, request emergency medical assistance. "
          + "Include the exact incident location. Do not wait for an online response. "
          + "EMERGENCY: Maaaring may kinalaman ang ulat na ito sa pagpatay o homicide. "
          + "Tumawag agad sa 911 at makipag-ugnayan sa PNP. Huwag lumapit sa pinaghihinalaang "
          + "salarin o galawin ang lugar ng insidente. Kung may posibilidad na buhay pa o "
          + "sugatan ang biktima, humingi agad ng tulong medikal. Isama ang eksaktong lokasyon.";

    public static final String RAPE =
            "This report may involve rape. Prioritize the victim's immediate safety and "
          + "privacy. Contact the PNP or the nearest Women and Children Protection Desk (WCPD) "
          + "for appropriate assistance. If the victim is in immediate danger or needs medical "
          + "attention, call 911. Do not confront the suspected offender. "
          + "Maaaring may kinalaman ang ulat na ito sa panggagahasa. Unahin ang kaligtasan "
          + "at privacy ng biktima. Makipag-ugnayan sa PNP o pinakamalapit na Women and "
          + "Children Protection Desk (WCPD). Kung may agarang panganib o nangangailangan "
          + "ng tulong medikal ang biktima, tumawag sa 911.";

    public static final String SEXUAL_ASSAULT =
            "This report may involve sexual assault or harassment. Ensure the affected person's "
          + "safety and privacy. Contact the PNP or the nearest WCPD for appropriate assistance. "
          + "Call 911 if there is immediate danger or urgent medical need. "
          + "Maaaring may kinalaman ang ulat na ito sa sexual assault o harassment. "
          + "Siguraduhin ang kaligtasan at privacy ng biktima. Makipag-ugnayan sa PNP o "
          + "pinakamalapit na WCPD. Tumawag sa 911 kung may agarang panganib o medical emergency.";

    public static final String CHILD_ABUSE =
            "This report may involve child abuse. Protect the child's immediate safety and "
          + "privacy. Contact the PNP Women and Children Protection Desk (WCPD) and the "
          + "appropriate child protection authorities. Call 911 if the child is in immediate "
          + "danger or needs urgent medical care. Do not confront the suspected offender. "
          + "Maaaring may kinalaman ang ulat na ito sa pang-aabuso sa bata. Unahin ang "
          + "kaligtasan at privacy ng bata. Makipag-ugnayan sa PNP-WCPD at kinauukulang "
          + "child protection authorities. Tumawag sa 911 kung nasa agarang panganib ang bata.";

    public static final String DOMESTIC_VIOLENCE =
            "This report may involve domestic violence. Move to a safe place if possible. "
          + "Contact the Barangay VAWC desk or the PNP Women and Children Protection Desk "
          + "(WCPD). Call 911 if you or someone else is in immediate danger. Avoid confronting "
          + "the suspected abuser. "
          + "Maaaring may kinalaman ang ulat na ito sa domestic violence. Pumunta sa ligtas "
          + "na lugar kung maaari. Makipag-ugnayan sa Barangay VAWC desk o PNP-WCPD. "
          + "Tumawag sa 911 kung may agarang panganib. Iwasang komprontahin ang pinaghihinalaang abusado.";

    public static final String PHYSICAL_ASSAULT =
            "This report may involve physical assault. Move away from the aggressor and seek "
          + "medical attention if anyone is injured. Contact the Barangay Hall or PNP. "
          + "Call 911 if there is immediate danger or serious injury. Include the exact location. "
          + "Maaaring may kinalaman ang ulat na ito sa pisikal na pananakit. Lumayo sa nanakit "
          + "at humingi ng tulong medikal kung may nasugatan. Makipag-ugnayan sa Barangay Hall "
          + "o PNP. Tumawag sa 911 kung may agarang panganib o malubhang pinsala.";

    public static final String KIDNAPPING =
            "This report may involve kidnapping or abduction. Contact the PNP immediately "
          + "and call 911 if someone is in immediate danger. Do not confront the suspected "
          + "offender. Provide the victim's last known location and relevant details to "
          + "authorized responders. "
          + "Maaaring may kinalaman ang ulat na ito sa pagdukot. Makipag-ugnayan agad sa PNP "
          + "at tumawag sa 911 kung may agarang panganib. Huwag komprontahin ang pinaghihinalaang "
          + "salarin. Ibigay sa awtorisadong responders ang huling lokasyon ng biktima.";

    public static final String HOSTAGE =
            "This report may involve a hostage situation. Call 911 and contact the PNP "
          + "immediately. Stay away from the location and do not attempt a rescue or confront "
          + "the armed person. Provide the exact location to emergency responders. "
          + "Maaaring may kinalaman ang ulat na ito sa hostage situation. Tumawag agad sa 911 "
          + "at makipag-ugnayan sa PNP. Lumayo sa lugar at huwag subukang magsagawa ng rescue "
          + "o komprontahin ang armadong tao. Ibigay ang eksaktong lokasyon sa responders.";

    public static final String HUMAN_TRAFFICKING =
            "This report may involve human trafficking or exploitation. Do not confront "
          + "the suspected trafficker. Contact the PNP or appropriate anti-trafficking "
          + "authorities. Protect the affected person's identity and safety. Call 911 if "
          + "there is immediate danger. "
          + "Maaaring may kinalaman ang ulat na ito sa human trafficking o pagsasamantala. "
          + "Huwag komprontahin ang pinaghihinalaang trafficker. Makipag-ugnayan sa PNP o "
          + "kinauukulang awtoridad. Protektahan ang pagkakakilanlan at kaligtasan ng biktima.";

    public static final String ARMED_ROBBERY =
            "This report may involve armed robbery. Do not approach or confront the suspects. "
          + "Move to a safe location and call 911 or the PNP immediately. Include the exact "
          + "incident location and provide details only when it is safe to do so. "
          + "Maaaring may kinalaman ang ulat na ito sa armadong holdap. Huwag lumapit o "
          + "komprontahin ang mga suspek. Pumunta sa ligtas na lugar at tumawag agad sa 911 "
          + "o PNP. Ibigay ang eksaktong lokasyon kapag ligtas nang gawin ito.";

    public static final String ROBBERY =
            "This report may involve robbery or theft. Move to a safe place and avoid "
          + "confronting the suspect. Report the incident to the PNP or Barangay Hall. "
          + "Call 911 if anyone is threatened or injured. Include the incident location. "
          + "Maaaring may kinalaman ang ulat na ito sa pagnanakaw o robbery. Pumunta sa "
          + "ligtas na lugar at iwasang komprontahin ang suspek. I-report ito sa PNP o "
          + "Barangay Hall. Tumawag sa 911 kung may nanganganib o nasugatan.";

    public static final String SELF_HARM =
            "I'm sorry you're going through this. You do not have to face this alone. "
          + "If you may hurt yourself or are in immediate danger, call 911 or go to the "
          + "nearest hospital. If possible, ask someone you trust to stay with you and "
          + "help you get support. "
          + "Pasensya na at pinagdadaanan mo ito. Hindi mo kailangang harapin ito nang mag-isa. "
          + "Kung may posibilidad na saktan mo ang iyong sarili o nasa agarang panganib ka, "
          + "tumawag sa 911 o pumunta sa pinakamalapit na ospital. Magpasama sa taong pinagkakatiwalaan mo.";

    // =====================================================
    // IMMEDIATE EMERGENCY RESPONSES
    // =====================================================

    public static final String FIRE =
            "EMERGENCY: A fire may be occurring. Call 911 or the local fire department "
          + "immediately. Evacuate the area using a safe exit. Do not re-enter the building. "
          + "Provide the exact location to responders. "
          + "EMERGENCY: Maaaring may sunog. Tumawag agad sa 911 o lokal na himpilan ng bumbero. "
          + "Lumikas gamit ang ligtas na daanan. Huwag bumalik sa nasusunog na gusali. "
          + "Ibigay ang eksaktong lokasyon sa responders.";

    public static final String HEART_ATTACK =
            "EMERGENCY: This may be a heart attack or cardiac emergency. Call 911 immediately "
          + "and request emergency medical assistance. Keep the person safe and follow the "
          + "dispatcher or medical professional's instructions. "
          + "EMERGENCY: Maaaring atake sa puso ito. Tumawag agad sa 911 at humingi ng emergency "
          + "medical assistance. Panatilihing ligtas ang pasyente at sundin ang instructions "
          + "ng dispatcher o medical professional.";

    public static final String STROKE =
            "EMERGENCY: This may be a stroke. Call 911 immediately. Note the time symptoms "
          + "started, if known. Do not delay medical care while waiting for an online reply. "
          + "EMERGENCY: Maaaring stroke ito. Tumawag agad sa 911. Tandaan kung kailan nagsimula "
          + "ang mga sintomas kung alam. Huwag ipagpaliban ang pagpapatingin sa ospital.";

    public static final String UNCONSCIOUSNESS =
            "EMERGENCY: Someone may be unconscious. Call 911 immediately and request medical "
          + "assistance. Check whether the person is breathing if you can do so safely and "
          + "follow the emergency dispatcher's instructions. "
          + "EMERGENCY: Maaaring walang malay ang isang tao. Tumawag agad sa 911 at humingi "
          + "ng tulong medikal. Sundin ang instructions ng emergency dispatcher.";

    public static final String BREATHING_DIFFICULTY =
            "EMERGENCY: Someone may be having difficulty breathing. Call 911 immediately "
          + "and request emergency medical assistance. Do not delay getting help. "
          + "EMERGENCY: Maaaring nahihirapang huminga ang isang tao. Tumawag agad sa 911 "
          + "at humingi ng agarang tulong medikal.";

    public static final String SEIZURE =
            "EMERGENCY: Someone may be having a seizure. Call 911 if it is prolonged, "
          + "repeated, the person is injured, or this is their first seizure. Keep the area "
          + "clear and do not restrain the person or put anything in their mouth. "
          + "EMERGENCY: Maaaring may kombulsyon ang isang tao. Tumawag sa 911 lalo na kung "
          + "matagal o paulit-ulit ito, may pinsala, o unang beses itong nangyari. Alisin "
          + "ang mga bagay na maaaring makasakit at huwag pigilan ang paggalaw ng tao.";

    public static final String BLEEDING =
            "EMERGENCY: This report may involve serious bleeding. Call 911 and request "
          + "emergency medical assistance. If safe, apply firm direct pressure to the wound "
          + "using a clean cloth while waiting for help. "
          + "EMERGENCY: Maaaring may malubhang pagdurugo. Tumawag sa 911 at humingi ng tulong "
          + "medikal. Kung ligtas, idiin ang malinis na tela sa sugat habang hinihintay ang tulong.";

    public static final String ACCIDENT =
            "EMERGENCY: An accident may have caused injuries. Call 911 if anyone is injured "
          + "or in danger. Stay in a safe area and provide the exact accident location. "
          + "EMERGENCY: Maaaring may nasugatan sa aksidente. Tumawag sa 911 kung may nasaktan "
          + "o nanganganib. Manatili sa ligtas na lugar at ibigay ang eksaktong lokasyon.";

    public static final String DROWNING =
            "EMERGENCY: This report may involve drowning. Call 911 immediately and request "
          + "rescue and medical assistance. Do not enter unsafe water or put yourself in danger. "
          + "EMERGENCY: Maaaring may nalulunod. Tumawag agad sa 911 at humingi ng rescue at "
          + "medical assistance. Huwag pumasok sa mapanganib na tubig o ilagay ang sarili sa panganib.";

    public static final String GAS_LEAK =
            "EMERGENCY: A gas leak may be present. Leave the area immediately if safe. "
          + "Avoid flames, smoking, and operating electrical switches near the suspected leak. "
          + "Call 911 or the appropriate emergency service from a safe location. "
          + "EMERGENCY: Maaaring may tagas ng gas. Lumikas agad kung ligtas. Iwasan ang apoy, "
          + "paninigarilyo, at paggamit ng electrical switches malapit sa pinaghihinalaang tagas. "
          + "Tumawag sa 911 mula sa ligtas na lugar.";

    public static final String ELECTROCUTION =
            "EMERGENCY: This report may involve electric shock. Do not touch the person "
          + "while they may still be in contact with electricity. Keep away from the hazard "
          + "and call 911 immediately. "
          + "EMERGENCY: Maaaring may nakuryente. Huwag hawakan ang tao kung maaari pa siyang "
          + "nakadikit sa kuryente. Lumayo sa panganib at tumawag agad sa 911.";

    public static final String FLOOD =
            "EMERGENCY: Flooding may threaten lives and property. Move to higher ground "
          + "if safe and stay away from moving floodwater and electrical hazards. Follow "
          + "official evacuation instructions. Call 911 if anyone is in immediate danger. "
          + "EMERGENCY: Maaaring magdulot ng panganib ang baha. Pumunta sa mataas na lugar "
          + "kung ligtas at lumayo sa rumaragasang tubig at mga electrical hazard. Sundin "
          + "ang opisyal na evacuation instructions.";

    public static final String LANDSLIDE =
            "EMERGENCY: A landslide or ground collapse may be occurring. Move away from "
          + "the affected slope or area if safe. Do not return until authorities declare "
          + "the area safe. Call 911 if anyone is trapped or injured. "
          + "EMERGENCY: Maaaring may pagguho ng lupa. Lumayo sa apektadong lugar kung ligtas. "
          + "Huwag bumalik hangga't hindi idinedeklarang ligtas ng awtoridad. Tumawag sa 911 "
          + "kung may na-trap o nasugatan.";

    public static final String EARTHQUAKE =
            "EMERGENCY: An earthquake has been reported. Follow official safety instructions "
          + "and move away from damaged structures when safe. Be prepared for aftershocks. "
          + "Call 911 if anyone is trapped or injured. "
          + "EMERGENCY: May naiulat na lindol. Sundin ang opisyal na safety instructions at "
          + "lumayo sa mga sirang gusali kapag ligtas. Mag-ingat sa aftershocks. Tumawag sa 911 "
          + "kung may na-trap o nasugatan.";

    public static final String WEAPON_THREAT =
            "EMERGENCY: A person may be threatening others with a weapon. Do not approach "
          + "or confront the person. Move to a secure location and call 911 or the PNP. "
          + "Provide the exact location from a safe place. "
          + "EMERGENCY: Maaaring may nananakot gamit ang armas. Huwag lumapit o komprontahin "
          + "ang tao. Pumunta sa ligtas na lugar at tumawag sa 911 o PNP. Ibigay ang lokasyon "
          + "mula sa ligtas na lugar.";

    public static final String BOMB_THREAT =
            "EMERGENCY: This report may involve a bomb or explosive threat. Move away from "
          + "the suspected object or area. Do not touch or inspect it. Call 911 or the PNP "
          + "immediately and follow official instructions. "
          + "EMERGENCY: Maaaring may bomba o banta ng pagsabog. Lumayo sa pinaghihinalaang "
          + "bagay o lugar. Huwag hawakan o siyasatin ito. Tumawag agad sa 911 o PNP at "
          + "sundin ang opisyal na instructions.";

    // =====================================================
    // SELECT EXACTLY ONE RESPONSE
    // =====================================================

    public static String forScreen(EmergencyKeywordScreen.Result r) {

        if (r == null) {
            return "Please contact the appropriate emergency service if anyone is in danger. "
                 + "Tumawag sa 911 kung may agarang panganib.";
        }

        switch (r.incidentType()) {

            case STABBING: return STABBING;
            case SHOOTING: return SHOOTING;
            case MURDER: return MURDER;
            case RAPE: return RAPE;
            case SEXUAL_ASSAULT: return SEXUAL_ASSAULT;
            case CHILD_ABUSE: return CHILD_ABUSE;
            case DOMESTIC_VIOLENCE: return DOMESTIC_VIOLENCE;
            case PHYSICAL_ASSAULT: return PHYSICAL_ASSAULT;
            case KIDNAPPING: return KIDNAPPING;
            case HOSTAGE: return HOSTAGE;
            case HUMAN_TRAFFICKING: return HUMAN_TRAFFICKING;
            case ARMED_ROBBERY: return ARMED_ROBBERY;
            case ROBBERY: return ROBBERY;
            case SELF_HARM: return SELF_HARM;

            case FIRE: return FIRE;
            case HEART_ATTACK: return HEART_ATTACK;
            case STROKE: return STROKE;
            case UNCONSCIOUSNESS: return UNCONSCIOUSNESS;
            case BREATHING_DIFFICULTY: return BREATHING_DIFFICULTY;
            case SEIZURE: return SEIZURE;
            case BLEEDING: return BLEEDING;
            case ACCIDENT: return ACCIDENT;
            case DROWNING: return DROWNING;
            case GAS_LEAK: return GAS_LEAK;
            case ELECTROCUTION: return ELECTROCUTION;
            case FLOOD: return FLOOD;
            case LANDSLIDE: return LANDSLIDE;
            case EARTHQUAKE: return EARTHQUAKE;
            case WEAPON_THREAT: return WEAPON_THREAT;
            case BOMB_THREAT: return BOMB_THREAT;

            case NONE:
            default:
                return "";
        }
    }
}
