package com.spawn.capture.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class CapturedEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val direction: String,
    val title: String?,
    val text: String?,
    val timestamp: Long
)
