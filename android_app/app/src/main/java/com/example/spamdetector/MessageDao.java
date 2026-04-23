package com.example.spamdetector;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY id DESC LIMIT :limit")
    List<MessageEntity> getRecent(int limit);

    @Query("SELECT * FROM messages ORDER BY id DESC")
    List<MessageEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(MessageEntity entity);

    @Query("SELECT COUNT(*) FROM messages WHERE label = 'Spam'")
    int spamCount();

    @Query("SELECT COUNT(*) FROM messages WHERE label != 'Spam'")
    int safeCount();

    @Query("SELECT COUNT(*) FROM messages")
    int totalCount();

    @Query("SELECT COUNT(*) FROM messages WHERE senderFingerprint = :senderFingerprint AND label = 'Spam'")
    int repeatedSpamCount(String senderFingerprint);

    @Query("UPDATE messages SET label = :label, confidence = :confidence, reasons = :reasons, category = :category WHERE messageKey = :messageKey")
    int updateMessageFeedback(String messageKey, String label, String confidence, String reasons, String category);
}
