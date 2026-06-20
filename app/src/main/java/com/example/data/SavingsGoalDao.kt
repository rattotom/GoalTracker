package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY lastUpdated DESC")
    fun getAllGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE isPrimary = 1 LIMIT 1")
    fun getPrimaryGoalFlow(): Flow<SavingsGoal?>

    @Query("SELECT * FROM savings_goals WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryGoal(): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)

    @Query("UPDATE savings_goals SET isPrimary = 0 WHERE id != :primaryId")
    suspend fun clearOtherPrimaryFlags(primaryId: Int)

    @Transaction
    suspend fun setPrimaryGoal(goalId: Int) {
        val all = mutableListOf<SavingsGoal>()
        // Simple transaction logic to ensure only one is primary
        // We'll set the primary flag in our repository or directly here.
    }
}
