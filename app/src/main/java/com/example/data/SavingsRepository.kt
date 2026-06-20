package com.example.data

import kotlinx.coroutines.flow.Flow

class SavingsRepository(private val dao: SavingsGoalDao) {
    val allGoals: Flow<List<SavingsGoal>> = dao.getAllGoals()
    val primaryGoalFlow: Flow<SavingsGoal?> = dao.getPrimaryGoalFlow()

    suspend fun getPrimaryGoal(): SavingsGoal? {
        return dao.getPrimaryGoal()
    }

    suspend fun insertGoal(goal: SavingsGoal): Long {
        val id = dao.insertGoal(goal)
        if (goal.isPrimary) {
            dao.clearOtherPrimaryFlags(id.toInt())
        }
        return id
    }

    suspend fun updateGoal(goal: SavingsGoal) {
        dao.updateGoal(goal)
        if (goal.isPrimary) {
            dao.clearOtherPrimaryFlags(goal.id)
        }
    }

    suspend fun deleteGoal(goal: SavingsGoal) {
        dao.deleteGoal(goal)
    }

    suspend fun makePrimary(goalId: Int, goal: SavingsGoal) {
        val updated = goal.copy(isPrimary = true, lastUpdated = System.currentTimeMillis())
        dao.updateGoal(updated)
        dao.clearOtherPrimaryFlags(goalId)
    }
}
