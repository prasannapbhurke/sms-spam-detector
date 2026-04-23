package com.example.spamdetector;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FeedbackDao {
    @Insert
    void insert(FeedbackEntity entity);

    @Query("SELECT * FROM feedback ORDER BY id DESC")
    List<FeedbackEntity> getAll();

    @Query("SELECT COUNT(*) FROM feedback")
    int count();
}
