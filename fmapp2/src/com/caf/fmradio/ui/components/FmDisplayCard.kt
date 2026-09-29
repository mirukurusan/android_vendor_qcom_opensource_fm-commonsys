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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caf.fmradio.R
import com.caf.fmradio.ui.theme.RadioRecordingRed
import com.caf.fmradio.ui.theme.RadioSignalActive
import com.caf.fmradio.viewmodel.FmUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FmDisplayCard(
    uiState: FmUiState,
    onFrequencyClick: () -> Unit,
    previewFrequencyKHz: Int? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
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
                    if (uiState.showSignalIndicator) {
                        SignalStrengthIndicator(
                            bars = if (uiState.isPoweredOn) uiState.signalStrength else 0
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    StatusBadge(
                        text = if (uiState.isStereo) "STEREO" else "MONO",
                        active = uiState.isPoweredOn
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(
                        text = "RDS",
                        active = uiState.isPoweredOn && uiState.rdsSupported
                    )
                }

                // Recording or Sleep Timer Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.isRecording) {
                        RecordingBadge(duration = uiState.formattedRecordDuration)
                    }
                    uiState.formattedSleepCountdown?.let { countdown ->
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(
                            text = countdown,
                            active = true,
                            badgeColor = MaterialTheme.colorScheme.tertiary,
                            iconRes = R.drawable.ic_timer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val isTuning = previewFrequencyKHz != null && previewFrequencyKHz != uiState.currentFrequencyKHz
            val displayedFreqKHz = previewFrequencyKHz ?: uiState.currentFrequencyKHz
            val frequencyText = if (displayedFreqKHz % 100 != 0) {
                String.format(java.util.Locale.US, "%.2f", displayedFreqKHz / 1000.0)
            } else {
                String.format(java.util.Locale.US, "%.1f", displayedFreqKHz / 1000.0)
            }

            // Main Frequency Big Display
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(enabled = uiState.isPoweredOn, onClick = onFrequencyClick)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = frequencyText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = when {
                        !uiState.isPoweredOn -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        isTuning -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
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

            // Station Name (RDS Program Service / Favorite / Tuning status)
            val matchingFavorite = if (isTuning) {
                uiState.favorites.find { it.frequencyKHz == displayedFreqKHz }
            } else null

            val stationTitle: String? = when {
                !uiState.isPoweredOn -> null
                isTuning -> matchingFavorite?.name ?: "Tuning..."
                uiState.isScanning -> "Scanning frequencies..."
                uiState.isSeeking -> "Seeking station..."
                matchingFavorite != null -> matchingFavorite.name
                uiState.stationName.isNotBlank() -> uiState.stationName
                else -> null
            }

            val subtitleText: String? = when {
                !uiState.isPoweredOn -> "Radio is OFF"
                isTuning -> "Release to tune frequency"
                uiState.radioText.isNotBlank() -> uiState.radioText
                else -> null
            }

            if (stationTitle != null || subtitleText != null) {
                Spacer(modifier = Modifier.height(4.dp))
            }

            stationTitle?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            if (stationTitle != null && subtitleText != null) {
                Spacer(modifier = Modifier.height(2.dp))
            }

            subtitleText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE)
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    active: Boolean,
    badgeColor: Color? = null,
    iconRes: Int? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        !active -> MaterialTheme.colorScheme.surfaceContainer
        badgeColor != null -> badgeColor.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        !active -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        badgeColor != null -> badgeColor
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
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
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .alpha(alpha)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REC $duration",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer
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
