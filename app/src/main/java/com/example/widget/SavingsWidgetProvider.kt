package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.SavingsGoal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavingsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Trigger background lookup and view bind
        updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        if (action == ACTION_SUB_10 || action == ACTION_ADD_10 || action == ACTION_ADD_50) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.savingsGoalDao()
                    var primary = dao.getPrimaryGoal()

                    // If empty, let's create a default goal so the action is interactive immediately!
                    if (primary == null) {
                        val defaultGoal = SavingsGoal(
                            title = "Main Savings Goal",
                            targetAmount = 5000.0,
                            currentAmount = 1500.0,
                            isPrimary = true,
                            displayStyle = "ring"
                        )
                        val newId = dao.insertGoal(defaultGoal)
                        primary = defaultGoal.copy(id = newId.toInt())
                    }

                    primary?.let { goal ->
                        val delta = when (action) {
                            ACTION_SUB_10 -> -10.0
                            ACTION_ADD_10 -> 10.0
                            ACTION_ADD_50 -> 50.0
                            else -> 0.0
                        }
                        if (delta != 0.0) {
                            val nextAmount = (goal.currentAmount + delta).coerceIn(0.0, goal.targetAmount)
                            val updated = goal.copy(
                                currentAmount = nextAmount,
                                lastUpdated = System.currentTimeMillis()
                            )
                            dao.updateGoal(updated)
                            updateAllWidgets(context)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun updateWidgetsAsync(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val dao = db.savingsGoalDao()
            var goal = dao.getPrimaryGoal()

            // If zero goals, fetch all and use first, or create a default
            if (goal == null) {
                // Let's create a default one
                val defaultGoal = SavingsGoal(
                    title = "Main Savings Goal",
                    targetAmount = 5000.0,
                    currentAmount = 1500.0,
                    isPrimary = true,
                    displayStyle = "ring"
                )
                val newId = dao.insertGoal(defaultGoal)
                goal = defaultGoal.copy(id = newId.toInt())
            }

            launch(Dispatchers.Main) {
                for (id in ids) {
                    val views = RemoteViews(context.packageName, R.layout.widget_savings_layout)

                    goal?.let { g ->
                        val pct = if (g.targetAmount > 0) {
                            ((g.currentAmount / g.targetAmount) * 100).toInt().coerceIn(0, 100)
                        } else 0

                        views.setTextViewText(R.id.widget_title, g.title)
                        views.setTextViewText(R.id.widget_percentage, "$pct%")
                        
                        val currency = g.currencySymbol
                        val formattedCurrent = String.format("%.0f", g.currentAmount)
                        val formattedTarget = String.format("%.0f", g.targetAmount)
                        views.setTextViewText(
                            R.id.widget_amount_status,
                            "$currency$formattedCurrent of $currency$formattedTarget saved"
                        )
                        
                        views.setProgressBar(R.id.widget_progress_bar, 100, pct, false)
                    }

                    // Set up pending intents for active buttons
                    views.setOnClickPendingIntent(
                        R.id.widget_btn_sub10,
                        getPendingSelfIntent(context, ACTION_SUB_10)
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_btn_add10,
                        getPendingSelfIntent(context, ACTION_ADD_10)
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_btn_add50,
                        getPendingSelfIntent(context, ACTION_ADD_50)
                    )

                    // Button: Open App
                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val openAppPendingIntent = PendingIntent.getActivity(
                        context,
                        100,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_open, openAppPendingIntent)

                    manager.updateAppWidget(id, views)
                }
            }
        }
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SavingsWidgetProvider::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_SUB_10 = "com.example.widget.ACTION_SUB_10"
        const val ACTION_ADD_10 = "com.example.widget.ACTION_ADD_10"
        const val ACTION_ADD_50 = "com.example.widget.ACTION_ADD_50"

        fun updateAllWidgets(context: Context) {
            // Update regular savings widget
            val mainIntent = Intent(context, SavingsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val mainIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, SavingsWidgetProvider::class.java)
            )
            mainIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mainIds)
            context.sendBroadcast(mainIntent)

            // Update grid savings widget
            val gridIntent = Intent(context, SavingsGridWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val gridIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, SavingsGridWidgetProvider::class.java)
            )
            gridIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, gridIds)
            context.sendBroadcast(gridIntent)
        }
    }
}
