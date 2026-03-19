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
import android.os.Bundle
import com.android.axion.platform.AxPlatformClient
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
        fun specToFeature(spec: String): String? = AxPlatformClient.resolveFeature(spec)

        fun get(context: Context): TileRepository {
            val app = context.applicationContext as AxionApp
            return app.appComponent.tileRepository()
        }
    }

    private val _tileStates = MutableStateFlow<Map<String, TileStateInfo>>(emptyMap())
    private val observedFeatures = mutableSetOf<String>()

    data class TileStateInfo(
        val spec: String,
        val isActive: Boolean = false,
        val isAvailable: Boolean = true,
        val label: String? = null,
        val secondaryLabel: String? = null,
        val tileState: Int = AxPlatformClient.TILE_STATE_INACTIVE,
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
            bridge.stateFlow(feature).collect { bundle ->
                if (bundle.isEmpty) return@collect
                val info = parseFeatureBundle(spec, feature, bundle)
                _tileStates.update { current -> current + (spec to info) }
            }
        }
    }

    private fun parseFeatureBundle(spec: String, feature: String, bundle: Bundle): TileStateInfo {
        return TileStateInfo(
            spec = spec,
            isActive = bundle.getBoolean("active", false),
            isAvailable = bundle.getBoolean("available", true),
            label = AxPlatformClient.getLabel(bundle),
            secondaryLabel = AxPlatformClient.getSecondaryLabel(bundle),
            tileState = AxPlatformClient.getTileState(bundle),
        )
    }

    fun startObservingSpec(spec: String) {
        if (_tileStates.value.containsKey(spec)) return
        observeSpec(spec)
    }

    private fun buildActiveTiles(states: Map<String, TileStateInfo>): TilesData {
        val widgetIds = WidgetPrefs.getAllWidgetIds(context)
        return widgetIds
            .mapNotNull { widgetId ->
                val spec = WidgetPrefs.getWidgetAction(context, widgetId) ?: return@mapNotNull null
                val info = states[spec]
                val isActive = info?.isActive == true
                widgetId to
                    TileData(
                        spec = spec,
                        isActive = isActive,
                        iconRes = TileIcons.getIcon(spec, isActive),
                        widgetId = widgetId,
                        label = info?.label ?: spec.replaceFirstChar { it.uppercase() },
                        secondaryLabel = info?.secondaryLabel,
                    )
            }
            .toMap()
    }

    fun toggle(spec: String) {
        val feature = specToFeature(spec) ?: return
        bridge.toggle(feature)
    }

    data class AvailableTile(val spec: String, val label: String, val category: String? = null)

    fun queryAvailableTiles(): Flow<List<AvailableTile>> = flow {
        val features = bridge.getSupportedFeatures()
        emit(
            features.map { feature ->
                val state = bridge.getState(feature)
                AvailableTile(
                    spec = feature,
                    label =
                        AxPlatformClient.getLabel(state)
                            ?: feature.replaceFirstChar { it.uppercase() },
                    category = AxPlatformClient.getCategory(feature),
                )
            }
        )
    }
}
