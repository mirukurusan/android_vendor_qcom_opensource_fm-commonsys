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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caf.fmradio.FMStats
import com.caf.fmradio.viewmodel.FmUiState

@Composable
fun SettingsDialog(
    uiState: FmUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "FM Radio Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Band Info
                SettingInfoRow(
                    title = "Band Range",
                    value = "${uiState.minFrequencyKHz / 1000.0} - ${uiState.maxFrequencyKHz / 1000.0} MHz"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Channel Spacing
                SettingInfoRow(
                    title = "Channel Spacing",
                    value = "${uiState.stepSizeKHz} kHz"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Audio Mode
                SettingInfoRow(
                    title = "Audio Output Mode",
                    value = if (uiState.isStereo) "Stereo" else "Mono"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Speaker / Headset
                SettingInfoRow(
                    title = "Current Audio Path",
                    value = if (uiState.isSpeakerOn) "Speaker (Loudspeaker)" else "Wired Headset"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Engineering Diagnostics button
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
                Text("OK")
            }
        }
    )
}

@Composable
private fun SettingInfoRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
