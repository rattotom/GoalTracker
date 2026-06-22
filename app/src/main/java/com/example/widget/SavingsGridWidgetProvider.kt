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
import kotlin.math.roundToInt
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class SavingsGridWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
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

            if (goal == null) {
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
                    val views = RemoteViews(context.packageName, R.layout.widget_savings_grid_layout)

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
                        
                        // Draw pristine 10x10 Grid View infographic (100% fidelity) on a Bitmap:
                        val size = 300
                        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        
                        val paintFilled = Paint().apply {
                            color = android.graphics.Color.WHITE
                            style = Paint.Style.FILL
                            isAntiAlias = true
                        }
                        
                        val paintUnfilled = Paint().apply {
                            color = android.graphics.Color.parseColor("#333333")
                            style = Paint.Style.FILL
                            isAntiAlias = true
                        }
                        
                        val rows = 10
                        val cols = 10
                        val spacing = 5f
                        val cellSize = (size - (spacing * (cols - 1))) / cols
                        val cornerRadius = cellSize * 0.2f
                        
                        val totalBlocks = rows * cols
                        val filledBlocks = pct.coerceIn(0, totalBlocks)
                        
                        for (row in 0 until rows) {
                            for (col in 0 until cols) {
                                val index = row * cols + col
                                val left = col * (cellSize + spacing)
                                val top = row * (cellSize + spacing)
                                val right = left + cellSize
                                val bottom = top + cellSize
                                
                                val paint = if (index < filledBlocks) paintFilled else paintUnfilled
                                canvas.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, paint)
                            }
                        }
                        
                        views.setImageViewBitmap(R.id.widget_grid_canvas, bitmap)
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
                        101,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_open, openAppPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

                    manager.updateAppWidget(id, views)
                }
            }
        }
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SavingsGridWidgetProvider::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode() + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_SUB_10 = "com.example.widget.GRID_ACTION_SUB_10"
        const val ACTION_ADD_10 = "com.example.widget.GRID_ACTION_ADD_10"
        const val ACTION_ADD_50 = "com.example.widget.GRID_ACTION_ADD_50"

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
