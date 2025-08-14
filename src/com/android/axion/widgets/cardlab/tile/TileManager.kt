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
import com.android.axion.widgets.R

data class TileData(
    val type: String,
    val isActive: Boolean,
    val iconRes: Int,
    val widgetId: Int,
    val label: String? = null,
)

object TileManager {

    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val consumers = mutableSetOf<Any>()

    private val _tilesFlow = MutableStateFlow<Map<Int, TileData>>(emptyMap())
    val tilesFlow: StateFlow<Map<Int, TileData>> = _tilesFlow.asStateFlow()

    fun addConsumer(consumer: Any) {
        consumers.add(consumer)
        if (consumers.size == 1) {
            appContext?.let { TileRepository.bind(it) }
        }
    }

    fun removeConsumer(consumer: Any) {
        consumers.remove(consumer)
        if (consumers.isEmpty()) {
            dispose()
        }
    }

    fun bind(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        if (consumers.isNotEmpty()) {
            TileRepository.bind(appContext!!)
        }
        observeRepository()
    }

    private fun observeRepository() {
        scope.launch {
            TileRepository.tileStates
                .combine(snapshotWidgetIds()) { states, ids ->
                    ids.mapNotNull { id ->
                        val type = WidgetPrefs.getWidgetAction(appContext!!, id) ?: return@mapNotNull null
                        val isActive = states.states[type] ?: false
                        val tileConfig = TileRepository.tilesRegistry.firstOrNull { it.type == type }
                        id to TileData(
                            type,
                            isActive,
                            getIconForTile(type, isActive),
                            id,
                            tileConfig?.getLabel?.invoke()
                        )
                    }.toMap()
                }
                .distinctUntilChanged()
                .collect { updated ->
                    _tilesFlow.value = updated
                    withContext(Dispatchers.Main) {
                        updated.values.forEach { data ->
                            appContext?.updateWidget(data.widgetId, data)
                        }
                    }
                }
        }
    }

    private fun snapshotWidgetIds(): Flow<List<Int>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(WidgetPrefs.getAllWidgetIds(appContext!!))
            delay(1000)
        }
    }

    fun updateState(widgetId: Int) {
        val type = WidgetPrefs.getWidgetAction(appContext!!, widgetId) ?: return
        scope.launch {
            val newState = TileRepository.updateState(type)
            val tileConfig = TileRepository.tilesRegistry.firstOrNull { it.type == type }
            withContext(Dispatchers.Main) {
                val data = TileData(
                    type,
                    newState,
                    getIconForTile(type, newState),
                    widgetId,
                    tileConfig?.getLabel?.invoke()
                )
                _tilesFlow.value = _tilesFlow.value + (widgetId to data)
                appContext?.updateWidget(widgetId, data)
            }
        }
    }

    private fun dispose() {
        scope.cancel()
        TileRepository.dispose()
        appContext = null
    }

    fun getIconForTile(type: String, active: Boolean): Int {
        return TileRepository.tilesRegistry.firstOrNull { it.type == type }?.getIcon?.invoke(active)
            ?: R.drawable.ic_wifi_off
    }

    fun setTileForWidget(widgetId: Int, type: String) {
        if (appContext == null) return
        val isActive = false
        val tileConfig = TileRepository.tilesRegistry.firstOrNull { it.type == type }
        val data = TileData(
            type,
            isActive,
            getIconForTile(type, isActive),
            widgetId,
            tileConfig?.getLabel?.invoke()
        )
        _tilesFlow.value = _tilesFlow.value + (widgetId to data)
        appContext?.updateWidget(widgetId, data)
    }
}
