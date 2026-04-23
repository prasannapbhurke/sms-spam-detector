package com.example.spamdetector;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "messages",
        indices = {
                @Index(value = {"messageKey"}, unique = true)
        }
)
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String messageKey;
    public String message;
    public String label;
    public String confidence;
    public String time;
    public String sender;
    public String senderFingerprint;
    public String category;
    public String reasons;
    public String language;
    public boolean hasLink;
}
