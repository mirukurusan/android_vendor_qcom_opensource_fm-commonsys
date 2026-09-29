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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caf.fmradio.ui.theme.RadioRecordingRed
import com.caf.fmradio.ui.theme.RadioSignalActive
import com.caf.fmradio.viewmodel.FmUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FmDisplayCard(
    uiState: FmUiState,
    onFrequencyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Status Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal & Audio Mode Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalStrengthIndicator(
                        bars = if (uiState.isPoweredOn) uiState.signalStrength else 0
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(
                        text = if (uiState.isStereo) "STEREO" else "MONO",
                        active = uiState.isPoweredOn
                    )
                    if (uiState.rdsSupported) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(text = "RDS", active = true)
                    }
                }

                // Recording or Sleep Timer Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.isRecording) {
                        RecordingBadge(duration = uiState.formattedRecordDuration)
                    }
                    uiState.formattedSleepCountdown?.let { countdown ->
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(
                            text = "Sleep: $countdown",
                            active = true,
                            badgeColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Frequency Big Display
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = uiState.isPoweredOn, onClick = onFrequencyClick)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = uiState.frequencyMHzString,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    ),
                    color = if (uiState.isPoweredOn) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MHz",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Station Name (RDS Program Service)
            val stationTitle = when {
                !uiState.isPoweredOn -> "Radio is OFF"
                uiState.isScanning -> "Scanning frequencies..."
                uiState.isSeeking -> "Seeking station..."
                uiState.stationName.isNotBlank() -> uiState.stationName
                else -> "FM Radio"
            }

            Text(
                text = stationTitle,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            // RadioText (RDS RT) with Marquee ticker
            val radioText = if (uiState.isPoweredOn && uiState.radioText.isNotBlank()) {
                uiState.radioText
            } else {
                if (uiState.isPoweredOn) "Ready" else "Tap power button to turn on"
            }

            Text(
                text = radioText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    active: Boolean,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (active) badgeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (active) badgeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RecordingBadge(duration: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recPulse"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = RadioRecordingRed.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RadioRecordingRed)
                    .alpha(alpha)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REC $duration",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RadioRecordingRed
            )
        }
    }
}

@Composable
private fun SignalStrengthIndicator(bars: Int) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..4) {
            val height = (4 + i * 3).dp
            val active = i <= bars
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (active) RadioSignalActive else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
