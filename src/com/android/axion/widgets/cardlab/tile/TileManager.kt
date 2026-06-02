/*
 * Copyright (C) 2025 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.axion.widgets.cardlab.tile

import android.content.Context
import android.media.AudioManager
import android.os.Vibrator
import com.android.axion.platform.AxFeatureState
import com.android.axion.widgets.AxionApp
import com.android.axion.widgets.data.*
import com.android.axion.widgets.platform.AxPlatformBridge
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@Singleton
class TileManager
@Inject
constructor(
    private val context: Context,
    private val repository: TileRepository,
    private val bridge: AxPlatformBridge,
    private val scope: CoroutineScope,
) {

    private val _tilesFlow = MutableStateFlow<Map<Int, TileData>>(emptyMap())
    var tilesFlow: Map<Int, TileData>
        get() = _tilesFlow.value
        set(value) {
            if (_tilesFlow.value != value) {
                _tilesFlow.value = value.toMap()
                value.values.forEach { data -> context.updateWidget(data.widgetId, data) }
            }
        }

    fun updateState(widgetId: Int, requestedRingerMode: Int? = null) {
        val spec = WidgetPrefs.getWidgetAction(context, widgetId) ?: return
        val current = _tilesFlow.value[widgetId] ?: createTileData(widgetId, spec)
        if (TileIcons.isRingerSpec(spec)) {
            val ringerMode = targetRingerMode(requestedRingerMode, current)
            val isActive = ringerMode == AudioManager.RINGER_MODE_NORMAL
            val updated =
                current.copy(
                    isActive = isActive,
                    iconRes = getIconForTile(spec, isActive, ringerMode),
                    ringerMode = ringerMode,
                )
            repository.setValue(spec, ringerMode)
            tilesFlow = _tilesFlow.value + (widgetId to updated)
            return
        }
        val updated =
            current.copy(
                isActive = !current.isActive,
                iconRes = getIconForTile(spec, !current.isActive),
            )
        repository.toggle(spec)
        tilesFlow = _tilesFlow.value + (widgetId to updated)
    }

    fun getIconForTile(spec: String, active: Boolean, ringerMode: Int? = null): Int =
        TileIcons.getIcon(spec, active, ringerMode)

    fun setTileForWidget(widgetId: Int, spec: String) {
        repository.startObservingSpec(spec)
        val data = createTileData(widgetId, spec)
        _tilesFlow.value = _tilesFlow.value + (widgetId to data)
        context.updateWidget(widgetId, data)
    }

    private fun createTileData(widgetId: Int, spec: String): TileData {
        val isRingerSpec = TileIcons.isRingerSpec(spec)
        val feature = TileRepository.specToFeature(spec)
        val state = feature?.let { bridge.getState(it) }
        val label = state?.label
        val secondaryLabel = state?.secondaryLabel
        val hasVibrator =
            if (isRingerSpec) {
                val platformHasVibrator =
                    state
                        ?.takeIf { it.containsKey(AxFeatureState.KEY_HAS_VIBRATOR) }
                        ?.hasVibrator()
                        ?: true
                platformHasVibrator && hasVibrator()
            } else {
                true
            }
        val ringerMode =
            if (isRingerSpec) {
                state
                    ?.takeIf { it.hasRingerMode() }
                    ?.getRingerMode(AudioManager.RINGER_MODE_NORMAL)
                    ?: localRingerMode()
            } else {
                null
            }
        val isActive =
            ringerMode?.let { it == AudioManager.RINGER_MODE_NORMAL }
                ?: state?.isActive
                ?: false
        return TileData(
            spec = spec,
            isActive = isActive,
            iconRes = TileIcons.getIcon(spec, isActive, ringerMode),
            widgetId = widgetId,
            label = buildTileLabel(spec, label, secondaryLabel, ringerMode),
            secondaryLabel = secondaryLabel,
            ringerMode = ringerMode,
            hasVibrator = hasVibrator,
        )
    }

    private fun targetRingerMode(
        requestedRingerMode: Int?,
        current: TileData,
    ): Int {
        val canVibrate = current.hasVibrator && hasVibrator()
        return sanitizeRingerMode(requestedRingerMode, canVibrate)
            ?: nextRingerMode(current.ringerMode ?: localRingerMode(), canVibrate)
    }

    private fun sanitizeRingerMode(ringerMode: Int?, hasVibrator: Boolean): Int? =
        when (ringerMode) {
            AudioManager.RINGER_MODE_NORMAL,
            AudioManager.RINGER_MODE_SILENT -> ringerMode
            AudioManager.RINGER_MODE_VIBRATE ->
                if (hasVibrator) AudioManager.RINGER_MODE_VIBRATE
                else AudioManager.RINGER_MODE_SILENT
            else -> null
        }

    private fun nextRingerMode(ringerMode: Int?, hasVibrator: Boolean): Int =
        when (ringerMode) {
            AudioManager.RINGER_MODE_NORMAL ->
                if (hasVibrator) {
                    AudioManager.RINGER_MODE_VIBRATE
                } else {
                    AudioManager.RINGER_MODE_SILENT
                }
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }

    private fun localRingerMode(): Int? =
        context.getSystemService(AudioManager::class.java)?.ringerMode

    private fun hasVibrator(): Boolean =
        context.getSystemService(Vibrator::class.java)?.hasVibrator() == true

    companion object {
        fun get(context: Context): TileManager {
            val app = context.applicationContext as AxionApp
            return app.appComponent.tileManager()
        }
    }
}
