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

package com.caf.fmradio.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caf.fmradio.FMStats
import com.caf.fmradio.FmSharedPreferences
import com.caf.fmradio.R
import com.caf.fmradio.viewmodel.FmUiState

@Composable
fun SettingsDialog(
    uiState: FmUiState,
    onSetRegionalBand: (Int) -> Unit,
    onSetUserDefinedBand: (minKHz: Int, maxKHz: Int, spacingKHz: Int) -> Unit,
    onSetAudioOutputMode: (Boolean) -> Unit,
    onSetAutoAF: (Boolean) -> Unit,
    onSetShowSignalIndicator: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bandEntries = remember {
        try {
            context.resources.getStringArray(R.array.regional_band_entries)
        } catch (e: Exception) {
            emptyArray()
        }
    }
    val bandValues = remember {
        try {
            context.resources.getStringArray(R.array.regional_band_values)
        } catch (e: Exception) {
            emptyArray()
        }
    }

    var showBandDropdown by remember { mutableStateOf(false) }
    var showUserDefinedDialog by remember { mutableStateOf(false) }
    var showSwitchToUserDefinedPrompt by remember { mutableStateOf(false) }

    val isUserDefined = uiState.regionalBandIndex == FmSharedPreferences.REGIONAL_BAND_USER_DEFINED

    val currentBandName = remember(uiState.regionalBandIndex, bandEntries, bandValues) {
        val index = bandValues.indexOf(uiState.regionalBandIndex.toString())
        if (index in bandEntries.indices) {
            bandEntries[index]
        } else if (isUserDefined) {
            "User Defined"
        } else {
            "Region ${uiState.regionalBandIndex}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "FM Radio Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Regional Band (Clickable with DropdownMenu)
                Box(modifier = Modifier.fillMaxWidth()) {
                    SettingClickableRow(
                        title = "Regional Band",
                        value = currentBandName,
                        subtitle = if (isUserDefined) "Custom frequency & step size" else "Standard broadcast region",
                        onClick = { showBandDropdown = true }
                    )

                    Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                        DropdownMenu(
                            expanded = showBandDropdown,
                            onDismissRequest = { showBandDropdown = false },
                            modifier = Modifier.heightIn(max = 350.dp)
                        ) {
                            bandEntries.forEachIndexed { index, name ->
                                val value = bandValues.getOrNull(index)?.toIntOrNull() ?: index
                                val selected = value == uiState.regionalBandIndex
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = if (selected) {
                                        {
                                            Text(
                                                text = "✓",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        showBandDropdown = false
                                        onSetRegionalBand(value)
                                        if (value == FmSharedPreferences.REGIONAL_BAND_USER_DEFINED) {
                                            showUserDefinedDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 2. Band Range (Clickable)
                SettingClickableRow(
                    title = "Band Range",
                    value = "${uiState.minFrequencyKHz / 1000.0} - ${uiState.maxFrequencyKHz / 1000.0} MHz",
                    subtitle = if (isUserDefined) "Tap to customize limits" else "Locked by region (tap to customize)",
                    onClick = {
                        if (isUserDefined) {
                            showUserDefinedDialog = true
                        } else {
                            showSwitchToUserDefinedPrompt = true
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 3. Channel Spacing (Clickable)
                SettingClickableRow(
                    title = "Channel Spacing",
                    value = "${uiState.stepSizeKHz} kHz",
                    subtitle = if (isUserDefined) "Tap to customize step" else "Locked by region (tap to customize)",
                    onClick = {
                        if (isUserDefined) {
                            showUserDefinedDialog = true
                        } else {
                            showSwitchToUserDefinedPrompt = true
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 4. Audio Output Mode (Stereo Toggle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stereo Audio",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.isStereo) "Stereo output enabled" else "Mono output forced",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isStereo,
                        onCheckedChange = { onSetAudioOutputMode(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 5. Auto AF (Alternative Frequency)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Alternative Frequency (AF)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatically switch to alternate frequency on weak RDS signals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isAutoAfEnabled,
                        onCheckedChange = { onSetAutoAF(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 6. Signal Strength Indicator Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Signal Strength Indicator",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Show real-time signal bars by periodic polling",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.showSignalIndicator,
                        onCheckedChange = { onSetShowSignalIndicator(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 7. Signal Diagnostics Button (FMStats)
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(context, FMStats::class.java)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Signal Diagnostics (FMStats)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )


    // Sub-dialog 2: Prompt to switch to User Defined
    if (showSwitchToUserDefinedPrompt) {
        AlertDialog(
            onDismissRequest = { showSwitchToUserDefinedPrompt = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Customize Band & Spacing") },
            text = {
                Text(
                    "Standard regional bands lock frequency range and channel spacing according to international regulations.\n\nWould you like to switch to 'User Defined' band to freely customize the range and spacing?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSwitchToUserDefinedPrompt = false
                    showUserDefinedDialog = true
                }) {
                    Text("Customize")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchToUserDefinedPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sub-dialog 3: User Defined Configuration Dialog
    if (showUserDefinedDialog) {
        UserDefinedConfigDialog(
            currentMinKHz = uiState.minFrequencyKHz,
            currentMaxKHz = uiState.maxFrequencyKHz,
            currentStepKHz = uiState.stepSizeKHz,
            onConfirm = { minKHz, maxKHz, spacingKHz ->
                onSetUserDefinedBand(minKHz, maxKHz, spacingKHz)
                showUserDefinedDialog = false
            },
            onDismiss = { showUserDefinedDialog = false }
        )
    }
}

@Composable
private fun SettingClickableRow(
    title: String,
    value: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun UserDefinedConfigDialog(
    currentMinKHz: Int,
    currentMaxKHz: Int,
    currentStepKHz: Int,
    onConfirm: (minKHz: Int, maxKHz: Int, spacingKHz: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var minText by remember(currentMinKHz) { mutableStateOf(String.format(java.util.Locale.US, "%.1f", currentMinKHz / 1000.0)) }
    var maxText by remember(currentMaxKHz) { mutableStateOf(String.format(java.util.Locale.US, "%.1f", currentMaxKHz / 1000.0)) }
    var selectedSpacing by remember(currentStepKHz) { mutableIntStateOf(currentStepKHz) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("User Defined Band") },
        text = {
            Column {
                Text(
                    text = "Configure custom FM range (76.0 - 108.0 MHz) and channel spacing:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it; errorMessage = null },
                    label = { Text("Lower Limit (MHz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = maxText,
                    onValueChange = { maxText = it; errorMessage = null },
                    label = { Text("Upper Limit (MHz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Channel Spacing:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(50, 100, 200).forEach { spacing ->
                        FilterChip(
                            selected = selectedSpacing == spacing,
                            onClick = { selectedSpacing = spacing },
                            label = { Text("$spacing kHz") }
                        )
                    }
                }

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minMHz = minText.toDoubleOrNull()
                val maxMHz = maxText.toDoubleOrNull()
                if (minMHz == null || maxMHz == null) {
                    errorMessage = "Please enter valid numeric frequencies"
                    return@TextButton
                }
                val minKHz = (minMHz * 1000).toInt()
                val maxKHz = (maxMHz * 1000).toInt()
                if (minKHz < 76000 || maxKHz > 108000) {
                    errorMessage = "Frequencies must be between 76.0 and 108.0 MHz"
                    return@TextButton
                }
                if (minKHz >= maxKHz) {
                    errorMessage = "Lower limit must be less than upper limit"
                    return@TextButton
                }
                if ((maxKHz - minKHz) < selectedSpacing) {
                    errorMessage = "Frequency band is too narrow for selected spacing"
                    return@TextButton
                }
                onConfirm(minKHz, maxKHz, selectedSpacing)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
