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

package com.caf.fmradio.data

/**
 * Immutable representation of an FM radio station or preset.
 *
 * @param frequencyKHz Frequency in kHz (e.g. 103700 for 103.7 MHz)
 * @param name Display name or RDS Program Service name
 * @param isPreset Whether this station is saved in user favorites/presets
 * @param rdsSupported Whether this station supports RDS features
 */
data class FmStation(
    val frequencyKHz: Int,
    val name: String = "",
    val isPreset: Boolean = false,
    val rdsSupported: Boolean = false
) {
    /**
     * Formats frequency into MHz string representation, e.g. "103.7"
     */
    val frequencyMHzString: String
        get() = String.format(java.util.Locale.US, "%.1f", frequencyKHz / 1000.0)

    val displayName: String
        get() = name.ifBlank { "$frequencyMHzString MHz" }
}
