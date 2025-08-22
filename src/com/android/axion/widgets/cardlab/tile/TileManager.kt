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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import com.android.axion.widgets.WidgetLifecycleManager
import com.android.axion.widgets.R
import java.util.concurrent.Executors

data class TileData(
    val type: String,
    val isActive: Boolean,
    val iconRes: Int,
    val widgetId: Int,
    val label: String? = null,
)

@Singleton
class TileManager @Inject constructor(
    private val context: Context,
    private val repository: TileRepository,
    private val lifecycleManager: WidgetLifecycleManager
) {

    private val bgDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + bgDispatcher)

    private val _tilesFlow = MutableStateFlow<Map<Int, TileData>>(emptyMap())
    val tilesFlow: StateFlow<Map<Int, TileData>> = _tilesFlow.asStateFlow()

    private val consumers = mutableSetOf<Any>()
    private var repoJob: Job? = null

    init {
        scope.launch {
            lifecycleManager.widgetsActive.collect { active ->
                if (active) start() else pause()
            }
        }
    }

    fun addConsumer(consumer: Any) = consumers.add(consumer)
    fun removeConsumer(consumer: Any) {
        consumers.remove(consumer)
        if (consumers.isEmpty()) dispose()
    }

    private fun start() {
        if (repoJob?.isActive == true) return
        repoJob = scope.launch {
            repository.activeTilesFlow.collect { tiles ->
                _tilesFlow.value = tiles
                withContext(Dispatchers.Main) {
                    tiles.values.forEach { data ->
                        context.updateWidget(data.widgetId, data)
                    }
                }
            }
        }
    }

    private fun pause() {
        repoJob?.cancel()
        repoJob = null
    }

    fun updateState(widgetId: Int) {
        val type = WidgetPrefs.getWidgetAction(context, widgetId) ?: return
        scope.launch {
            val newState = repository.updateState(type)
            val tileConfig = repository.tilesRegistry.firstOrNull { it.type == type }
            withContext(Dispatchers.Main) {
                val data = TileData(
                    type,
                    newState,
                    tileConfig?.getIcon?.invoke(newState) ?: R.drawable.ic_wifi_off,
                    widgetId,
                    tileConfig?.getLabel?.invoke()
                )
                _tilesFlow.value = _tilesFlow.value + (widgetId to data)
                context.updateWidget(widgetId, data)
            }
        }
    }

    fun getIconForTile(type: String, active: Boolean): Int {
        return repository.tilesRegistry.firstOrNull { it.type == type }?.getIcon?.invoke(active)
            ?: R.drawable.ic_wifi_off
    }

    fun setTileForWidget(widgetId: Int, type: String) {
        val isActive = false
        val tileConfig = repository.tilesRegistry.firstOrNull { it.type == type }
        val data = TileData(
            type,
            isActive,
            getIconForTile(type, isActive),
            widgetId,
            tileConfig?.getLabel?.invoke()
        )
        _tilesFlow.value = _tilesFlow.value + (widgetId to data)
        context.updateWidget(widgetId, data)
    }

    fun dispose() {
        pause()
        scope.cancel()
        bgDispatcher.close()
        repository.dispose()
    }
}
