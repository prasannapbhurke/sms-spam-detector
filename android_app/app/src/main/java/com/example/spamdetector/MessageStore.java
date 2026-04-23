package com.example.spamdetector;

import android.content.Context;
import android.content.Intent;

import androidx.room.Room;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageStore {
    public static final String ACTION_MESSAGES_UPDATED = "com.example.spamdetector.MESSAGES_UPDATED";
    public static final String EXTRA_MESSAGE_KEY = "extra_message_key";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_LABEL = "extra_label";
    public static final String EXTRA_CONFIDENCE = "extra_confidence";
    public static final String EXTRA_TIME = "extra_time";
    public static final String EXTRA_SENDER = "extra_sender";
    public static final String EXTRA_CATEGORY = "extra_category";
    public static final String EXTRA_REASONS = "extra_reasons";
    public static final String EXTRA_HAS_LINK = "extra_has_link";
    private static MessageDatabase database;

    private static synchronized MessageDatabase getDatabase(Context context) {
        if (database == null) {
            database = Room.databaseBuilder(context.getApplicationContext(), MessageDatabase.class, "message_store.db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return database;
    }

    public static String fingerprint(String sender) {
        String safeSender = sender == null ? "" : sender.trim().toLowerCase(Locale.getDefault());
        return safeSender.replaceAll("[^a-z0-9+]", "");
    }

    private static String buildMessageKey(String sender, String message, String time) {
        String safeSender = sender == null ? "" : sender.trim().toLowerCase(Locale.getDefault());
        String safeMessage = message == null ? "" : message.trim().toLowerCase(Locale.getDefault());
        String safeTime = time == null ? "" : time.trim().toLowerCase(Locale.getDefault());
        String raw = safeSender + "|" + safeMessage + "|" + safeTime;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    public static boolean saveMessage(Context context, String message, String label, String confidence, String sender, String time, String category, String reasons, boolean hasLink, String language) {
        return saveMessage(context, message, label, confidence, sender, time, category, reasons, hasLink, language, true);
    }

    public static boolean saveMessage(Context context, String message, String label, String confidence, String sender, String time, String category, String reasons, boolean hasLink, String language, boolean shouldBroadcast) {
        MessageEntity entity = new MessageEntity();
        entity.messageKey = buildMessageKey(sender, message, time);
        entity.message = CryptoManager.encrypt(context, message);
        entity.label = label;
        entity.confidence = confidence;
        entity.sender = CryptoManager.encrypt(context, sender == null || sender.isEmpty() ? "Unknown sender" : sender);
        entity.senderFingerprint = fingerprint(sender);
        entity.time = time;
        entity.category = category == null || category.isEmpty() ? "General" : category;
        entity.reasons = CryptoManager.encrypt(context, reasons == null || reasons.isEmpty() ? "No detailed reason available" : reasons);
        entity.language = language == null || language.isEmpty() ? "English" : language;
        entity.hasLink = hasLink;
        long rowId = getDatabase(context).messageDao().insert(entity);
        if (rowId != -1L) {
            if (shouldBroadcast) {
                Intent updateIntent = new Intent(ACTION_MESSAGES_UPDATED);
                updateIntent.putExtra(EXTRA_MESSAGE_KEY, entity.messageKey);
                updateIntent.putExtra(EXTRA_MESSAGE, message);
                updateIntent.putExtra(EXTRA_LABEL, label);
                updateIntent.putExtra(EXTRA_CONFIDENCE, confidence);
                updateIntent.putExtra(EXTRA_TIME, time);
                updateIntent.putExtra(EXTRA_SENDER, sender == null || sender.isEmpty() ? "Unknown sender" : sender);
                updateIntent.putExtra(EXTRA_CATEGORY, entity.category);
                updateIntent.putExtra(EXTRA_REASONS, reasons == null || reasons.isEmpty() ? "No detailed reason available" : reasons);
                updateIntent.putExtra(EXTRA_HAS_LINK, hasLink);
                context.sendBroadcast(updateIntent);
            }
            return true;
        }
        return false;
    }

    public static List<MessageModel> getMessages(Context context) {
        List<MessageModel> messages = new ArrayList<>();
        List<MessageEntity> entities = getDatabase(context).messageDao().getAll();
        for (MessageEntity entry : entities) {
            messages.add(new MessageModel(
                    entry.messageKey,
                    CryptoManager.decrypt(context, entry.message),
                    entry.label,
                    entry.confidence,
                    entry.time,
                    CryptoManager.decrypt(context, entry.sender),
                    entry.category,
                    CryptoManager.decrypt(context, entry.reasons),
                    entry.hasLink
            ));
        }
        return messages;
    }

    public static int spamCount(Context context) {
        return getDatabase(context).messageDao().spamCount();
    }

    public static int safeCount(Context context) {
        return getDatabase(context).messageDao().safeCount();
    }

    public static int totalCount(Context context) {
        return getDatabase(context).messageDao().totalCount();
    }

    public static int feedbackCount(Context context) {
        return getDatabase(context).feedbackDao().count();
    }

    public static int repeatedSpamCount(Context context, String sender) {
        return getDatabase(context).messageDao().repeatedSpamCount(fingerprint(sender));
    }

    public static boolean applyManualLabel(Context context, MessageModel model, String newLabel) {
        if (model == null || model.getMessageKey() == null || model.getMessageKey().isEmpty()) {
            return false;
        }

        String learnedReason = CryptoManager.encrypt(context, "Learned from your feedback");
        int updated = getDatabase(context).messageDao().updateMessageFeedback(
                model.getMessageKey(),
                newLabel,
                "100%",
                learnedReason,
                model.getCategory() == null ? "General" : model.getCategory()
        );

        saveFeedback(context, model.getMessage(), model.getSender(), newLabel, model.getCategory(), "Manual correction");
        if (updated > 0) {
            context.sendBroadcast(new Intent(ACTION_MESSAGES_UPDATED));
            return true;
        }
        return false;
    }

    public static String getLearnedLabel(Context context, String message, String sender) {
        String normalizedMessage = normalizeForMatch(message);
        String normalizedSender = normalizeForMatch(sender);
        for (FeedbackEntity entity : getDatabase(context).feedbackDao().getAll()) {
            String savedMessage = normalizeForMatch(CryptoManager.decrypt(context, entity.message));
            String savedSender = normalizeForMatch(CryptoManager.decrypt(context, entity.sender));
            if (normalizedMessage.equals(savedMessage) && normalizedSender.equals(savedSender)) {
                return entity.expectedLabel;
            }
        }
        return null;
    }

    private static String normalizeForMatch(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.getDefault()).replaceAll("\\s+", " ");
    }

    public static void saveFeedback(Context context, String message, String sender, String expectedLabel, String category, String notes) {
        FeedbackEntity entity = new FeedbackEntity();
        entity.message = CryptoManager.encrypt(context, message);
        entity.sender = CryptoManager.encrypt(context, sender == null ? "" : sender);
        entity.expectedLabel = expectedLabel;
        entity.category = category == null ? "General" : category;
        entity.notes = CryptoManager.encrypt(context, notes == null ? "" : notes);
        entity.createdAt = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(new java.util.Date());
        getDatabase(context).feedbackDao().insert(entity);
    }
}
