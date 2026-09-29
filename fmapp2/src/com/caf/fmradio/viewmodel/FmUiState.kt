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

package com.caf.fmradio.viewmodel

import androidx.compose.runtime.Immutable
import com.caf.fmradio.data.FmStation
import java.util.Locale

/**
 * Immutable UI State representing the complete presentation state of FM Radio.
 */
@Immutable
data class FmUiState(
    val isServiceConnected: Boolean = false,
    val isPoweredOn: Boolean = false,
    val isAntennaAvailable: Boolean = true,
    val currentFrequencyKHz: Int = 87500,
    val minFrequencyKHz: Int = 87500,
    val maxFrequencyKHz: Int = 108000,
    val stepSizeKHz: Int = 100,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isStereo: Boolean = true,
    val isScanning: Boolean = false,
    val isSeeking: Boolean = false,
    val isRecording: Boolean = false,
    val recordDurationSeconds: Long = 0L,
    val stationName: String = "",
    val radioText: String = "",
    val rdsSupported: Boolean = false,
    val signalStrength: Int = 0, // 0 to 4 levels
    val showSignalIndicator: Boolean = false,
    val presets: List<FmStation> = emptyList(),
    val scannedStations: List<FmStation> = emptyList(),
    val sleepTimerRemainingSeconds: Long? = null,
    val regionalBandIndex: Int = 8,
    val isAutoAfEnabled: Boolean = true,
    val userMessage: String? = null
) {
    /**
     * Display string for current frequency (e.g. "103.7")
     */
    val frequencyMHzString: String
        get() = String.format(Locale.US, "%.1f", currentFrequencyKHz / 1000.0)

    /**
     * Formatted string for recording duration (e.g. "01:23")
     */
    val formattedRecordDuration: String
        get() {
            val minutes = recordDurationSeconds / 60
            val seconds = recordDurationSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    /**
     * Formatted string for sleep timer countdown (e.g. "15:00")
     */
    val formattedSleepCountdown: String?
        get() = sleepTimerRemainingSeconds?.let { totalSec ->
            val minutes = totalSec / 60
            val seconds = totalSec % 60
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    /**
     * Returns true if the current frequency is already in the preset list
     */
    val isCurrentPreset: Boolean
        get() = presets.any { it.frequencyKHz == currentFrequencyKHz }
}
