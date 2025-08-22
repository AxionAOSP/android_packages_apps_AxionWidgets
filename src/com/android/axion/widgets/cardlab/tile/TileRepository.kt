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
import com.android.axion.widgets.WidgetLifecycleManager
import com.android.axion.widgets.R
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.concurrent.*
import javax.inject.Inject
import javax.inject.Singleton

data class TileStates(val states: Map<String, Boolean> = emptyMap())

@Singleton
class TileRepository @Inject constructor(
    private val context: Context,
    private val lifecycleManager: WidgetLifecycleManager
) {

    private val tileConfigs = TileConfigs(context)
    private val _tileStates = MutableStateFlow(TileStates())

    val tileStates: StateFlow<TileStates> = _tileStates.asStateFlow()
    val tilesRegistry get() = tileConfigs.tilesRegistry
    
    private val trigger = Channel<Unit>(Channel.CONFLATED)
    private val buffer = mutableMapOf<String, Boolean>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    val activeTilesFlow: Flow<Map<Int, TileData>> = tileStates
        .map { statesSnapshot ->
            val widgetIds = WidgetPrefs.getAllWidgetIds(context)
            val result = mutableMapOf<Int, TileData>()
            for (widgetId in widgetIds) {
                val type = WidgetPrefs.getWidgetAction(context, widgetId) ?: continue
                val isActive = statesSnapshot.states[type] ?: continue
                val tileConfig = tilesRegistry.firstOrNull { it.type == type } ?: continue
                result[widgetId] = TileData(
                    type,
                    isActive,
                    tileConfig.getIcon?.invoke(isActive) ?: R.drawable.ic_wifi_off,
                    widgetId,
                    tileConfig.getLabel?.invoke()
                )
            }
            result
        }
        .distinctUntilChanged()

    init {
        val initialStates = tilesRegistry.associate { tile ->
            tile.type to runCatching { tile.observeState() }.getOrDefault(false)
        }
        _tileStates.value = TileStates(initialStates)
        buffer.putAll(initialStates)

        scope.launch {
            lifecycleManager.widgetsActive.collect { active ->
                if (active) start() else pause()
            }
        }
    }

    private fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                select<Unit> {
                    onTimeout(3000L) {
                        updateTiles()
                    }
                    trigger.onReceive {
                        updateTiles()
                    }
                }
            }
        }
    }

    private fun pause() {
        job?.cancel()
        job = null
    }

    suspend fun updateState(type: String): Boolean {
        val tile = tilesRegistry.firstOrNull { it.type == type } ?: return false
        val newState = withContext(Dispatchers.Default) { tile.toggle() }
        buffer[type] = newState
        trigger.trySend(Unit)
        return newState
    }

    private fun updateTiles() {
        val activeWidgetIds = WidgetPrefs.getAllWidgetIds(context)
        val activeTypes = activeWidgetIds.mapNotNull { WidgetPrefs.getWidgetAction(context, it) }.toSet()
        var hasChange = false
        for (tile in tilesRegistry) {
            if (tile.type !in activeTypes) continue
            val newState = runCatching { tile.observeState() }.getOrDefault(false)
            if (buffer[tile.type] != newState) {
                buffer[tile.type] = newState
                hasChange = true
            }
        }
        if (hasChange) {
            _tileStates.value = TileStates(buffer.toMap())
        } else {
            _tileStates.value = TileStates(buffer.toMap())
        }
    }

    fun dispose() {
        pause()
        scope.cancel()
        _tileStates.value = TileStates()
    }
}
