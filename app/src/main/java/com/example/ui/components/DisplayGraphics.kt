package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 1. RICH CIRCULAR RING GAUGE
 * Supports drag gestures around the ring to adjust progress in real-time!
 */
@Composable
fun RingGraphic(
    progress: Float, // 0f to 1f
    currentAmount: Double,
    targetAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onProgressChange: (Float) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(400),
        label = "ring_progress"
    )

    // Resolve density-independent dimensions once for optimized drawing performance
    val density = LocalDensity.current
    val strokeWidthPx = remember(density) { with(density) { 24.dp.toPx() } }
    val beadOuterRadiusPx = remember(density) { with(density) { 10.dp.toPx() } }
    val beadInnerRadiusPx = remember(density) { with(density) { 6.dp.toPx() } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .testTag("ring_graphic"),
        contentAlignment = Alignment.Center
    ) {
        // Draw Ring Gauge with pointer drag
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val sizeVal = size
                        val center = Offset(sizeVal.width / 2f, sizeVal.height / 2f)
                        val touchPoint = change.position
                        
                        // Calculate angle in radians, convert to 0..2pi
                        val dx = touchPoint.x - center.x
                        val dy = touchPoint.y - center.y
                        var angle = atan2(dy, dx)
                        if (angle < 0) angle += (2 * Math.PI).toFloat()
                        
                        // Convert angle to percentage (0..1f) starting at top (-PI/2)
                        var normalizedAngle = angle + (Math.PI / 2f)
                        if (normalizedAngle < 0) normalizedAngle += (2 * Math.PI)
                        if (normalizedAngle > (2 * Math.PI)) normalizedAngle -= (2 * Math.PI)
                        
                        val newProgress = (normalizedAngle / (2 * Math.PI)).toFloat()
                        onProgressChange(newProgress.coerceIn(0f, 1f))
                        change.consume()
                    }
                }
        ) {
            val d = size.minDimension - strokeWidthPx
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val rectSize = Size(d, d)

            // 1. Unfilled Background Arc
            drawArc(
                color = Color(0xFF3E4759).copy(alpha = 0.5f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = rectSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // 2. Custom Sweeping Cool Blue Theme Gradient Arc
            val gradientBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFD0E4FF),
                    Color(0xFF64B5F6),
                    Color(0xFF1E88E5),
                    Color(0xFFD0E4FF)
                ),
                center = Offset(size.width / 2f, size.height / 2f)
            )

            drawArc(
                brush = gradientBrush,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = rectSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // 3. Sliding core pointer handle bead
            val radius = d / 2f
            val radAngle = (animatedProgress * 360f - 90f) * (Math.PI / 180f)
            val handleX = (size.width / 2f) + radius * cos(radAngle).toFloat()
            val handleY = (size.height / 2f) + radius * sin(radAngle).toFloat()

            drawCircle(
                color = Color.White,
                radius = beadOuterRadiusPx,
                center = Offset(handleX, handleY)
            )
            drawCircle(
                color = Color(0xFFD0E4FF),
                radius = beadInnerRadiusPx,
                center = Offset(handleX, handleY)
            )
        }

        // Inside quantitative descriptions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${(progress * 100).toInt().coerceIn(0, 100)}%",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$currencySymbol${String.format("%,.0f", currentAmount)}",
                color = Color(0xFFD0E4FF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Target: $currencySymbol${String.format("%,.0f", targetAmount)}",
                color = Color(0xFF8D9199),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 2. INTERACTIVE SAVINGS JAR
 * Tapping the jar simulates dropping gold coins inside! Play satisfying haptic pops visually.
 */
@Composable
fun JarGraphic(
    progress: Float,
    currentAmount: Double,
    targetAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onProgressChange: (Float) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "jar_progress"
    )

    // Local cached dimensions resolved outside DrawScope
    val density = LocalDensity.current
    val coinRadiusPx = remember(density) { with(density) { 9.dp.toPx() } }
    val borderStrokePx = remember(density) { with(density) { 2.5.dp.toPx() } }
    val lidRadiusPx = remember(density) { with(density) { 4.dp.toPx() } }

    var coinsAddedCount by remember { mutableStateOf(0) }
    val activeCoinsList = remember { mutableStateListOf<Offset>() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .testTag("jar_graphic"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Label & Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap glass to drop golden coins",
                    color = Color(0xFF8D9199),
                    fontSize = 11.sp
                )
                Text(
                    text = "${(progress * 100).toInt().coerceIn(0, 100)}%",
                    color = Color(0xFFD0E4FF),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Liquid Jar Canvas
                Canvas(
                    modifier = Modifier
                        .size(170.dp, 200.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                activeCoinsList.add(Offset(offset.x, offset.y))
                                coinsAddedCount++

                                val currentPct = progress
                                val newProgress = (currentPct + 0.01f).coerceAtMost(1f)
                                onProgressChange(newProgress)

                                scope.launch {
                                    delay(800)
                                    if (activeCoinsList.isNotEmpty()) {
                                        activeCoinsList.removeAt(0)
                                    }
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Jar Glass Body Outline
                    val jarPath = Path().apply {
                        moveTo(w * 0.3f, h * 0.10f)
                        lineTo(w * 0.7f, h * 0.10f)
                        lineTo(w * 0.85f, h * 0.22f)
                        lineTo(w * 0.85f, h * 0.90f)
                        quadraticTo(w * 0.85f, h * 0.95f, w * 0.75f, h * 0.95f)
                        lineTo(w * 0.25f, h * 0.95f)
                        quadraticTo(w * 0.15f, h * 0.95f, w * 0.15f, h * 0.90f)
                        lineTo(w * 0.15f, h * 0.22f)
                        lineTo(w * 0.3f, h * 0.10f)
                        close()
                    }

                    // Background semi-reflective bottle color
                    drawPath(
                        path = jarPath,
                        color = Color(0x13FFFFFF),
                        style = Fill
                    )

                    // Jar lid - Styled as an elegant warm timber cork lid
                    drawRoundRect(
                        color = Color(0xFF8D6E63), // Mahogany wood structure
                        topLeft = Offset(w * 0.28f, h * 0.02f),
                        size = Size(w * 0.44f, h * 0.08f),
                        cornerRadius = CornerRadius(lidRadiusPx, lidRadiusPx)
                    )
                    // High-fidelity cork grain texture lines
                    drawLine(
                        color = Color(0xFF4E342E),
                        start = Offset(w * 0.35f, h * 0.05f),
                        end = Offset(w * 0.65f, h * 0.05f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF5D4037),
                        start = Offset(w * 0.30f, h * 0.08f),
                        end = Offset(w * 0.70f, h * 0.08f),
                        strokeWidth = 1.5f
                    )

                    // 2. Liquid progress - elegant shimmering gold honey gradient
                    val fillHeight = (h * 0.70f) * animatedProgress
                    val fillTopY = (h * 0.92f) - fillHeight

                    if (fillHeight > 0) {
                        val liquidPath = Path().apply {
                            moveTo(w * 0.16f, fillTopY)
                            quadraticTo(w * 0.5f, fillTopY - 8f, w * 0.84f, fillTopY)
                            lineTo(w * 0.84f, h * 0.91f)
                            quadraticTo(w * 0.84f, h * 0.94f, w * 0.73f, h * 0.94f)
                            lineTo(w * 0.27f, h * 0.94f)
                            quadraticTo(w * 0.16f, h * 0.94f, w * 0.16f, h * 0.91f)
                            close()
                        }

                        drawPath(
                            path = liquidPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xEEFFD54F), // Bright golden shimmer top surface
                                    Color(0xEEFFB300), // Amber gold
                                    Color(0xEEF57F17)  // Deep rich cognac gold
                                ),
                                startY = fillTopY,
                                endY = h * 0.95f
                            )
                        )
                    }

                    // 3. Stacking Gold Coins - high quality Dual concentric golden circles
                    val numCoins = (animatedProgress * 15).toInt()
                    val coinPositions = listOf(
                        Offset(w * 0.35f, h * 0.88f), Offset(w * 0.65f, h * 0.88f),
                        Offset(w * 0.50f, h * 0.89f), Offset(w * 0.40f, h * 0.80f),
                        Offset(w * 0.60f, h * 0.80f), Offset(w * 0.52f, h * 0.74f),
                        Offset(w * 0.30f, h * 0.85f), Offset(w * 0.70f, h * 0.83f),
                        Offset(w * 0.45f, h * 0.68f), Offset(w * 0.58f, h * 0.67f),
                        Offset(w * 0.38f, h * 0.63f), Offset(w * 0.62f, h * 0.62f),
                        Offset(w * 0.49f, h * 0.55f), Offset(w * 0.42f, h * 0.48f),
                        Offset(w * 0.55f, h * 0.42f)
                    )

                    for (i in 0 until numCoins.coerceAtMost(coinPositions.size)) {
                        val pos = coinPositions[i]
                        if (pos.y >= fillTopY - 10) {
                            // Coin outer gold rim
                            drawCircle(
                                color = Color(0xFFFFB300),
                                radius = coinRadiusPx,
                                center = pos
                            )
                            // Coin center shimmering light highlights
                            drawCircle(
                                color = Color(0xFFFFEE58),
                                radius = coinRadiusPx * 0.65f,
                                center = pos
                            )
                            // Rim accent border
                            drawCircle(
                                color = Color(0xFFF57F17),
                                radius = coinRadiusPx,
                                center = pos,
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }

                    // 4 Bouncing Tap Particles - glowing drops
                    for (tapPoint in activeCoinsList) {
                        drawCircle(
                            color = Color(0xFFFFF59D),
                            radius = coinRadiusPx * 1.2f,
                            center = tapPoint
                        )
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = coinRadiusPx,
                            center = tapPoint
                        )
                        drawCircle(
                            color = Color(0xFFF57F17),
                            radius = coinRadiusPx * 0.4f,
                            center = tapPoint
                        )
                    }

                    // Sleek Vintage Paper sticker label in the center face of glass jar
                    drawRoundRect(
                        color = Color(0xFAFCEFE2), // Aged paper white
                        topLeft = Offset(w * 0.28f, h * 0.44f),
                        size = Size(w * 0.44f, h * 0.18f),
                        cornerRadius = CornerRadius(with(density) { 4.dp.toPx() })
                    )
                    // Border Frame on sticker label
                    drawRoundRect(
                        color = Color(0xFF795548),
                        topLeft = Offset(w * 0.31f, h * 0.46f),
                        size = Size(w * 0.38f, h * 0.14f),
                        style = Stroke(width = with(density) { 1.dp.toPx() })
                    )
                    // Decorative representational vintage print lines representing typewriter ledger text
                    drawLine(
                        color = Color(0xFF8D6E63),
                        start = Offset(w * 0.36f, h * 0.51f),
                        end = Offset(w * 0.64f, h * 0.51f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF8D6E63),
                        start = Offset(w * 0.34f, h * 0.56f),
                        end = Offset(w * 0.66f, h * 0.56f),
                        strokeWidth = 2f
                    )

                    // 5 Glass Reflection Highlights - sleek glossy vertical sheen on the jar edge
                    val glassHighlight = Path().apply {
                        moveTo(w * 0.22f, h * 0.25f)
                        quadraticTo(w * 0.20f, h * 0.55f, w * 0.22f, h * 0.85f)
                    }
                    drawPath(
                        path = glassHighlight,
                        color = Color.White.copy(alpha = 0.22f),
                        style = Stroke(width = with(density) { 4.dp.toPx() }, cap = StrokeCap.Round)
                    )

                    // 6 Outer high-contrast boundary stroke
                    drawPath(
                        path = jarPath,
                        color = Color(0xFF3E4759),
                        style = Stroke(width = borderStrokePx)
                    )
                }
            }

            Text(
                text = "${currencySymbol}${String.format("%,.2f", currentAmount)} of ${currencySymbol}${String.format("%,.2f", targetAmount)}",
                color = Color(0xFF8D9199),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}

/**
 * 3. CAMPAIGN THERMOMETER DISPLAY
 * Displays the current savings scaled along a thick progress mercury column.
 */
@Composable
fun ThermometerGraphic(
    progress: Float,
    currentAmount: Double,
    targetAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onProgressChange: (Float) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "thermometer_progress"
    )

    // Resolve density measurements outside of DrawScope
    val density = LocalDensity.current
    val bulbRadiusPx = remember(density) { with(density) { 20.dp.toPx() } }
    val tubeWidthPx = remember(density) { with(density) { 14.dp.toPx() } }
    val tubeTopYPx = remember(density) { with(density) { 15.dp.toPx() } }
    val strokeWidthPx = remember(density) { with(density) { 1.5.dp.toPx() } }
    val roundingRadiusPx = remember(density) { with(density) { 7.dp.toPx() } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .testTag("thermometer_graphic"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .width(220.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left milestone clicks
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf(100, 75, 50, 25, 0).forEach { pct ->
                    Text(
                        text = "$pct%",
                        color = if (progress * 100 >= pct) Color(0xFFD0E4FF) else Color(0xFF8D9199),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onProgressChange(pct / 100f)
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Thermometer Tube
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val boundsH = size.height
                            val bulbTopY = boundsH - bulbRadiusPx * 2
                            val tubeHeight = bulbTopY - tubeTopYPx
                            val fraction = 1f - ((offset.y - tubeTopYPx) / tubeHeight)
                            onProgressChange(fraction.coerceIn(0f, 1f))
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                val centerX = w / 2f
                val bulbCenterY = h - bulbRadiusPx - 4f

                val tubeLeft = centerX - (tubeWidthPx / 2f)
                val tubeTop = tubeTopYPx

                // 1 background cylinder
                drawRoundRect(
                    color = Color(0x3D3E4759),
                    topLeft = Offset(tubeLeft, tubeTop),
                    size = Size(tubeWidthPx, bulbCenterY - tubeTop),
                    cornerRadius = CornerRadius(roundingRadiusPx, roundingRadiusPx)
                )

                // background bulb
                drawCircle(
                    color = Color(0x3D3E4759),
                    radius = bulbRadiusPx,
                    center = Offset(centerX, bulbCenterY)
                )

                // 2 mercury fluid mapping
                val activeTopY = bulbCenterY - (bulbCenterY - tubeTop) * animatedProgress

                if (animatedProgress > 0) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFD0E4FF), Color(0xFF42A5F5)),
                            startY = tubeTop,
                            endY = h
                        ),
                        topLeft = Offset(tubeLeft + 2f, activeTopY.coerceAtMost(bulbCenterY)),
                        size = Size(tubeWidthPx - 4f, bulbCenterY - activeTopY.coerceAtLeast(0f)),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                }

                // Fill glowing mercury core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFD0E4FF), Color(0xFF1E88E5)),
                        center = Offset(centerX, bulbCenterY),
                        radius = bulbRadiusPx
                    ),
                    radius = bulbRadiusPx - 2f,
                    center = Offset(centerX, bulbCenterY)
                )

                // Outline tube
                drawRoundRect(
                    color = Color(0xFF3E4759),
                    topLeft = Offset(tubeLeft, tubeTop),
                    size = Size(tubeWidthPx, bulbCenterY - tubeTop),
                    style = Stroke(width = strokeWidthPx),
                    cornerRadius = CornerRadius(roundingRadiusPx, roundingRadiusPx)
                )

                drawCircle(
                    color = Color(0xFF3E4759),
                    radius = bulbRadiusPx,
                    center = Offset(centerX, bulbCenterY),
                    style = Stroke(width = strokeWidthPx)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Milestone description
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "$currencySymbol${String.format("%,.0f", currentAmount)}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                HorizontalDivider(
                    color = Color(0xFF3E4759),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Tap levels on left or inside column to jump levels!",
                    color = Color(0xFF8D9199),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * 4. FINANCIAL BONSAI TREE GRAPHIC
 * Watch your wealth grow in real-time from a simple sprout to a lush golden fortune tree!
 */
@Composable
fun TreeGraphic(
    progress: Float,
    currentAmount: Double,
    targetAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onProgressChange: (Float) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "tree_progress"
    )

    val density = LocalDensity.current
    val strokeWidthPx = remember(density) { with(density) { 1.5.dp.toPx() } }

    var showSparkles by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .testTag("tree_graphic"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌱",
                    fontSize = 22.sp
                )

                // Interactive Water button
                Button(
                    onClick = {
                        showSparkles = true
                        val curr = progress
                        onProgressChange((curr + 0.02f).coerceAtMost(1f))
                        scope.launch {
                            delay(1000)
                            showSparkles = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0E4FF),
                        contentColor = Color(0xFF003258)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("💧 Water", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Canvas drawing
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(200.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val pct = animatedProgress

                    // 1 Draw Pot
                    val potWidth = w * 0.45f
                    val potHeight = h * 0.16f
                    val potLeft = (w - potWidth) / 2f
                    val potTop = h * 0.78f

                    // Soil
                    drawOval(
                        color = Color(0xFF222427),
                        topLeft = Offset(potLeft + 10f, potTop - 6f),
                        size = Size(potWidth - 20f, 12f)
                    )

                    // Pot Shape
                    val potPath = Path().apply {
                        moveTo(potLeft, potTop)
                        lineTo(potLeft + potWidth, potTop)
                        lineTo(potLeft + potWidth - 20f, potTop + potHeight)
                        lineTo(potLeft + 20f, potTop + potHeight)
                        close()
                    }
                    drawPath(path = potPath, color = Color(0xFF3E4759))
                    drawPath(path = potPath, color = Color(0xFF3E4759).copy(alpha = 0.5f), style = Stroke(width = strokeWidthPx))

                    // 2 Draw plant/trunk based on percentage
                    if (pct > 0.02f) {
                        val trunkHeight = (h * 0.35f) * pct
                        val trunkTopY = potTop - trunkHeight
                        val startWidth = 12f * (1f + pct)

                        // Clean robust trunk shape drawing
                        val trPath = Path().apply {
                            moveTo(w * 0.5f - startWidth/2f, potTop)
                            quadraticTo(w * 0.48f, potTop - trunkHeight * 0.5f, w * 0.5f - 4f, trunkTopY)
                            lineTo(w * 0.5f + 4f, trunkTopY)
                            quadraticTo(w * 0.52f, potTop - trunkHeight * 0.5f, w * 0.5f + startWidth/2f, potTop)
                            close()
                        }
                        drawPath(path = trPath, color = Color(0xFF78350F))

                        // Branch stems
                        if (pct >= 0.25f) {
                            drawLine(
                                color = Color(0xFF78350F),
                                start = Offset(w * 0.49f, potTop - trunkHeight * 0.4f),
                                end = Offset(w * 0.38f, potTop - trunkHeight * 0.7f),
                                strokeWidth = 8f,
                                cap = StrokeCap.Round
                            )
                        }

                        if (pct >= 0.50f) {
                            drawLine(
                                color = Color(0xFF78350F),
                                start = Offset(w * 0.51f, potTop - trunkHeight * 0.45f),
                                end = Offset(w * 0.62f, potTop - trunkHeight * 0.75f),
                                strokeWidth = 8f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 3 foliage layers
                    if (pct > 0f && pct < 0.25f) {
                        // Sprout sprout
                        drawCircle(color = Color(0xFF10B981), radius = 6f, center = Offset(w * 0.5f, potTop - 12f))
                    } else if (pct >= 0.25f && pct < 0.50f) {
                        // Young foliage
                        drawCircle(color = Color(0xFF10B981), radius = 18f, center = Offset(w * 0.5f, potTop - (h * 0.35f) * pct))
                        drawCircle(color = Color(0xFF059669), radius = 14f, center = Offset(w * 0.4f, potTop - (h * 0.35f) * pct * 0.9f))
                    } else if (pct >= 0.50f && pct < 0.75f) {
                        val th = (h * 0.35f) * pct
                        drawCircle(color = Color(0xFF047857), radius = 24f, center = Offset(w * 0.5f, potTop - th))
                        drawCircle(color = Color(0xFF10B981), radius = 20f, center = Offset(w * 0.4f, potTop - th + 10f))
                        drawCircle(color = Color(0xFF10B981), radius = 20f, center = Offset(w * 0.6f, potTop - th + 5f))

                        // gold coins
                        drawCircle(color = Color(0xFFFBBF24), radius = 6f, center = Offset(w * 0.48f, potTop - th - 10f))
                        drawCircle(color = Color(0xFFFBBF24), radius = 6f, center = Offset(w * 0.58f, potTop - th + 12f))
                    } else if (pct >= 0.75f) {
                        val th = (h * 0.35f) * pct
                        // fully blossomed coin branches
                        drawCircle(color = Color(0xFF065F46), radius = 30f, center = Offset(w * 0.5f, potTop - th))
                        drawCircle(color = Color(0xFF047857), radius = 24f, center = Offset(w * 0.36f, potTop - th * 0.8f))
                        drawCircle(color = Color(0xFF047857), radius = 24f, center = Offset(w * 0.64f, potTop - th * 0.85f))

                        // glowing gold leaves
                        drawCircle(color = Color(0xFFFBBF24), radius = 7f, center = Offset(w * 0.5f, potTop - th - 15f))
                        drawCircle(color = Color(0xFFF59E0B), radius = 8f, center = Offset(w * 0.42f, potTop - th + 12f))
                        drawCircle(color = Color(0xFFFBBF24), radius = 7f, center = Offset(w * 0.58f, potTop - th))
                        drawCircle(color = Color(0xFFFBBF24), radius = 7f, center = Offset(w * 0.34f, potTop - th * 0.75f - 10f))
                        drawCircle(color = Color(0xFFFBBF24), radius = 7f, center = Offset(w * 0.66f, potTop - th * 0.8f - 10f))
                    }

                    // Water splashes
                    if (showSparkles) {
                        drawCircle(color = Color(0xFF60A5FA), radius = 4f, center = Offset(w * 0.42f, potTop - 35f))
                        drawCircle(color = Color(0xFF60A5FA), radius = 5f, center = Offset(w * 0.52f, potTop - 45f))
                        drawCircle(color = Color(0xFF60A5FA), radius = 4f, center = Offset(w * 0.62f, potTop - 38f))
                    }
                }
            }

            // Stats footer
            Text(
                text = if (progress >= 1f) "Wealth tree is fully mature! 🌟" else "Keep adding to nurture your wealth Bonsai!",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

/**
 * 5. GRID VIEW INFOGRAPHIC DISPLAY
 * Shows a beautiful 10x10 minimalist grid of rounded squares / dots to visualize 100% savings progress.
 * Highly responsive, black and grey themed, blending perfectly with minimalist/Pixel designs.
 */
@Composable
fun GridViewInfographic(
    progress: Float,
    currentAmount: Double,
    targetAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val pct = (progress * 100).toInt().coerceIn(0, 100)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Minimalist description of the grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$pct% Completed",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Target: $currencySymbol${String.format("%,.0f", targetAmount)}",
                color = Color(0xFF8D9199),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 10x10 grid with beautifully styled rounded boxes that scale dynamically
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val rows = 10
            val cols = 10
            val gap = 4.dp
            
            val totalSpacingX = gap * (cols - 1)
            val totalSpacingY = gap * (rows - 1)
            
            val availableWidth = maxWidth - 16.dp
            val availableHeight = maxHeight - 16.dp
            
            // Dynamic cell calculation targeting perfect aspect-ratio fitting in BOTH width and height:
            val sizeByWidth = (availableWidth - totalSpacingX) / cols
            val sizeByHeight = (availableHeight - totalSpacingY) / rows
            
            // Cells are guaranteed to be square as they use size(computedSize)
            val computedSize = minOf(sizeByWidth, sizeByHeight).coerceAtLeast(4.dp)
            val cornerRadius = (computedSize * 0.2f).coerceAtLeast(1.5.dp)

            Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val totalBlocks = rows * cols
                val filledBlocks = pct.coerceIn(0, totalBlocks)
                for (row in 0 until rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0 until cols) {
                            val index = row * cols + col
                            val isFilled = index < filledBlocks
                            val color = if (isFilled) Color.White else Color(0xFF333333)
                            Box(
                                modifier = Modifier
                                    .size(computedSize)
                                    .clip(RoundedCornerShape(cornerRadius))
                                    .background(color)
                                    .testTag("grid_dot_${index}")
                            )
                        }
                    }
                }
            }
        }
    }
}

