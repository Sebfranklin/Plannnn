package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val startTime: Long, // timestamp
    val endTime: Long, // timestamp
    val location: String? = null,
    val color: Int = 0xFF2196F3.toInt(), // default light blue
    val isSynced: Boolean = false
)
