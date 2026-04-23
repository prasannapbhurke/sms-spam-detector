package com.example.spamdetector;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {MessageEntity.class, FeedbackEntity.class}, version = 3, exportSchema = false)
public abstract class MessageDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
    public abstract FeedbackDao feedbackDao();
}
