package com.kenkho.nutritracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp")
    fun mealsBetween(startMillis: Long, endMillis: Long): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_entries ORDER BY timestamp DESC")
    fun allMeals(): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE id = :id")
    suspend fun byId(id: Long): MealWithItems?

    @Insert
    suspend fun insertEntry(entry: MealEntry): Long

    @Update
    suspend fun updateEntry(entry: MealEntry)

    @Insert
    suspend fun insertItems(items: List<MealItem>)

    @Query("DELETE FROM meal_items WHERE entryId = :entryId")
    suspend fun deleteItemsOf(entryId: Long)

    @Query("DELETE FROM meal_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    /** Saves the meal and replaces its items with [items], in list order. Returns the meal id. */
    @Transaction
    suspend fun save(entry: MealEntry, items: List<MealItem>): Long {
        val id = if (entry.id == 0L) insertEntry(entry) else entry.id.also { updateEntry(entry) }
        deleteItemsOf(id)
        insertItems(items.mapIndexed { i, item -> item.copy(id = 0, entryId = id, position = i) })
        return id
    }

    @Transaction
    suspend fun delete(entryId: Long) {
        deleteItemsOf(entryId)
        deleteEntry(entryId)
    }
}
