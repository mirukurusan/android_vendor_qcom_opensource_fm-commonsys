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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caf.fmradio.R
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
    onCancelScan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val enabled = uiState.isPoweredOn

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Secondary Utility Actions Row (Speaker, Mute, Record, Scan)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speaker / Headset
            val speakerTooltip = stringResource(if (uiState.isSpeakerOn) R.string.tooltip_speaker else R.string.tooltip_headset)
            UtilityActionButton(
                iconRes = if (uiState.isSpeakerOn) R.drawable.ic_volume_up else R.drawable.ic_headset,
                contentDescription = speakerTooltip,
                tooltip = speakerTooltip,
                active = uiState.isSpeakerOn,
                enabled = enabled,
                onClick = onToggleSpeaker
            )

            // Mute / Unmute
            val muteTooltip = stringResource(if (uiState.isMuted) R.string.tooltip_unmute else R.string.tooltip_mute)
            UtilityActionButton(
                iconRes = if (uiState.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up,
                contentDescription = muteTooltip,
                tooltip = muteTooltip,
                active = uiState.isMuted,
                activeContainerColor = MaterialTheme.colorScheme.errorContainer,
                activeContentColor = MaterialTheme.colorScheme.onErrorContainer,
                enabled = enabled,
                onClick = onToggleMute
            )

            // Recording
            val recordTooltip = stringResource(if (uiState.isRecording) R.string.tooltip_record_stop else R.string.tooltip_record_start)
            UtilityActionButton(
                iconRes = if (uiState.isRecording) R.drawable.ic_stop else R.drawable.ic_fiber_manual_record,
                contentDescription = recordTooltip,
                tooltip = recordTooltip,
                active = uiState.isRecording,
                activeContainerColor = MaterialTheme.colorScheme.errorContainer,
                activeContentColor = MaterialTheme.colorScheme.error,
                enabled = enabled,
                onClick = onToggleRecording
            )

            // Scan / Auto search
            val scanTooltip = stringResource(if (uiState.isScanning) R.string.tooltip_scan_stop else R.string.tooltip_scan_start)
            UtilityActionButton(
                iconRes = if (uiState.isScanning) R.drawable.ic_search_off else R.drawable.ic_search,
                contentDescription = scanTooltip,
                tooltip = scanTooltip,
                active = uiState.isScanning,
                activeContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                activeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                enabled = enabled,
                onClick = if (uiState.isScanning) onCancelScan else onStartScan
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Tuning and Playback Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seek Down (<<)
            AppTooltip(tooltip = stringResource(R.string.tooltip_seek_down)) {
                OutlinedIconButton(
                    onClick = { onSeek(false) },
                    enabled = enabled && !uiState.isSeeking && !uiState.isScanning,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fast_rewind),
                        contentDescription = stringResource(R.string.tooltip_seek_down),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Step Down (-)
            AppTooltip(tooltip = stringResource(R.string.tooltip_step_down)) {
                OutlinedIconButton(
                    onClick = { onStep(false) },
                    enabled = enabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = stringResource(R.string.tooltip_step_down),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Central Power / Play Button
            val powerTooltip = stringResource(if (uiState.isPoweredOn) R.string.tooltip_power_off else R.string.tooltip_power_on)
            AppTooltip(tooltip = powerTooltip) {
                FilledIconButton(
                    onClick = onTogglePower,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (uiState.isPoweredOn) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                ) {
                    val tint = if (uiState.isPoweredOn) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_power_settings_new),
                            contentDescription = powerTooltip,
                            modifier = Modifier.size(28.dp),
                            tint = tint
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isPoweredOn) "ON" else "OFF",
                            color = tint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Step Up (+)
            AppTooltip(tooltip = stringResource(R.string.tooltip_step_up)) {
                OutlinedIconButton(
                    onClick = { onStep(true) },
                    enabled = enabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = stringResource(R.string.tooltip_step_up),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Seek Up (>>)
            AppTooltip(tooltip = stringResource(R.string.tooltip_seek_up)) {
                OutlinedIconButton(
                    onClick = { onSeek(true) },
                    enabled = enabled && !uiState.isSeeking && !uiState.isScanning,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fast_forward),
                        contentDescription = stringResource(R.string.tooltip_seek_up),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UtilityActionButton(
    iconRes: Int,
    contentDescription: String,
    tooltip: String = contentDescription,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    activeContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.38f)
        active -> activeContainerColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        active -> activeContentColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AppTooltip(tooltip = tooltip) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(44.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.38f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
