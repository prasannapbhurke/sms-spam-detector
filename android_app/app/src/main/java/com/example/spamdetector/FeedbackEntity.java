package com.example.spamdetector;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "feedback")
public class FeedbackEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String message;
    public String sender;
    public String expectedLabel;
    public String category;
    public String notes;
    public String createdAt;
}
