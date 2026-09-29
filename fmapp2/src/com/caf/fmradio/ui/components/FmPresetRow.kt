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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.caf.fmradio.data.FmStation
import com.caf.fmradio.viewmodel.FmUiState

@Deprecated("Use FmFavoritesList instead")
@Composable
fun FmPresetRow(
    uiState: FmUiState,
    onTune: (Int) -> Unit,
    onToggleCurrentPreset: () -> Unit,
    onPresetLongClick: (FmStation) -> Unit,
    modifier: Modifier = Modifier
) {
    FmFavoritesList(
        favorites = uiState.favorites,
        currentFrequencyKHz = uiState.currentFrequencyKHz,
        isCurrentFavorite = uiState.isCurrentFavorite,
        isPoweredOn = uiState.isPoweredOn,
        onTune = onTune,
        onToggleCurrentFavorite = onToggleCurrentPreset,
        onRenameFavorite = onPresetLongClick,
        onRemoveFavorite = onPresetLongClick,
        modifier = modifier
    )
}
