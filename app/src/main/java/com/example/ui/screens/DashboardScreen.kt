package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SavingsGoal
import com.example.ui.components.GridViewInfographic
import com.example.ui.components.JarGraphic
import com.example.ui.components.RingGraphic
import com.example.ui.components.ThermometerGraphic
import com.example.ui.components.TreeGraphic
import com.example.viewmodel.SavingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: SavingsViewModel,
    modifier: Modifier = Modifier
) {
    // Collect reactive Room DB states via ViewModel
    val goals by viewModel.allGoals.collectAsStateWithLifecycle()
    val primaryGoal by viewModel.primaryGoal.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    var isCreateSheetOpen by remember { mutableStateOf(false) }
    var isCustomContributionOpen by remember { mutableStateOf(false) }

    // Proactively initialize a default "New Car" target if none exists so that the user immediately sees a working app
    LaunchedEffect(Unit) {
        viewModel.initializeDefaultGoalIfNeeded()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF1A1C1E)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1A1C1E))
        ) {
            if (goals.isEmpty()) {
                // Empty state illustration fallback
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty",
                        tint = Color(0xFF3E4759),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Build a wealthy path!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create your first savings target to visualize progress",
                        color = Color(0xFF8D9199),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { isCreateSheetOpen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0E4FF),
                            contentColor = Color(0xFF003258)
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("empty_state_create_button")
                    ) {
                        Text("Create Savings Goal", color = Color(0xFF003258), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                primaryGoal?.let { goal ->
                    // Main layout with horizontal bounds alignment for large screens or tablet foldables
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp, bottom = 24.dp)
                            .widthIn(max = 600.dp)
                            .align(Alignment.TopCenter),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Sleek IMMERSIVE HEADER (replaces the redundant scaffold title and card)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = goal.title.uppercase(),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "SAVINGS GOAL",
                                    color = Color(0xFF8D9199),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            IconButton(
                                onClick = { isCreateSheetOpen = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3E4759).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configure Goal",
                                    tint = Color(0xFFD0E4FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 2. Main interactive Graphic Display wrapper
                        val computedProgress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF222427)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.3f)
                                    .testTag("graphic_view_card")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    when (goal.displayStyle) {
                                        "grid" -> GridViewInfographic(
                                            progress = computedProgress,
                                            currentAmount = goal.currentAmount,
                                            targetAmount = goal.targetAmount,
                                            currencySymbol = goal.currencySymbol,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        "ring" -> RingGraphic(
                                            progress = computedProgress,
                                            currentAmount = goal.currentAmount,
                                            targetAmount = goal.targetAmount,
                                            currencySymbol = goal.currencySymbol,
                                            modifier = Modifier.fillMaxSize(),
                                            onProgressChange = { nextFraction ->
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                viewModel.updateGoalProgress(goal, nextFraction)
                                            }
                                        )
                                        "jar" -> JarGraphic(
                                            progress = computedProgress,
                                            currentAmount = goal.currentAmount,
                                            targetAmount = goal.targetAmount,
                                            currencySymbol = goal.currencySymbol,
                                            modifier = Modifier.fillMaxSize(),
                                            onProgressChange = { nextFraction ->
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                viewModel.updateGoalProgress(goal, nextFraction)
                                            }
                                        )
                                        "thermometer" -> ThermometerGraphic(
                                            progress = computedProgress,
                                            currentAmount = goal.currentAmount,
                                            targetAmount = goal.targetAmount,
                                            currencySymbol = goal.currencySymbol,
                                            modifier = Modifier.fillMaxSize(),
                                            onProgressChange = { nextFraction ->
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                viewModel.updateGoalProgress(goal, nextFraction)
                                            }
                                        )
                                        "tree" -> TreeGraphic(
                                            progress = computedProgress,
                                            currentAmount = goal.currentAmount,
                                            targetAmount = goal.targetAmount,
                                            currencySymbol = goal.currencySymbol,
                                            modifier = Modifier.fillMaxSize(),
                                            onProgressChange = { nextFraction ->
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                viewModel.updateGoalProgress(goal, nextFraction)
                                            }
                                        )
                                        else -> GridViewInfographic(
                                            progress = computedProgress,
                                            currentAmount = goal.currentAmount,
                                            targetAmount = goal.targetAmount,
                                            currencySymbol = goal.currencySymbol,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                        // 2b. Grid-Style Financial Counter Card
                        val currentAmount = goal.currentAmount
                            val targetAmount = goal.targetAmount
                            val currencySymbol = goal.currencySymbol
                            val remaining = (targetAmount - currentAmount).coerceAtLeast(0.0)
                            val pct = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt().coerceIn(0, 100) else 0

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF222427)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.1f)
                                    .testTag("grid_counter_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceAround
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "CURRENT SAVINGS",
                                                color = Color(0xFF8D9199),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$currencySymbol${String.format("%,.2f", currentAmount)}",
                                                color = Color(0xFFD0E4FF),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(40.dp)
                                                .background(Color(0xFF3E4759).copy(alpha = 0.5f))
                                        )
                                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                            Text(
                                                text = "TOTAL TARGET",
                                                color = Color(0xFF8D9199),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$currencySymbol${String.format("%,.2f", targetAmount)}",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0xFF3E4759).copy(alpha = 0.3f))
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "REMAINING BALANCE",
                                                color = Color(0xFF8D9199),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$currencySymbol${String.format("%,.2f", remaining)}",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(40.dp)
                                                .background(Color(0xFF3E4759).copy(alpha = 0.5f))
                                        )
                                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                            Text(
                                                text = "COMPLETION RATE",
                                                color = Color(0xFF8D9199),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$pct% Complete",
                                                color = Color(0xFFD0E4FF),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                        // 3. Compact Slider/Manual Value Controller Card
                        Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF222427)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.9f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceAround
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Contribute",
                                            color = Color(0xFF8D9199),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val pctText = "${((goal.currentAmount / goal.targetAmount) * 100).toInt().coerceIn(0, 100)}% Saved"
                                        Text(
                                            text = pctText,
                                            color = Color(0xFFD0E4FF),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    val currentPct = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                                    
                                    Slider(
                                        value = currentPct,
                                        onValueChange = { nextPct ->
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            viewModel.updateGoalProgress(goal, nextPct)
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color(0xFFD0E4FF),
                                            inactiveTrackColor = Color(0xFF3E4759)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp) // Tighter slider bounds
                                            .testTag("manual_slider")
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Dynamic quick budget incremental buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val target = goal.targetAmount
                                        val step1 = getNiceIncrement(target * 0.005)
                                        val step2 = getNiceIncrement(target * 0.02)
                                        val step3 = getNiceIncrement(target * 0.05)
                                        val increments = listOf(step1, step2, step3).map { it.coerceAtLeast(1.0) }.distinct()

                                        increments.forEach { increment ->
                                            val textLabel = when {
                                                increment >= 1_000_000.0 -> "+${String.format("%.1fM", increment / 1_000_000.0).replace(".0", "")}"
                                                increment >= 1_000.0 -> "+${String.format("%.1fK", increment / 1_000.0).replace(".0", "")}"
                                                increment % 1.0 == 0.0 -> "+${String.format("%.0f", increment)}"
                                                else -> "+${String.format("%.2f", increment)}"
                                            }

                                            Button(
                                                onClick = {
                                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                    val nextAmt = (goal.currentAmount + increment).coerceAtMost(goal.targetAmount)
                                                    viewModel.updateGoalAmount(goal, nextAmt)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF3E4759).copy(alpha = 0.5f),
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp) // Tighter, smaller button heights while keeping layout compact
                                                    .testTag("add_budget_${increment}")
                                            ) {
                                                Text(
                                                    text = textLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Custom Contribution Button replacing the fourth add option
                                        Button(
                                            onClick = {
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                isCustomContributionOpen = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD0E4FF),
                                                contentColor = Color(0xFF003258)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("add_custom_contribution_button")
                                        ) {
                                            Text(
                                                text = "Custom",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                    }
                }
            }
        }
    }

    // Modern Dialog modal to configure Goal Settings
    if (isCreateSheetOpen) {
        val currentGoal = primaryGoal
        var newTitle by remember(isCreateSheetOpen) { mutableStateOf(currentGoal?.title ?: "Savings Goal") }
        var newTarget by remember(isCreateSheetOpen) { mutableStateOf(currentGoal?.targetAmount?.toString() ?: "") }
        var selectedStyle by remember(isCreateSheetOpen) { mutableStateOf(currentGoal?.displayStyle ?: "grid") }
        var selectedCurrency by remember(isCreateSheetOpen) { mutableStateOf(currentGoal?.currencySymbol ?: "$") }
        var validationErrorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { isCreateSheetOpen = false },
            containerColor = Color(0xFF222427),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Goal Name input
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Goal Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0E4FF),
                            unfocusedBorderColor = Color(0xFF3E4759),
                            focusedLabelColor = Color(0xFFD0E4FF),
                            unfocusedLabelColor = Color(0xFF8D9199),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_goal_title")
                    )

                    OutlinedTextField(
                        value = newTarget,
                        onValueChange = { newTarget = it },
                        label = { Text("Target Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0E4FF),
                            unfocusedBorderColor = Color(0xFF3E4759),
                            focusedLabelColor = Color(0xFFD0E4FF),
                            unfocusedLabelColor = Color(0xFF8D9199),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_goal_target")
                    )

                    // Currency chooser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Currency Symbol: ", color = Color(0xFF8D9199), fontSize = 13.sp)
                        listOf("$", "€", "£", "¥", "₹").forEach { char ->
                            val isSel = selectedCurrency == char
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFFD0E4FF) else Color(0xFF3E4759).copy(alpha = 0.4f))
                                    .clickable { selectedCurrency = char }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = char,
                                    color = if (isSel) Color(0xFF003258) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Display Graphic Mode dropdown segment selector
                    Text("Display Graphic Mode:", color = Color(0xFF8D9199), fontSize = 13.sp)
                    val styleOptions = listOf(
                        StyleBtn("grid", "Grid", Icons.AutoMirrored.Filled.List),
                        StyleBtn("ring", "Gauge", Icons.Default.Star),
                        StyleBtn("jar", "Jar", Icons.Default.Favorite),
                        StyleBtn("thermometer", "Glass", Icons.Default.PlayArrow),
                        StyleBtn("tree", "Bonsai", Icons.Default.Home)
                    )
                    val firstRow = styleOptions.take(3)
                    val secondRow = styleOptions.drop(3)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            firstRow.forEach { option ->
                                val isChosen = selectedStyle == option.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) Color(0xFFD0E4FF) else Color(0xFF3E4759).copy(alpha = 0.4f))
                                        .clickable { selectedStyle = option.id }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.label,
                                        color = if (isChosen) Color(0xFF003258) else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            secondRow.forEach { option ->
                                val isChosen = selectedStyle == option.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) Color(0xFFD0E4FF) else Color(0xFF3E4759).copy(alpha = 0.4f))
                                        .clickable { selectedStyle = option.id }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.label,
                                        color = if (isChosen) Color(0xFF003258) else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f))
                        }
                    }

                    // Live Visual Preview block of the chosen style
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131416))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewProgress = 0.65f
                        when (selectedStyle) {
                            "grid" -> GridViewInfographic(
                                progress = previewProgress,
                                currentAmount = 65.0,
                                targetAmount = 100.0,
                                currencySymbol = selectedCurrency,
                                modifier = Modifier.fillMaxWidth()
                            )
                            "ring" -> RingGraphic(
                                progress = previewProgress,
                                currentAmount = 65.0,
                                targetAmount = 100.0,
                                currencySymbol = selectedCurrency,
                                onProgressChange = {}
                            )
                            "jar" -> JarGraphic(
                                progress = previewProgress,
                                currentAmount = 65.0,
                                targetAmount = 100.0,
                                currencySymbol = selectedCurrency,
                                onProgressChange = {}
                            )
                            "thermometer" -> ThermometerGraphic(
                                progress = previewProgress,
                                currentAmount = 65.0,
                                targetAmount = 100.0,
                                currencySymbol = selectedCurrency,
                                onProgressChange = {}
                            )
                            "tree" -> TreeGraphic(
                                progress = previewProgress,
                                currentAmount = 65.0,
                                targetAmount = 100.0,
                                currencySymbol = selectedCurrency,
                                onProgressChange = {}
                            )
                        }
                    }

                    validationErrorMsg?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val numTarget = newTarget.toDoubleOrNull()
                        val finalTitle = newTitle.trim()
                        if (finalTitle.isEmpty()) {
                            validationErrorMsg = "Goal name cannot be empty"
                        } else if (numTarget == null || numTarget <= 0) {
                            validationErrorMsg = "Target must be a positive number"
                        } else {
                            if (currentGoal != null) {
                                viewModel.updateGoalSettings(
                                    goal = currentGoal,
                                    title = finalTitle,
                                    target = numTarget,
                                    style = selectedStyle,
                                    currency = selectedCurrency
                                )
                            } else {
                                viewModel.addNewGoal(
                                    title = finalTitle,
                                    target = numTarget,
                                    style = selectedStyle,
                                    currency = selectedCurrency
                                )
                            }
                            isCreateSheetOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0E4FF),
                        contentColor = Color(0xFF003258)
                    ),
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text("Save", color = Color(0xFF003258), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isCreateSheetOpen = false }) {
                    Text("Cancel", color = Color(0xFF8D9199))
                }
            }
        )
    }

    if (isCustomContributionOpen) {
        val currentGoal = primaryGoal
        var contributionInput by remember(isCustomContributionOpen) { mutableStateOf("") }
        var inputErrorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { isCustomContributionOpen = false },
            containerColor = Color(0xFF222427),
            title = {
                Text(
                    text = "Custom Contribution",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the amount you would like to contribute to your goal:",
                        color = Color(0xFF8D9199),
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = contributionInput,
                        onValueChange = {
                            contributionInput = it
                            inputErrorMsg = null
                        },
                        label = { Text("Contribution Amount (${currentGoal?.currencySymbol ?: "$"})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0E4FF),
                            unfocusedBorderColor = Color(0xFF3E4759),
                            focusedLabelColor = Color(0xFFD0E4FF),
                            unfocusedLabelColor = Color(0xFF8D9199),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_contribution")
                    )

                    inputErrorMsg?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = contributionInput.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            inputErrorMsg = "Please enter a positive amount"
                        } else {
                            if (currentGoal != null) {
                                val nextAmt = (currentGoal.currentAmount + amount).coerceAtMost(currentGoal.targetAmount)
                                viewModel.updateGoalAmount(currentGoal, nextAmt)
                            }
                            isCustomContributionOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0E4FF),
                        contentColor = Color(0xFF003258)
                    ),
                    modifier = Modifier.testTag("dialog_custom_contribution_confirm")
                ) {
                    Text("Add", color = Color(0xFF003258), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isCustomContributionOpen = false }) {
                    Text("Cancel", color = Color(0xFF8D9199))
                }
            }
        )
    }
}

private data class StyleBtn(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun getNiceIncrement(value: Double): Double {
    return when {
        value < 1.0 -> 1.0
        value < 5.0 -> kotlin.math.round(value).coerceAtLeast(1.0)
        value < 25.0 -> (kotlin.math.round(value / 5.0) * 5.0).coerceAtLeast(5.0)
        value < 100.0 -> (kotlin.math.round(value / 10.0) * 10.0).coerceAtLeast(10.0)
        value < 500.0 -> (kotlin.math.round(value / 50.0) * 50.0).coerceAtLeast(50.0)
        value < 2500.0 -> (kotlin.math.round(value / 100.0) * 100.0).coerceAtLeast(100.0)
        value < 10000.0 -> (kotlin.math.round(value / 500.0) * 500.0).coerceAtLeast(500.0)
        value < 50000.0 -> (kotlin.math.round(value / 1000.0) * 1000.0).coerceAtLeast(1000.0)
        else -> (kotlin.math.round(value / 5000.0) * 5000.0).coerceAtLeast(5000.0)
    }
}

