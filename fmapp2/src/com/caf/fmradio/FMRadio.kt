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

package com.caf.fmradio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caf.fmradio.ui.screens.FmMainScreen
import com.caf.fmradio.ui.theme.FmTheme
import com.caf.fmradio.viewmodel.FmRadioViewModel

/**
 * Modern Jetpack Compose entry Activity for FM Radio.
 */
class FMRadio : ComponentActivity() {

    companion object {
        const val LOGTAG: String = "FMRadio"
        const val RECORDING_ENABLE: Boolean = true
        const val SCAN_STATION_PREFS_NAME: String = "scan_station_list"
        const val NUM_OF_STATIONS: String = "number_of_stations"
        const val STATION_NAME: String = "name_of_station"
        const val STATION_FREQUENCY: String = "frequency_of_station"
        private const val PERMISSION_REQUEST_CODE: Int = 101
    }

    private val viewModel: FmRadioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC

        checkAndRequestPermissions()

        setContent {
            FmTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                FmMainScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            requestPermissions(needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }
}
