package com.example.spamdetector;

public class MessageModel {
    private String message;
    private String label;
    private String confidence;
    private String time;

    public MessageModel(String message, String label, String confidence, String time) {
        this.message = message;
        this.label = label;
        this.confidence = confidence;
        this.time = time;
    }

    public String getMessage() { return message; }
    public String getLabel() { return label; }
    public String getConfidence() { return confidence; }
    public String getTime() { return time; }
}
