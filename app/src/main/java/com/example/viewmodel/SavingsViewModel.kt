package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SavingsApplication
import com.example.data.SavingsGoal
import com.example.widget.SavingsWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as SavingsApplication).repository

    val allGoals: StateFlow<List<SavingsGoal>> = repository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val primaryGoal: StateFlow<SavingsGoal?> = repository.primaryGoalFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun initializeDefaultGoalIfNeeded() {
        viewModelScope.launch {
            val currentPrimary = repository.getPrimaryGoal()
            if (currentPrimary == null) {
                val defaultGoal = SavingsGoal(
                    title = "Savings Goal",
                    targetAmount = 10000.0,
                    currentAmount = 2500.0,
                    currencySymbol = "$",
                    isPrimary = true,
                    displayStyle = "ring",
                    lastUpdated = System.currentTimeMillis()
                )
                repository.insertGoal(defaultGoal)
                notifyWidget()
            }
        }
    }

    fun updateGoalProgress(goal: SavingsGoal, progressFraction: Float) {
        viewModelScope.launch {
            val nextAmount = (goal.targetAmount * progressFraction.coerceIn(0f, 1f))
            val updated = goal.copy(
                currentAmount = nextAmount.coerceIn(0.0, goal.targetAmount),
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateGoal(updated)
            notifyWidget()
        }
    }

    fun updateGoalAmount(goal: SavingsGoal, amount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(
                currentAmount = amount.coerceIn(0.0, goal.targetAmount),
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateGoal(updated)
            notifyWidget()
        }
    }

    fun updateStyleTheme(goal: SavingsGoal, style: String) {
        viewModelScope.launch {
            val updated = goal.copy(
                displayStyle = style,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateGoal(updated)
            notifyWidget()
        }
    }

    fun updateGoalSettings(goal: SavingsGoal, title: String, target: Double, style: String, currency: String) {
        viewModelScope.launch {
            val updated = goal.copy(
                title = title,
                targetAmount = target,
                displayStyle = style,
                currencySymbol = currency,
                currentAmount = goal.currentAmount.coerceAtMost(target),
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateGoal(updated)
            notifyWidget()
        }
    }

    fun setPrimaryGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.makePrimary(goal.id, goal)
            notifyWidget()
        }
    }

    fun addNewGoal(title: String, target: Double, style: String = "ring", currency: String = "$") {
        viewModelScope.launch {
            val newGoal = SavingsGoal(
                title = if (title.isNotBlank()) title else "Savings Goal",
                targetAmount = if (target > 0) target else 100.0,
                currentAmount = 0.0,
                displayStyle = style,
                currencySymbol = currency,
                isPrimary = true,
                lastUpdated = System.currentTimeMillis()
            )
            repository.insertGoal(newGoal)
            notifyWidget()
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            notifyWidget()
        }
    }

    private fun notifyWidget() {
        SavingsWidgetProvider.updateAllWidgets(getApplication())
    }
}
