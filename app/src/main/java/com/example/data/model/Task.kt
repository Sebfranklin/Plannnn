package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val dueDate: Long, // timestamp (start of day)
    val dueTime: String? = null, // e.g., "14:30"
    val priority: String = "Medium", // High, Medium, Low
    val isCompleted: Boolean = false,
    val category: String = "General", // Work, Personal, Fitness, etc.
    val remindAt: Long? = null // timestamp
)
