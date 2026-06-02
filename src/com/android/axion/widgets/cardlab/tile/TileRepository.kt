/*
 * Copyright (C) 2025-2026 AxionOS Project
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
import android.os.Vibrator
import com.android.axion.platform.AxFeatureState
import com.android.axion.platform.AxPlatformFeature
import com.android.axion.widgets.AxionApp
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.*
import com.android.axion.widgets.platform.AxPlatformBridge
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Singleton
class TileRepository
@Inject
constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val bridge: AxPlatformBridge,
) : AxionProvider<TilesData> {

    companion object {
        fun specToFeature(spec: String): String? = AxPlatformFeature.resolve(spec)

        fun get(context: Context): TileRepository {
            val app = context.applicationContext as AxionApp
            return app.appComponent.tileRepository()
        }
    }

    private val _tileStates = MutableStateFlow<Map<String, TileStateInfo>>(emptyMap())
    private val observedFeatures = mutableSetOf<String>()

    data class TileStateInfo(
        val isActive: Boolean = false,
        val label: String? = null,
        val secondaryLabel: String? = null,
        val ringerMode: Int? = null,
        val hasVibrator: Boolean = true,
    )

    override val dataFlow: Flow<TilesData?> = _tileStates.map { states -> buildActiveTiles(states) }

    init {
        startListening()
    }

    private fun startListening() {
        val widgetIds = WidgetPrefs.getAllWidgetIds(context)
        val activeSpecs = widgetIds.mapNotNull { WidgetPrefs.getWidgetAction(context, it) }.toSet()
        activeSpecs.forEach { spec -> observeSpec(spec) }
    }

    private fun observeSpec(spec: String) {
        val feature = specToFeature(spec) ?: return
        if (!observedFeatures.add(feature)) return
        scope.launch {
            bridge.stateFlow(feature).collect { state ->
                if (state.isEmpty) return@collect
                val info = parseFeatureState(feature, state)
                _tileStates.update { current -> current + (feature to info) }
            }
        }
    }

    private fun parseFeatureState(feature: String, state: AxFeatureState): TileStateInfo {
        val isRingerSpec = feature == AxPlatformFeature.RINGER_MODE
        return TileStateInfo(
            isActive = state.isActive,
            label = state.label,
            secondaryLabel = state.secondaryLabel,
            ringerMode =
                if (isRingerSpec) {
                    state.takeIf { it.hasRingerMode() }?.getRingerMode(0)
                } else {
                    null
                },
            hasVibrator =
                if (isRingerSpec) {
                    state.hasVibrator() && hasVibrator()
                } else {
                    true
                },
        )
    }

    fun startObservingSpec(spec: String) {
        observeSpec(spec)
    }

    private fun buildActiveTiles(states: Map<String, TileStateInfo>): TilesData {
        val widgetIds = WidgetPrefs.getAllWidgetIds(context)
        return widgetIds
            .mapNotNull { widgetId ->
                val spec = WidgetPrefs.getWidgetAction(context, widgetId) ?: return@mapNotNull null
                val feature = specToFeature(spec)
                val info = feature?.let { states[it] }
                val isActive = info?.isActive == true
                val ringerMode = info?.ringerMode
                val hasVibrator =
                    if (TileIcons.isRingerSpec(spec)) {
                        (info?.hasVibrator ?: true) && hasVibrator()
                    } else {
                        true
                    }
                val label = buildTileLabel(spec, info?.label, info?.secondaryLabel, ringerMode)
                widgetId to
                    TileData(
                        spec = spec,
                        isActive = isActive,
                        iconRes = TileIcons.getIcon(spec, isActive, ringerMode),
                        widgetId = widgetId,
                        label = label,
                        secondaryLabel = info?.secondaryLabel,
                        ringerMode = ringerMode,
                        hasVibrator = hasVibrator,
                    )
            }
            .toMap()
    }

    fun toggle(spec: String) {
        val feature = specToFeature(spec) ?: return
        bridge.toggle(feature)
    }

    fun setValue(spec: String, value: Int) {
        val feature = specToFeature(spec) ?: return
        bridge.setValue(feature, value)
    }

    private fun hasVibrator(): Boolean =
        context.getSystemService(Vibrator::class.java)?.hasVibrator() == true

    data class AvailableTile(val spec: String, val label: String, val category: String? = null)

    fun queryAvailableTiles(): Flow<List<AvailableTile>> = flow {
        val features = bridge.getSupportedFeatures()
        emit(
            features.map { feature ->
                val state = bridge.getState(feature)
                AvailableTile(
                    spec = feature,
                    label =
                        state.label ?: feature.replaceFirstChar { it.uppercase() },
                    category = state.category ?: AxPlatformFeature.getCategory(feature),
                )
            }
        )
    }
}

internal fun buildTileLabel(
    spec: String,
    label: String?,
    secondaryLabel: String?,
    ringerMode: Int?,
): String {
    val baseLabel = label ?: spec.replaceFirstChar { it.uppercase() }
    return if (ringerMode != null) {
        secondaryLabel?.takeIf { it.isNotEmpty() } ?: baseLabel
    } else {
        baseLabel
    }
}
