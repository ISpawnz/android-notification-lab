package com.spawn.capture.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: CapturedEvent): Long

    @Query("SELECT * FROM events ORDER BY timestamp ASC LIMIT 500")
    suspend fun pending(): List<CapturedEvent>

    @Query("DELETE FROM events WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)
}
