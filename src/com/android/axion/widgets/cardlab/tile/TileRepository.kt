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
import com.android.axion.widgets.AxionApp
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.concurrent.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileRepository @Inject constructor(
    private val context: Context,
    private val tileConfigs: TileConfigs,
    private val scope: CoroutineScope
) : AxionProvider<TilesData> {

    private val _tileStates = MutableStateFlow(TileStates())
    private val tileStates: StateFlow<TileStates> = _tileStates.asStateFlow()
    val tilesRegistry get() = tileConfigs.tilesRegistry

    private val forceRefresh = Channel<Unit>(Channel.CONFLATED)

    override val dataFlow: Flow<Map<Int, TileData>> = flow {
        while (true) {
            emit(buildActiveTiles(tileStates.value))
            select<Unit> {
                onTimeout(3000) {}
                forceRefresh.onReceive { }
            }
        }
    }

    init {
        val initialStates = tilesRegistry.associate { tile ->
            tile.type to runCatching { tile.observeState() }.getOrDefault(false)
        }
        _tileStates.value = TileStates(initialStates)
    }

    private fun buildActiveTiles(statesSnapshot: TileStates): Map<Int, TileData> {
        val widgetIds = WidgetPrefs.getAllWidgetIds(context)
        return widgetIds.mapNotNull { widgetId ->
            val type = WidgetPrefs.getWidgetAction(context, widgetId) ?: return@mapNotNull null
            val isActive = statesSnapshot.states[type] ?: return@mapNotNull null
            val tileConfig = tilesRegistry.firstOrNull { it.type == type } ?: return@mapNotNull null
            widgetId to tileConfigs.createTileData(type, widgetId)
        }.toMap()
    }

    suspend fun updateState(type: String): Boolean {
        val tile = tilesRegistry.firstOrNull { it.type == type } ?: return false
        val newState = withContext(scope.coroutineContext) { tile.toggle() }
        updateTiles(force = true)
        forceRefresh.trySend(Unit)
        return newState
    }

    private fun updateTiles(force: Boolean = false) {
        val activeWidgetIds = WidgetPrefs.getAllWidgetIds(context)
        val activeTypes = activeWidgetIds.mapNotNull { WidgetPrefs.getWidgetAction(context, it) }.toSet()
        var hasChange = false
        val buffer = _tileStates.value.states.toMutableMap()

        for (tile in tilesRegistry) {
            if (tile.type !in activeTypes) continue
            val newState = runCatching { tile.observeState() }.getOrDefault(false)
            if (buffer[tile.type] != newState) {
                buffer[tile.type] = newState
                hasChange = true
            }
        }

        if (hasChange || force) {
            _tileStates.value = TileStates(buffer.toMap())
        }
    }

    companion object {
        fun get(context: Context): TileRepository {
            val app = context.applicationContext as AxionApp
            return app.appComponent.tileRepository()
        }
    }
}
