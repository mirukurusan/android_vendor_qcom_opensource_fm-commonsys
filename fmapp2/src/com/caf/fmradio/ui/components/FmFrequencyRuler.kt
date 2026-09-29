/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.caf.fmradio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.caf.fmradio.ui.theme.RadioDialIndicator
import kotlin.math.roundToInt

/**
 * Modern interactive frequency ruler/dial with smooth drag gestures,
 * magnetic tick snapping, and haptic feedback.
 */
@Composable
fun FmFrequencyRuler(
    currentFrequencyKHz: Int,
    minFrequencyKHz: Int,
    maxFrequencyKHz: Int,
    stepSizeKHz: Int,
    enabled: Boolean,
    onFrequencyChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Pixels per step (e.g. 100 kHz = 16 dp)
    val stepWidthPx = with(density) { 16.dp.toPx() }

    // Internal dragged frequency tracker to avoid state jumping during continuous drag
    var draggedFrequency by remember { mutableFloatStateOf(currentFrequencyKHz.toFloat()) }
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Sync with external frequency changes when not actively dragging
    LaunchedEffect(currentFrequencyKHz, isDragging) {
        if (!isDragging) {
            draggedFrequency = currentFrequencyKHz.toFloat()
        }
    }

    val currentOnFrequencyChanged by rememberUpdatedState(onFrequencyChanged)

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(MaterialTheme.shapes.large)
            .background(surfaceColor)
            .pointerInput(enabled, minFrequencyKHz, maxFrequencyKHz, stepSizeKHz) {
                if (!enabled) return@pointerInput

                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        // Magnetic snap to nearest valid step
                        val snapped = snapToStep(
                            draggedFrequency.roundToInt(),
                            minFrequencyKHz,
                            maxFrequencyKHz,
                            stepSizeKHz
                        )
                        draggedFrequency = snapped.toFloat()
                        currentOnFrequencyChanged(snapped)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragCancel = {
                        isDragging = false
                        draggedFrequency = currentFrequencyKHz.toFloat()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        // Dragging left moves to higher frequencies, dragging right to lower
                        val deltaKHz = -(dragAmount / stepWidthPx) * stepSizeKHz
                        val newFreq = (draggedFrequency + deltaKHz).coerceIn(
                            minFrequencyKHz.toFloat(),
                            maxFrequencyKHz.toFloat()
                        )
                        val prevStep = (draggedFrequency / stepSizeKHz).roundToInt()
                        val newStep = (newFreq / stepSizeKHz).roundToInt()

                        draggedFrequency = newFreq

                        // Trigger haptic click when crossing each step
                        if (prevStep != newStep) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val rulerHeight = size.height

            val currentFreq = draggedFrequency
            val startFreq = (currentFreq - (centerX / stepWidthPx) * stepSizeKHz)
                .toInt().coerceAtLeast(minFrequencyKHz)
            val endFreq = (currentFreq + (centerX / stepWidthPx) * stepSizeKHz)
                .toInt().coerceAtMost(maxFrequencyKHz)

            // Draw ticks
            val firstTickFreq = (startFreq / stepSizeKHz) * stepSizeKHz
            for (freq in firstTickFreq..endFreq step stepSizeKHz) {
                val offsetSteps = (freq - currentFreq) / stepSizeKHz.toFloat()
                val tickX = centerX + offsetSteps * stepWidthPx

                if (tickX < 0 || tickX > size.width) continue

                val isMajorTick = freq % 1000 == 0 // Every 1 MHz
                val isHalfTick = freq % 500 == 0 && !isMajorTick // Every 500 kHz

                val tickHeight = when {
                    isMajorTick -> rulerHeight * 0.40f
                    isHalfTick -> rulerHeight * 0.25f
                    else -> rulerHeight * 0.15f
                }
                val tickAlpha = if (enabled) 0.85f else 0.3f
                val tickColor = if (isMajorTick) primaryColor.copy(alpha = tickAlpha) else onSurfaceColor.copy(alpha = tickAlpha * 0.6f)
                val strokeWidth = if (isMajorTick) 3f else 1.5f

                drawLine(
                    color = tickColor,
                    start = Offset(tickX, rulerHeight - 12.dp.toPx() - tickHeight),
                    end = Offset(tickX, rulerHeight - 12.dp.toPx()),
                    strokeWidth = strokeWidth
                )

                // Draw text labels for major MHz ticks
                if (isMajorTick) {
                    val mhz = freq / 1000
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = onSurfaceColor.toArgb()
                            textSize = 12.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            alpha = if (enabled) 220 else 80
                        }
                        drawText(
                            "$mhz",
                            tickX,
                            rulerHeight - 16.dp.toPx() - tickHeight,
                            paint
                        )
                    }
                }
            }

            // Draw center cursor / pointer
            drawCenterIndicator(centerX, rulerHeight, if (enabled) RadioDialIndicator else Color.Gray)
        }
    }
}

private fun DrawScope.drawCenterIndicator(centerX: Float, height: Float, color: Color) {
    // Top triangle pointer
    val triangleSize = 8.dp.toPx()
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(centerX - triangleSize, 0f)
        lineTo(centerX + triangleSize, 0f)
        lineTo(centerX, triangleSize * 1.5f)
        close()
    }
    drawPath(path, color)

    // Center vertical needle line
    drawLine(
        color = color,
        start = Offset(centerX, triangleSize),
        end = Offset(centerX, height - 8.dp.toPx()),
        strokeWidth = 3.dp.toPx(),
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}

private fun snapToStep(value: Int, min: Int, max: Int, step: Int): Int {
    val stepsFromMin = ((value - min + (step / 2.0)) / step).toInt()
    return (min + stepsFromMin * step).coerceIn(min, max)
}
