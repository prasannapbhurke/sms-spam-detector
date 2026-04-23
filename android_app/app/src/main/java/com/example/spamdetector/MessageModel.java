package com.example.spamdetector;

public class MessageModel {
    private final String messageKey;
    private final String message;
    private final String label;
    private final String confidence;
    private final String time;
    private final String sender;
    private final String category;
    private final String reasons;
    private final boolean hasLink;

    public MessageModel(String messageKey, String message, String label, String confidence, String time, String sender, String category, String reasons, boolean hasLink) {
        this.messageKey = messageKey;
        this.message = message;
        this.label = label;
        this.confidence = confidence;
        this.time = time;
        this.sender = sender;
        this.category = category;
        this.reasons = reasons;
        this.hasLink = hasLink;
    }

    public String getMessageKey() { return messageKey; }
    public String getMessage() { return message; }
    public String getLabel() { return label; }
    public String getConfidence() { return confidence; }
    public String getTime() { return time; }
    public String getSender() { return sender; }
    public String getCategory() { return category; }
    public String getReasons() { return reasons; }
    public boolean hasLink() { return hasLink; }
    public boolean isLearned() {
        return reasons != null && reasons.toLowerCase().contains("learned from your feedback");
    }

    public boolean isNeedsReview() {
        return getDisplayLabel().equalsIgnoreCase("Needs Review");
    }

    public String getDisplayLabel() {
        if (label == null) {
            return "Safe";
        }
        if (label.equalsIgnoreCase("Spam")) {
            return "Spam";
        }
        if (label.equalsIgnoreCase("Needs Review") || label.equalsIgnoreCase("Review")) {
            return "Needs Review";
        }
        if (label.equalsIgnoreCase("Not Spam") || label.equalsIgnoreCase("Safe")) {
            return "Safe";
        }
        return label;
    }

    public MessageModel withManualLabel(String newLabel) {
        return new MessageModel(
                messageKey,
                message,
                newLabel,
                "100%",
                time,
                sender,
                category == null || category.isEmpty() ? "General" : category,
                "Learned from your feedback",
                hasLink
        );
    }
}
