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

import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caf.fmradio.ui.theme.RadioRecordingRed
import com.caf.fmradio.viewmodel.FmUiState

@Composable
fun FmControlBar(
    uiState: FmUiState,
    onTogglePower: () -> Unit,
    onStep: (Boolean) -> Unit,
    onSeek: (Boolean) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleRecording: () -> Unit,
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = uiState.isPoweredOn

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Tuning and Playback Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seek Down (<<)
            OutlinedIconButton(
                onClick = { onSeek(false) },
                enabled = enabled && !uiState.isSeeking && !uiState.isScanning,
                modifier = Modifier.size(52.dp)
            ) {
                Text("<<", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Step Down (-)
            OutlinedIconButton(
                onClick = { onStep(false) },
                enabled = enabled,
                modifier = Modifier.size(46.dp)
            ) {
                Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            // Central Power / Play Button
            FilledIconButton(
                onClick = onTogglePower,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (uiState.isPoweredOn) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(
                    text = if (uiState.isPoweredOn) "ON" else "OFF",
                    color = if (uiState.isPoweredOn) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Step Up (+)
            OutlinedIconButton(
                onClick = { onStep(true) },
                enabled = enabled,
                modifier = Modifier.size(46.dp)
            ) {
                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // Seek Up (>>)
            OutlinedIconButton(
                onClick = { onSeek(true) },
                enabled = enabled && !uiState.isSeeking && !uiState.isScanning,
                modifier = Modifier.size(52.dp)
            ) {
                Text(">>", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Secondary Utility Bar (Speaker, Mute, Record, Scan)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speaker / Headset
                UtilityButton(
                    label = if (uiState.isSpeakerOn) "Speaker" else "Headset",
                    active = uiState.isSpeakerOn,
                    enabled = enabled,
                    onClick = onToggleSpeaker
                )

                // Mute
                UtilityButton(
                    label = if (uiState.isMuted) "Unmute" else "Mute",
                    active = uiState.isMuted,
                    enabled = enabled,
                    onClick = onToggleMute
                )

                // Recording
                UtilityButton(
                    label = if (uiState.isRecording) "Stop Rec" else "Record",
                    active = uiState.isRecording,
                    activeColor = RadioRecordingRed,
                    enabled = enabled,
                    onClick = onToggleRecording
                )

                // Scan / Auto search
                UtilityButton(
                    label = if (uiState.isScanning) "Scanning" else "Scan",
                    active = uiState.isScanning,
                    enabled = enabled && !uiState.isScanning,
                    onClick = onStartScan
                )
            }
        }
    }
}

@Composable
private fun UtilityButton(
    label: String,
    active: Boolean,
    enabled: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !enabled -> Color.Transparent
        active -> activeColor.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        active -> activeColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor
        )
    }
}
