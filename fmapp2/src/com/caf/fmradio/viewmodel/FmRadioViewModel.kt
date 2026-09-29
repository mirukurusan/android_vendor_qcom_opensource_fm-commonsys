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

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.caf.fmradio.FMRadio
import com.caf.fmradio.FmSharedPreferences
import com.caf.fmradio.PresetStation
import com.caf.fmradio.data.FmServiceRepository
import com.caf.fmradio.data.FmStation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FmRadioViewModel(application: Application) : AndroidViewModel(application), FmServiceRepository.Listener {

    companion object {
        private const val TAG = "FmRadioViewModel"
    }

    private val repository = FmServiceRepository()
    private val _uiState = MutableStateFlow(FmUiState())
    val uiState: StateFlow<FmUiState> = _uiState.asStateFlow()

    private var recordTimerJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", 0)
                val plugged = state == 1
                Log.d(TAG, "Headset plug changed: plugged=$plugged")
                _uiState.update { it.copy(isAntennaAvailable = plugged) }
                if (!plugged && _uiState.value.isPoweredOn) {
                    _uiState.update { it.copy(userMessage = "Headset unplugged. Antenna disconnected.") }
                }
            }
        }
    }

    init {
        repository.listener = this
        // Register headset broadcast receiver
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        application.registerReceiver(headsetReceiver, filter)

        // Initialize SharedPreferences bounds and presets
        try {
            FmSharedPreferences.load(application)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading FmSharedPreferences", e)
        }
        loadConfigurationAndPresets()

        // Bind to background service
        repository.bind(application)
    }

    fun loadConfigurationAndPresets() {
        val minFreq = try {
            FmSharedPreferences.getLowerLimit().takeIf { it > 0 } ?: 87500
        } catch (e: Exception) {
            87500
        }
        val maxFreq = try {
            FmSharedPreferences.getUpperLimit().takeIf { it > 0 } ?: 108000
        } catch (e: Exception) {
            108000
        }
        val stepSize = try {
            FmSharedPreferences.getFrequencyStepSize().takeIf { it > 0 } ?: 100
        } catch (e: Exception) {
            100
        }
        val tuned = try {
            FmSharedPreferences.getTunedFrequency().takeIf { it in minFreq..maxFreq } ?: minFreq
        } catch (e: Exception) {
            minFreq
        }
        val country = try {
            FmSharedPreferences.getCountry()
        } catch (e: Exception) {
            FmSharedPreferences.REGIONAL_BAND_NORTH_AMERICA
        }
        val autoAf = try {
            FmSharedPreferences.getAutoAFSwitch()
        } catch (e: Exception) {
            true
        }
        val isStereo = try {
            FmSharedPreferences.getAudioOutputMode()
        } catch (e: Exception) {
            true
        }

        val presets = loadPresetsFromPreferences()
        val scanned = loadScannedStations()

        _uiState.update {
            it.copy(
                minFrequencyKHz = minFreq,
                maxFrequencyKHz = maxFreq,
                stepSizeKHz = stepSize,
                currentFrequencyKHz = tuned,
                presets = presets,
                scannedStations = scanned,
                regionalBandIndex = country,
                isAutoAfEnabled = autoAf,
                isStereo = isStereo
            )
        }
    }

    private fun saveScannedStations(stations: List<FmStation>) {
        try {
            val sp: SharedPreferences = getApplication<Application>().getSharedPreferences(FMRadio.SCAN_STATION_PREFS_NAME, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sp.edit()
            editor.clear()
            var index = 0
            for (st in stations) {
                index++
                val name = st.name.ifBlank { "Station $index" }
                editor.putString(FMRadio.STATION_NAME + index, name)
                editor.putInt(FMRadio.STATION_FREQUENCY + index, st.frequencyKHz)
            }
            editor.putInt(FMRadio.NUM_OF_STATIONS, index)
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving scanned stations", e)
        }
    }

    private fun loadScannedStations(): List<FmStation> {
        val list = mutableListOf<FmStation>()
        try {
            val sp: SharedPreferences = getApplication<Application>().getSharedPreferences(FMRadio.SCAN_STATION_PREFS_NAME, Context.MODE_PRIVATE)
            val count = sp.getInt(FMRadio.NUM_OF_STATIONS, 0)
            for (i in 1..count) {
                val name = sp.getString(FMRadio.STATION_NAME + i, "") ?: ""
                val freq = sp.getInt(FMRadio.STATION_FREQUENCY + i, 0)
                if (freq > 0) {
                    list.add(FmStation(frequencyKHz = freq, name = name))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading scanned stations", e)
        }
        return list
    }

    private fun autoPopulatePresetsIfEmpty(stations: List<FmStation>) {
        try {
            val curIndex = FmSharedPreferences.getCurrentListIndex()
            val presetList = FmSharedPreferences.getStationList(curIndex)
            val currentCount = presetList?.stationCount ?: 0
            if (currentCount == 0 && stations.isNotEmpty()) {
                for (st in stations.take(12)) {
                    val name = st.name.ifBlank { st.displayName }
                    FmSharedPreferences.addStation(curIndex, PresetStation(name, st.frequencyKHz))
                }
                FmSharedPreferences.save(getApplication())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-populating presets", e)
        }
    }

    private fun loadPresetsFromPreferences(): List<FmStation> {
        val list = mutableListOf<FmStation>()
        try {
            val curIndex = FmSharedPreferences.getCurrentListIndex()
            val presetList = FmSharedPreferences.getStationList(curIndex)
            if (presetList != null) {
                for (i in 0 until presetList.stationCount) {
                    val st = presetList.getStationFromIndex(i)
                    if (st != null) {
                        list.add(
                            FmStation(
                                frequencyKHz = st.frequency,
                                name = st.name ?: "",
                                isPreset = true,
                                rdsSupported = st.rdsSupported
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading presets", e)
        }
        return list
    }

    // --- User Actions ---

    fun togglePower() {
        val state = _uiState.value
        Log.d(TAG, "togglePower called, current isPoweredOn=${state.isPoweredOn}")
        if (state.isPoweredOn) {
            val success = repository.fmOff()
            Log.d(TAG, "fmOff executed, success=$success")
            _uiState.update {
                it.copy(
                    isPoweredOn = false,
                    isRecording = false,
                    isScanning = false,
                    isSeeking = false
                )
            }
            stopRecordTimer()
        } else {
            if (!state.isAntennaAvailable) {
                _uiState.update { it.copy(userMessage = "Please plug in wired headset as antenna") }
                return
            }
            val success = repository.fmOn()
            Log.d(TAG, "fmOn executed, success=$success")
            if (success) {
                _uiState.update {
                    it.copy(
                        isPoweredOn = true,
                        isMuted = repository.isMuted(),
                        isSpeakerOn = repository.isSpeakerEnabled()
                    )
                }
                tune(_uiState.value.currentFrequencyKHz)
            } else {
                _uiState.update { it.copy(userMessage = "Failed to turn on FM Radio") }
            }
        }
    }

    fun tune(frequencyKHz: Int) {
        val clamped = frequencyKHz.coerceIn(_uiState.value.minFrequencyKHz, _uiState.value.maxFrequencyKHz)
        _uiState.update { it.copy(currentFrequencyKHz = clamped) }
        try {
            FmSharedPreferences.setTunedFrequency(clamped)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving tuned frequency", e)
        }
        repository.tune(clamped)
    }

    fun step(forward: Boolean) {
        val current = _uiState.value.currentFrequencyKHz
        val step = _uiState.value.stepSizeKHz
        val min = _uiState.value.minFrequencyKHz
        val max = _uiState.value.maxFrequencyKHz

        val next = if (forward) {
            if (current + step > max) min else current + step
        } else {
            if (current - step < min) max else current - step
        }
        tune(next)
    }

    fun seek(forward: Boolean) {
        _uiState.update { it.copy(isSeeking = true) }
        repository.seek(forward)
    }

    fun startScan() {
        _uiState.update { it.copy(isScanning = true) }
        repository.scan(0)
    }

    fun cancelScan() {
        repository.cancelSearch()
        _uiState.update { it.copy(isScanning = false, isSeeking = false) }
    }

    fun toggleMute() {
        val currentlyMuted = _uiState.value.isMuted
        Log.d(TAG, "toggleMute called, currentlyMuted=$currentlyMuted")
        if (currentlyMuted) {
            repository.unMute()
            val newMuted = repository.isMuted()
            Log.d(TAG, "unMute executed, newMuted=$newMuted")
            _uiState.update { it.copy(isMuted = newMuted) }
        } else {
            repository.mute()
            val newMuted = repository.isMuted()
            Log.d(TAG, "mute executed, newMuted=$newMuted")
            _uiState.update { it.copy(isMuted = newMuted) }
        }
    }

    fun toggleSpeaker() {
        val next = !_uiState.value.isSpeakerOn
        repository.enableSpeaker(next)
        _uiState.update { it.copy(isSpeakerOn = next) }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            repository.stopRecording()
            _uiState.update { it.copy(isRecording = false) }
            stopRecordTimer()
        } else {
            val started = repository.startRecording()
            if (started) {
                _uiState.update { it.copy(isRecording = true, recordDurationSeconds = 0L) }
                startRecordTimer()
            } else {
                _uiState.update { it.copy(userMessage = "Failed to start recording") }
            }
        }
    }

    fun togglePreset(frequencyKHz: Int = _uiState.value.currentFrequencyKHz, name: String = "") {
        try {
            val curIndex = FmSharedPreferences.getCurrentListIndex()
            val existing = FmSharedPreferences.getStationFromFrequency(frequencyKHz)
            if (existing != null) {
                // Remove preset
                FmSharedPreferences.removeStation(curIndex, existing)
            } else {
                // Add preset
                val stationName = name.ifBlank { _uiState.value.stationName }
                val newStation = PresetStation(stationName, frequencyKHz)
                FmSharedPreferences.addStation(curIndex, newStation)
            }
            FmSharedPreferences.save(getApplication())
            _uiState.update { it.copy(presets = loadPresetsFromPreferences()) }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling preset", e)
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerRemainingSeconds = null) }
            return
        }

        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60L
            while (isActive && remaining > 0) {
                _uiState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
                delay(1000L)
                remaining--
            }
            if (isActive) {
                _uiState.update { it.copy(sleepTimerRemainingSeconds = null) }
                repository.fmOff()
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _uiState.update { it.copy(sleepTimerRemainingSeconds = null) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun setRegionalBand(bandIndex: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "setRegionalBand: $bandIndex")
                FmSharedPreferences.setCountry(bandIndex)
                FmSharedPreferences.save(getApplication())
                if (_uiState.value.isPoweredOn) {
                    repository.fmReconfigure()
                }
                loadConfigurationAndPresets()
                val min = _uiState.value.minFrequencyKHz
                val max = _uiState.value.maxFrequencyKHz
                val current = _uiState.value.currentFrequencyKHz
                if (current !in min..max) {
                    tune(min)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting regional band", e)
            }
        }
    }

    fun setUserDefinedBand(minFreqKHz: Int, maxFreqKHz: Int, spacingKHz: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "setUserDefinedBand: min=$minFreqKHz, max=$maxFreqKHz, spacing=$spacingKHz")
                FmSharedPreferences.setCountry(FmSharedPreferences.REGIONAL_BAND_USER_DEFINED)
                FmSharedPreferences.setLowerLimit(minFreqKHz)
                FmSharedPreferences.setUpperLimit(maxFreqKHz)
                val spacingCode = when (spacingKHz) {
                    50 -> 2
                    200 -> 0
                    else -> 1 // 100 kHz
                }
                FmSharedPreferences.setChSpacing(spacingCode)
                FmSharedPreferences.save(getApplication())
                if (_uiState.value.isPoweredOn) {
                    repository.fmReconfigure()
                }
                loadConfigurationAndPresets()
                val min = _uiState.value.minFrequencyKHz
                val max = _uiState.value.maxFrequencyKHz
                val current = _uiState.value.currentFrequencyKHz
                if (current !in min..max) {
                    tune(min)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting user defined band", e)
            }
        }
    }

    fun setAudioOutputMode(isStereo: Boolean) {
        viewModelScope.launch {
            try {
                FmSharedPreferences.setAudioOutputMode(isStereo)
                FmSharedPreferences.save(getApplication())
                repository.enableStereo(isStereo)
                _uiState.update { it.copy(isStereo = isStereo) }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting audio output mode", e)
            }
        }
    }

    fun setAutoAF(enabled: Boolean) {
        viewModelScope.launch {
            try {
                FmSharedPreferences.setAutoAFSwitch(enabled)
                FmSharedPreferences.save(getApplication())
                repository.enableAutoAF(enabled)
                _uiState.update { it.copy(isAutoAfEnabled = enabled) }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting auto AF", e)
            }
        }
    }

    // --- Service Listener Implementation ---

    override fun onServiceConnected() {
        viewModelScope.launch {
            loadConfigurationAndPresets()
            val isAntenna = repository.isAntennaAvailable()
            val isFmOn = repository.isFmOn()
            val tuned = try {
                FmSharedPreferences.getTunedFrequency().takeIf { it > 0 } ?: _uiState.value.currentFrequencyKHz
            } catch (e: Exception) {
                _uiState.value.currentFrequencyKHz
            }
            Log.d(TAG, "onServiceConnected: isFmOn=$isFmOn, isAntenna=$isAntenna, tuned=$tuned")

            if (!isFmOn && isAntenna) {
                // Auto start FM on launch when antenna is present (matching legacy FMRadio behavior)
                val started = repository.fmOn()
                Log.d(TAG, "Auto fmOn result: $started")
                if (started) {
                    _uiState.update {
                        it.copy(
                            isServiceConnected = true,
                            isPoweredOn = true,
                            isAntennaAvailable = true,
                            currentFrequencyKHz = tuned,
                            isMuted = repository.isMuted(),
                            isSpeakerOn = repository.isSpeakerEnabled(),
                            isRecording = false
                        )
                    }
                    repository.tune(tuned)
                } else {
                    _uiState.update {
                        it.copy(
                            isServiceConnected = true,
                            isPoweredOn = false,
                            isAntennaAvailable = true,
                            currentFrequencyKHz = tuned
                        )
                    }
                }
            } else if (isFmOn) {
                _uiState.update {
                    it.copy(
                        isServiceConnected = true,
                        isPoweredOn = true,
                        isAntennaAvailable = isAntenna,
                        currentFrequencyKHz = tuned,
                        isMuted = repository.isMuted(),
                        isSpeakerOn = repository.isSpeakerEnabled(),
                        isRecording = repository.isRecording()
                    )
                }
                repository.tune(tuned)
            } else {
                _uiState.update {
                    it.copy(
                        isServiceConnected = true,
                        isPoweredOn = false,
                        isAntennaAvailable = isAntenna,
                        currentFrequencyKHz = tuned
                    )
                }
            }
        }
    }

    override fun onServiceDisconnected() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isServiceConnected = false,
                    isPoweredOn = false,
                    isRecording = false
                )
            }
        }
    }

    override fun onEnabled() {
        viewModelScope.launch {
            val tuned = try {
                FmSharedPreferences.getTunedFrequency().takeIf { it > 0 } ?: _uiState.value.currentFrequencyKHz
            } catch (e: Exception) {
                _uiState.value.currentFrequencyKHz
            }
            Log.d(TAG, "onEnabled callback received, tuning to $tuned")
            _uiState.update {
                it.copy(
                    isPoweredOn = true,
                    currentFrequencyKHz = tuned,
                    isMuted = repository.isMuted(),
                    isSpeakerOn = repository.isSpeakerEnabled()
                )
            }
            repository.tune(tuned)
        }
    }

    override fun onDisabled() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPoweredOn = false,
                    isRecording = false,
                    isScanning = false,
                    isSeeking = false
                )
            }
            stopRecordTimer()
        }
    }

    override fun onRadioReset() {
        onDisabled()
    }

    override fun onTuneStatusChanged() {
        viewModelScope.launch {
            val tuned = try {
                FmSharedPreferences.getTunedFrequency()
            } catch (e: Exception) {
                _uiState.value.currentFrequencyKHz
            }
            val ps = repository.getProgramService()
            val rt = repository.getRadioText()

            _uiState.update {
                it.copy(
                    currentFrequencyKHz = tuned,
                    stationName = ps,
                    radioText = rt,
                    isSeeking = false
                )
            }
        }
    }

    override fun onProgramServiceChanged() {
        viewModelScope.launch {
            val ps = repository.getProgramService()
            _uiState.update { it.copy(stationName = ps) }
        }
    }

    override fun onRadioTextChanged() {
        viewModelScope.launch {
            val rt = repository.getRadioText()
            _uiState.update { it.copy(radioText = rt) }
        }
    }

    override fun onSignalStrengthChanged() {
        viewModelScope.launch {
            val rssi = repository.getRssi()
            // Map RSSI (e.g. 0-100 or dBm) to 0..4 bars
            val bars = when {
                rssi > 70 -> 4
                rssi > 50 -> 3
                rssi > 30 -> 2
                rssi > 10 -> 1
                else -> 0
            }
            _uiState.update { it.copy(signalStrength = bars) }
        }
    }

    override fun onSearchComplete(scannedFrequencies: List<Int>) {
        viewModelScope.launch {
            val wasScanning = _uiState.value.isScanning || scannedFrequencies.isNotEmpty()
            val tuned = try {
                FmSharedPreferences.getTunedFrequency().takeIf { it > 0 } ?: _uiState.value.currentFrequencyKHz
            } catch (e: Exception) {
                _uiState.value.currentFrequencyKHz
            }
            val ps = repository.getProgramService()
            val rt = repository.getRadioText()

            Log.d(TAG, "onSearchComplete: wasScanning=$wasScanning, tuned=$tuned, ps=$ps, freqsCount=${scannedFrequencies.size}")

            if (wasScanning) {
                val sortedFrequencies = scannedFrequencies.distinct().sorted()
                val stations = sortedFrequencies.map { freq ->
                    FmStation(frequencyKHz = freq)
                }
                saveScannedStations(stations)
                autoPopulatePresetsIfEmpty(stations)
                val updatedPresets = loadPresetsFromPreferences()

                _uiState.update {
                    it.copy(
                        currentFrequencyKHz = tuned,
                        stationName = ps,
                        radioText = rt,
                        scannedStations = stations,
                        presets = updatedPresets,
                        isScanning = false,
                        isSeeking = false,
                        userMessage = if (stations.isEmpty()) "Search complete: No stations found" else "Found ${stations.size} stations"
                    )
                }
            } else {
                // Seek (>> or <<) completed: update current frequency and station info!
                _uiState.update {
                    it.copy(
                        currentFrequencyKHz = tuned,
                        stationName = ps,
                        radioText = rt,
                        isSeeking = false,
                        isScanning = false
                    )
                }
            }
        }
    }

    override fun onMute(isMuted: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMuted = isMuted) }
        }
    }

    override fun onAudioUpdate(isStereo: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStereo = isStereo) }
        }
    }

    override fun onStationRDSSupported(isRdsSupported: Boolean) {
        viewModelScope.launch {
            if (isRdsSupported) {
                _uiState.update { it.copy(rdsSupported = true) }
            }
        }
    }

    override fun onRecordingStarted() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecording = true, recordDurationSeconds = 0L) }
            startRecordTimer()
        }
    }

    override fun onRecordingStopped() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecording = false) }
            stopRecordTimer()
        }
    }

    override fun onFmAudioPathStarted() {}
    override fun onFmAudioPathStopped() {}

    private fun startRecordTimer() {
        recordTimerJob?.cancel()
        recordTimerJob = viewModelScope.launch {
            var duration = 0L
            while (isActive) {
                delay(1000L)
                duration++
                _uiState.update { it.copy(recordDurationSeconds = duration) }
            }
        }
    }

    private fun stopRecordTimer() {
        recordTimerJob?.cancel()
        recordTimerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(headsetReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering headset receiver", e)
        }
        stopRecordTimer()
        cancelSleepTimer()
        repository.unbind(getApplication())
    }
}
