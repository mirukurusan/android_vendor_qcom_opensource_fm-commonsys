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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caf.fmradio.R
import com.caf.fmradio.data.FmStation
import com.caf.fmradio.ui.components.FmControlBar
import com.caf.fmradio.ui.components.FmDisplayCard
import com.caf.fmradio.ui.components.FmFrequencyRuler
import com.caf.fmradio.ui.components.FmPresetRow
import com.caf.fmradio.viewmodel.FmRadioViewModel
import com.caf.fmradio.viewmodel.FmUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FmMainScreen(
    viewModel: FmRadioViewModel,
    uiState: FmUiState
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showTuneDialog by remember { mutableStateOf(false) }
    var showStationSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedPresetForOption by remember { mutableStateOf<FmStation?>(null) }

    // Show user messages via Snackbar
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FM Radio",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Station List / Search Action
                    IconButton(onClick = { showStationSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_radio),
                            contentDescription = "Stations"
                        )
                    }

                    // Sleep Timer Action
                    IconButton(onClick = { showSleepDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bedtime),
                            contentDescription = "Sleep Timer"
                        )
                    }

                    // Settings Action
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                // Missing Headset Warning Banner
                if (!uiState.isAntennaAvailable) {
                    NoAntennaBanner(modifier = Modifier.padding(bottom = 12.dp))
                }

                // Station Info and Digital Display Card
                FmDisplayCard(
                    uiState = uiState,
                    onFrequencyClick = {
                        if (uiState.isPoweredOn) {
                            showTuneDialog = true
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Frequency Ruler Dial
                FmFrequencyRuler(
                    currentFrequencyKHz = uiState.currentFrequencyKHz,
                    minFrequencyKHz = uiState.minFrequencyKHz,
                    maxFrequencyKHz = uiState.maxFrequencyKHz,
                    stepSizeKHz = uiState.stepSizeKHz,
                    enabled = uiState.isPoweredOn,
                    onFrequencyChanged = { freq ->
                        viewModel.tune(freq)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Favorite Presets Bar
                FmPresetRow(
                    uiState = uiState,
                    onTune = { freq -> viewModel.tune(freq) },
                    onToggleCurrentPreset = { viewModel.togglePreset() },
                    onPresetLongClick = { station -> selectedPresetForOption = station }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback & Tuning Control Bar
            FmControlBar(
                uiState = uiState,
                onTogglePower = { viewModel.togglePower() },
                onStep = { forward -> viewModel.step(forward) },
                onSeek = { forward -> viewModel.seek(forward) },
                onToggleMute = { viewModel.toggleMute() },
                onToggleSpeaker = { viewModel.toggleSpeaker() },
                onToggleRecording = { viewModel.toggleRecording() },
                onStartScan = { viewModel.startScan() },
                onCancelScan = { viewModel.cancelScan() },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    // Direct Frequency Input Dialog
    if (showTuneDialog) {
        var inputFreqText by remember {
            mutableStateOf(uiState.frequencyMHzString)
        }
        AlertDialog(
            onDismissRequest = { showTuneDialog = false },
            title = { Text("Tune Frequency") },
            text = {
                Column {
                    Text(
                        "Enter frequency between ${uiState.minFrequencyKHz / 1000.0} and ${uiState.maxFrequencyKHz / 1000.0} MHz:"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputFreqText,
                        onValueChange = { inputFreqText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        label = { Text("Frequency (MHz)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mhz = inputFreqText.toDoubleOrNull()
                        if (mhz != null) {
                            val khz = (mhz * 1000).toInt()
                            viewModel.tune(khz)
                        }
                        showTuneDialog = false
                    }
                ) {
                    Text("Tune")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTuneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Preset Option Dialog (Delete preset)
    selectedPresetForOption?.let { station ->
        AlertDialog(
            onDismissRequest = { selectedPresetForOption = null },
            title = { Text(station.displayName) },
            text = { Text("Do you want to remove this station from presets?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.togglePreset(station.frequencyKHz)
                        selectedPresetForOption = null
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPresetForOption = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Station List Bottom Sheet
    if (showStationSheet) {
        StationListSheet(
            uiState = uiState,
            onTune = { freq -> viewModel.tune(freq) },
            onTogglePreset = { freq -> viewModel.togglePreset(freq) },
            onStartScan = { viewModel.startScan() },
            onCancelScan = { viewModel.cancelScan() },
            onDismiss = { showStationSheet = false }
        )
    }

    // Sleep Timer Dialog
    if (showSleepDialog) {
        SleepTimerDialog(
            currentRemainingSec = uiState.sleepTimerRemainingSeconds,
            onSetTimer = { minutes -> viewModel.setSleepTimer(minutes) },
            onCancelTimer = { viewModel.cancelSleepTimer() },
            onDismiss = { showSleepDialog = false }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            uiState = uiState,
            onSetRegionalBand = { bandIndex -> viewModel.setRegionalBand(bandIndex) },
            onSetUserDefinedBand = { minKHz, maxKHz, stepKHz ->
                viewModel.setUserDefinedBand(minKHz, maxKHz, stepKHz)
            },
            onSetAudioOutputMode = { isStereo -> viewModel.setAudioOutputMode(isStereo) },
            onSetAutoAF = { enabled -> viewModel.setAutoAF(enabled) },
            onSetShowSignalIndicator = { enabled -> viewModel.setShowSignalIndicator(enabled) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

