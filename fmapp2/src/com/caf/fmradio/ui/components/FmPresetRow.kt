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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caf.fmradio.R
import com.caf.fmradio.data.FmStation
import com.caf.fmradio.viewmodel.FmUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FmPresetRow(
    uiState: FmUiState,
    onTune: (Int) -> Unit,
    onToggleCurrentPreset: () -> Unit,
    onPresetLongClick: (FmStation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            // Bookmark / Save Current Station button
            AssistChip(
                onClick = onToggleCurrentPreset,
                enabled = uiState.isPoweredOn,
                leadingIcon = {
                    Icon(
                        painter = painterResource(
                            if (uiState.isCurrentPreset) R.drawable.ic_star else R.drawable.ic_add
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (uiState.isCurrentPreset) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                label = {
                    Text(
                        if (uiState.isCurrentPreset) "Saved" else "Add Preset",
                        fontSize = 12.sp
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = if (uiState.isCurrentPreset) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            )
        }

        if (uiState.presets.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "No presets saved yet. Tune and tap 'Add Preset'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.presets, key = { it.frequencyKHz }) { station ->
                    val isSelected = station.frequencyKHz == uiState.currentFrequencyKHz

                    FilterChip(
                        selected = isSelected,
                        onClick = { onTune(station.frequencyKHz) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_star),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else null,
                        label = {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = station.frequencyMHzString,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (station.name.isNotBlank()) {
                                    Text(
                                        text = station.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.combinedClickable(
                            onClick = { onTune(station.frequencyKHz) },
                            onLongClick = { onPresetLongClick(station) }
                        )
                    )
                }
            }
        }
    }
}
