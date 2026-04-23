package com.example.spamdetector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextFeatureExtractor {
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+|www\\.\\S+|\\b\\S+\\.(com|in|net|org|co)\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT_URL_PATTERN = Pattern.compile("\\b(bit\\.ly|tinyurl\\.com|t\\.co|goo\\.gl|rb\\.gy|cutt\\.ly)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("(?:rs\\.?|inr|usd|eur|\\$|₹)\\s*\\d+[\\d,]*(?:\\.\\d+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern LARGE_NUMBER_PATTERN = Pattern.compile("\\b\\d{4,}\\b");
    private static final Pattern OTP_DIGIT_PATTERN = Pattern.compile("\\b\\d{4,8}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?\\d{1,3}[\\s-]?)?(?:\\d[\\s-]?){10,12}\\b");

    private TextFeatureExtractor() {
    }

    public static List<String> buildFeatures(String text) {
        String normalized = normalize(text);
        List<String> baseTokens = tokenize(normalized);
        Set<String> features = new LinkedHashSet<>(baseTokens);

        for (int i = 0; i < baseTokens.size() - 1; i++) {
            features.add(baseTokens.get(i) + "_" + baseTokens.get(i + 1));
        }
        for (int i = 0; i < baseTokens.size() - 2; i++) {
            features.add(baseTokens.get(i) + "_" + baseTokens.get(i + 1) + "_" + baseTokens.get(i + 2));
        }

        features.addAll(extractEngineeredSignals(text, normalized));
        return new ArrayList<>(features);
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.toLowerCase(Locale.getDefault()).trim();
        normalized = URL_PATTERN.matcher(normalized).replaceAll(" url_token ");
        normalized = CURRENCY_PATTERN.matcher(normalized).replaceAll(" currency_amount ");
        normalized = PHONE_PATTERN.matcher(normalized).replaceAll(" phone_number ");
        normalized = normalized.replaceAll("(?<!\\w)\\d{4,8}(?!\\w)", " otp_digits ");
        normalized = normalized.replaceAll("(?<!\\w)\\d+(?!\\w)", " number_token ");
        normalized = normalized.replaceAll("[^\\p{L}\\p{Nd}_ ]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private static List<String> tokenize(String normalizedText) {
        List<String> tokens = new ArrayList<>();
        if (normalizedText.isEmpty()) {
            return tokens;
        }

        for (String token : normalizedText.split("\\s+")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static List<String> extractEngineeredSignals(String rawText, String normalizedText) {
        List<String> signals = new ArrayList<>();
        String loweredRaw = rawText == null ? "" : rawText.toLowerCase(Locale.getDefault());

        if (URL_PATTERN.matcher(loweredRaw).find()) {
            signals.add("feature_has_url");
        }
        if (SHORT_URL_PATTERN.matcher(loweredRaw).find()) {
            signals.add("feature_has_short_url");
        }
        if (CURRENCY_PATTERN.matcher(loweredRaw).find()) {
            signals.add("feature_has_currency");
        }
        if (containsAny(loweredRaw, "otp", "verify", "verification", "bank", "account", "upi", "kyc", "wallet")) {
            signals.add("feature_finance_context");
        }
        if (containsAny(loweredRaw, "call now", "contact", "urgent", "immediately", "final warning", "claim")) {
            signals.add("feature_action_pressure");
        }
        if (containsAny(loweredRaw, "loan", "salary", "reward", "prize", "parcel", "delivery", "job")) {
            signals.add("feature_scam_theme");
        }
        if (OTP_DIGIT_PATTERN.matcher(loweredRaw).find()) {
            signals.add("feature_has_otp_digits");
        }
        if (PHONE_PATTERN.matcher(loweredRaw).find()) {
            signals.add("feature_has_phone_number");
        }

        int digitCount = 0;
        for (int i = 0; i < loweredRaw.length(); i++) {
            if (Character.isDigit(loweredRaw.charAt(i))) {
                digitCount++;
            }
        }

        if (digitCount >= 6) {
            signals.add("feature_many_digits");
        }
        if (digitCount >= 10) {
            signals.add("feature_dense_numeric");
        }
        if (LARGE_NUMBER_PATTERN.matcher(loweredRaw).find()) {
            signals.add("feature_large_number");
        }
        if (normalizedText.contains("currency_amount") && normalizedText.contains("url_token")) {
            signals.add("feature_money_plus_link");
        }
        if (normalizedText.contains("otp_digits") && normalizedText.contains("account")) {
            signals.add("feature_otp_account_combo");
        }

        return signals;
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
