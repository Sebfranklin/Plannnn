package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val targetValue: Float, // e.g. 5.0 (for exercise) nebo 100.0 (percents)
    val currentValue: Float,
    val unit: String, // e.g. "times", "hr", "cups"
    val category: String = "Personal", // Wellness, Learning, Productivity
    val deadline: Long, // timestamp
    val remindersActive: Boolean = true
)
