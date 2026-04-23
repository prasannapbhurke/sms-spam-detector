package com.example.spamdetector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RiskAnalyzer {
    private static final Pattern OTP_DIGIT_PATTERN = Pattern.compile("\\b\\d{4,8}\\b");
    private static final Pattern LARGE_NUMBER_PATTERN = Pattern.compile("\\b\\d{4,}\\b");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("(?:rs\\.?|inr|usd|eur|\\$|₹)\\s*\\d+[\\d,]*(?:\\.\\d+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?\\d{1,3}[\\s-]?)?(?:\\d[\\s-]?){10,12}\\b");
    private static final Map<String, String[]> LANGUAGE_PHRASES = new LinkedHashMap<>();

    static {
        LANGUAGE_PHRASES.put("Hindi", new String[]{
                "बैंक खाता", "केवाईसी", "तुरंत", "इनाम", "जीत", "लिंक", "सत्यापित", "ओटीपी", "ऋण", "अकाउंट ब्लॉक"
        });
        LANGUAGE_PHRASES.put("Hinglish", new String[]{
                "bank account", "account verify", "otp share", "click karo", "link kholo", "reward claim",
                "kyc update", "loan approved", "urgent hai", "account block"
        });
        LANGUAGE_PHRASES.put("Tamil", new String[]{
                "வங்கி கணக்கு", "சரிபார்", "வெற்றி", "இணைப்பை திற", "உடனே", "கடன் ஒப்புதல்"
        });
        LANGUAGE_PHRASES.put("Telugu", new String[]{
                "బ్యాంక్ ఖాతా", "ధృవీకరించండి", "లింక్ తెరవండి", "తక్షణం", "బహుమతి", "రుణం"
        });
        LANGUAGE_PHRASES.put("Kannada", new String[]{
                "ಬ್ಯಾಂಕ್ ಖಾತೆ", "ದೃಢೀಕರಿಸಿ", "ಲಿಂಕ್ ತೆರೆಯಿರಿ", "ತಕ್ಷಣ", "ಬಹುಮಾನ", "ಸಾಲ"
        });
        LANGUAGE_PHRASES.put("Malayalam", new String[]{
                "ബാങ്ക് അക്കൗണ്ട്", "സ്ഥിരീകരിക്കുക", "ലിങ്ക് തുറക്കുക", "ഉടൻ", "ബഹുമതി", "വായ്പ"
        });
        LANGUAGE_PHRASES.put("Bengali", new String[]{
                "ব্যাঙ্ক অ্যাকাউন্ট", "যাচাই করুন", "লিঙ্ক খুলুন", "জরুরি", "পুরস্কার", "ঋণ"
        });
        LANGUAGE_PHRASES.put("Marathi", new String[]{
                "बँक खाते", "सत्यापित करा", "लिंक उघडा", "तात्काळ", "बक्षीस", "कर्ज"
        });
    }

    public static class RiskResult {
        public String category;
        public String reasons;
        public boolean hasLink;
        public String language;
        public String label;
        public String riskLevel;

        public RiskResult(String category, String reasons, boolean hasLink, String language, String label, String riskLevel) {
            this.category = category;
            this.reasons = reasons;
            this.hasLink = hasLink;
            this.language = language;
            this.label = label;
            this.riskLevel = riskLevel;
        }
    }

    public static RiskResult analyze(String message, String sender, int repeatedSpamHits, float mlScore) {
        String lowered = message.toLowerCase(Locale.getDefault());
        String normalized = TextFeatureExtractor.normalize(message);
        List<String> reasons = new ArrayList<>();
        boolean hasLink = lowered.contains("http") || lowered.contains("www") || lowered.contains(".com") || lowered.contains(".in");
        String language = detectLanguage(message);
        int ruleHits = 0;

        if (hasLink) { reasons.add("Suspicious link detected"); ruleHits++; }
        if (lowered.contains("bit.ly") || lowered.contains("tinyurl")) { reasons.add("Shortened URL detected"); ruleHits++; }
        if (containsLookalikeDomain(lowered)) { reasons.add("Possible lookalike domain pattern"); ruleHits++; }
        if (lowered.contains("otp") || lowered.contains("verify") || lowered.contains("password")) { reasons.add("Credential-related wording found"); ruleHits++; }
        if (lowered.contains("bank") || lowered.contains("upi") || lowered.contains("kyc") || lowered.contains("wallet") || lowered.contains("account")) { reasons.add("Banking or account keywords found"); ruleHits++; }
        if (lowered.contains("urgent") || lowered.contains("final warning") || lowered.contains("immediately")) { reasons.add("Urgency language found"); ruleHits++; }
        if (CURRENCY_PATTERN.matcher(lowered).find()) { reasons.add("Money amount pattern detected"); ruleHits++; }
        if (hasOtpDigits(lowered)) { reasons.add("OTP-style numeric code detected"); ruleHits++; }
        if (PHONE_PATTERN.matcher(lowered).find()) { reasons.add("Phone-number-style pattern detected"); ruleHits++; }
        if (hasDenseDigits(lowered)) { reasons.add("Heavy numeric density found"); ruleHits++; }
        if (normalized.contains("currency_amount") && normalized.contains("url_token")) { reasons.add("Money + link combination detected"); ruleHits++; }
        if (normalized.contains("otp_digits") && normalized.contains("account")) { reasons.add("OTP + account combination detected"); ruleHits++; }
        if (sender != null && sender.startsWith("+") && !sender.startsWith("+91")) { reasons.add("International sender number"); ruleHits++; }
        if (sender != null && sender.matches(".*[A-Za-z].*\\d.*")) { reasons.add("Alphanumeric sender pattern"); ruleHits++; }
        if (repeatedSpamHits >= 2) { reasons.add("Sender has repeated spam history"); ruleHits++; }

        int multilingualHits = addPhrasePackReasons(lowered, reasons);
        if (multilingualHits > 0) {
            ruleHits += multilingualHits;
            reasons.add("Matched multilingual scam phrase pack");
        }

        if (!language.equals("English")) {
            reasons.add(language + " language pattern detected");
        }

        String label;
        String riskLevel;
        if (mlScore >= 0.8f) {
            label = "Spam";
            riskLevel = "High Risk";
            reasons.add("ML model confidence is above 0.8");
        } else if (mlScore >= 0.4f) {
            if (ruleHits >= 1) {
                label = "Spam";
                riskLevel = "Elevated";
                reasons.add("ML score confirmed by rule-based checks");
            } else {
                label = "Needs Review";
                riskLevel = "Review";
                reasons.add("Moderate ML score without strong rule confirmation");
            }
        } else {
            label = ruleHits >= 3 ? "Spam" : "Safe";
            riskLevel = label.equals("Spam") ? "Elevated" : "Low Risk";
            if (reasons.isEmpty()) {
                reasons.add("Low ML confidence and few risk indicators");
            }
        }

        return new RiskResult(
                inferCategory(lowered, mlScore),
                android.text.TextUtils.join(" | ", reasons),
                hasLink,
                language,
                label,
                riskLevel
        );
    }

    private static int addPhrasePackReasons(String lowered, List<String> reasons) {
        int hits = 0;
        for (Map.Entry<String, String[]> entry : LANGUAGE_PHRASES.entrySet()) {
            for (String phrase : entry.getValue()) {
                if (lowered.contains(phrase.toLowerCase(Locale.getDefault()))) {
                    reasons.add("Detected " + entry.getKey() + " scam phrase: " + phrase);
                    hits++;
                    break;
                }
            }
        }
        return hits;
    }

    private static boolean containsLookalikeDomain(String lowered) {
        return lowered.contains("secure-") || lowered.contains("verify-") || lowered.contains("login-") || lowered.contains("update-kyc");
    }

    private static boolean hasOtpDigits(String lowered) {
        Matcher matcher = OTP_DIGIT_PATTERN.matcher(lowered);
        return matcher.find();
    }

    private static boolean hasDenseDigits(String lowered) {
        int digitCount = 0;
        for (int i = 0; i < lowered.length(); i++) {
            if (Character.isDigit(lowered.charAt(i))) {
                digitCount++;
            }
        }
        return digitCount >= 6 || LARGE_NUMBER_PATTERN.matcher(lowered).find();
    }

    public static String inferCategory(String lowered, float mlScore) {
        if ((lowered.contains("otp") || lowered.contains("password") || lowered.contains("ओटीपी")) &&
                (lowered.contains("account") || lowered.contains("bank") || lowered.contains("wallet") || lowered.contains("खाता"))) {
            return "Account Takeover Attempt";
        }
        if (lowered.contains("loan") || lowered.contains("credit") || lowered.contains("cash") || lowered.contains("ऋण") || lowered.contains("कर्ज")) return "Loan scam";
        if (lowered.contains("parcel") || lowered.contains("delivery") || lowered.contains("shipment") || lowered.contains("courier")) return "Delivery fraud";
        if (lowered.contains("job") || lowered.contains("part-time") || lowered.contains("earn") || lowered.contains("salary")) return "Job scam";
        if (lowered.contains("prize") || lowered.contains("reward") || lowered.contains("win") || lowered.contains("free") || lowered.contains("इनाम") || lowered.contains("जीत")) return "Prize scam";
        if (lowered.contains("offer") || lowered.contains("discount") || lowered.contains("sale")) return "Promotional";
        if (lowered.contains("bank") || lowered.contains("upi") || lowered.contains("wallet") || lowered.contains("केवाईसी") || lowered.contains("खाता")) return "Phishing";
        if (mlScore >= 0.8f) return "High Risk Pattern";
        return "General";
    }

    public static String detectLanguage(String text) {
        if (text.matches(".*[\\u0900-\\u097F].*")) return "Hindi";
        if (text.matches(".*[\\u0B80-\\u0BFF].*")) return "Tamil";
        if (text.matches(".*[\\u0C00-\\u0C7F].*")) return "Telugu";
        if (text.matches(".*[\\u0C80-\\u0CFF].*")) return "Kannada";
        if (text.matches(".*[\\u0D00-\\u0D7F].*")) return "Malayalam";
        if (text.matches(".*[\\u0980-\\u09FF].*")) return "Bengali";
        if (text.matches(".*[\\u0900-\\u097F].*")) return "Marathi";

        String lowered = text.toLowerCase(Locale.getDefault());
        if (containsAny(lowered, LANGUAGE_PHRASES.get("Hinglish"))) return "Hinglish";
        if (containsAny(lowered, LANGUAGE_PHRASES.get("Hindi"))) return "Mixed Hindi";
        if (containsAny(lowered, LANGUAGE_PHRASES.get("Tamil"))) return "Tamil";
        if (containsAny(lowered, LANGUAGE_PHRASES.get("Telugu"))) return "Telugu";
        if (containsAny(lowered, LANGUAGE_PHRASES.get("Kannada"))) return "Kannada";
        if (containsAny(lowered, LANGUAGE_PHRASES.get("Malayalam"))) return "Malayalam";
        if (containsAny(lowered, LANGUAGE_PHRASES.get("Bengali"))) return "Bengali";
        if (containsAny(lowered, LANGUAGE_PHRASES.get("Marathi"))) return "Marathi";
        return "English";
    }

    private static boolean containsAny(String lowered, String[] phrases) {
        if (phrases == null) {
            return false;
        }
        for (String phrase : phrases) {
            if (lowered.contains(phrase.toLowerCase(Locale.getDefault()))) {
                return true;
            }
        }
        return false;
    }
}
