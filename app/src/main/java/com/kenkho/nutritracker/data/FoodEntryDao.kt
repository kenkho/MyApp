package com.kenkho.nutritracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp")
    fun entriesBetween(startMillis: Long, endMillis: Long): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC")
    fun allEntries(): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE id = :id")
    suspend fun byId(id: Long): FoodEntry?

    @Upsert
    suspend fun upsert(entry: FoodEntry): Long

    @Delete
    suspend fun delete(entry: FoodEntry)
}
