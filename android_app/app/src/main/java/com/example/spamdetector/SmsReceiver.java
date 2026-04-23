package com.example.spamdetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Locale;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private LocalPredictor localPredictor;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }
        if (!SettingsStore.isAutoScanEnabled(context)) {
            return;
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) {
            return;
        }

        String sender = messages[0].getDisplayOriginatingAddress();
        StringBuilder combinedBody = new StringBuilder();
        for (SmsMessage smsMessage : messages) {
            combinedBody.append(smsMessage.getMessageBody());
        }

        String messageBody = combinedBody.toString().trim();
        Log.d(TAG, "SMS Received: " + messageBody);

        if (localPredictor == null) {
            localPredictor = new LocalPredictor(context);
        }

        String learnedLabel = MessageStore.getLearnedLabel(context, messageBody, sender);
        if (learnedLabel != null) {
            handleDetection(context, sender, messageBody, learnedLabel, 1f);
            return;
        }

        localPredictor.predictAsync(messageBody, probability -> {
            int repeatedSpamHits = MessageStore.repeatedSpamCount(context, sender);
            RiskAnalyzer.RiskResult hybridRisk = RiskAnalyzer.analyze(messageBody, sender, repeatedSpamHits, probability);
            if ("Spam".equalsIgnoreCase(hybridRisk.label)) {
                handleDetection(context, sender, messageBody, hybridRisk.label, Math.max(probability, 0.55f));
                return;
            }

            ApiHelper.checkSpam(sender, messageBody, new ApiHelper.DetectionCallback() {
                @Override
                public void onResult(String label, float confidence, String sender, String messageText) {
                    handleDetection(context, sender, messageText, label, confidence);
                }

                @Override
                public void onFailure() {
                    float boundedProbability = Math.max(probability, 0f);
                    handleDetection(context, sender, messageBody, hybridRisk.label, Math.max(1f - boundedProbability, 0.55f));
                }
            });
        });
    }

    private void handleDetection(Context context, String sender, String messageBody, String label, float confidence) {
        String safeSender = (sender == null || sender.isEmpty()) ? "Unknown sender" : sender;
        String formattedConfidence = String.format(Locale.getDefault(), "%.0f%%", confidence * 100f);
        int repeatedSpamHits = MessageStore.repeatedSpamCount(context, safeSender);
        RiskAnalyzer.RiskResult risk = RiskAnalyzer.analyze(messageBody, safeSender, repeatedSpamHits, confidence);
        if ("Spam".equalsIgnoreCase(label) || "Safe".equalsIgnoreCase(label)) {
            risk.label = label;
            risk.reasons = risk.reasons + " | Learned from your feedback";
        }
        MessageStore.saveMessage(
                context,
                messageBody,
                risk.label,
                formattedConfidence,
                safeSender,
                java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(new java.util.Date()),
                risk.category,
                risk.reasons + " | " + risk.riskLevel,
                risk.hasLink,
                risk.language
        );
        NotificationHelper.showNotification(context, label, formattedConfidence, safeSender, messageBody);
    }
}
