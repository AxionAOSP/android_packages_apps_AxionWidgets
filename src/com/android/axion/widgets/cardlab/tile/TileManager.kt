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
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

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
    private val repository: TileRepository
) {

    private val bgDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + bgDispatcher)

    private val consumers = mutableSetOf<Any>()

    private val _tilesFlow = MutableStateFlow<Map<Int, TileData>>(emptyMap())
    val tilesFlow: StateFlow<Map<Int, TileData>> = _tilesFlow.asStateFlow()

    init {
        observeRepository()
    }

    fun addConsumer(consumer: Any) {
        consumers.add(consumer)
    }

    fun removeConsumer(consumer: Any) {
        consumers.remove(consumer)
        if (consumers.isEmpty()) {
            dispose()
        }
    }

    private fun observeRepository() {
        scope.launch {
            combine(repository.tileStates, snapshotWidgetIds()) { states, ids ->
                ids.mapNotNull { id ->
                    val type = WidgetPrefs.getWidgetAction(context, id) ?: return@mapNotNull null
                    val isActive = states.states[type] ?: false
                    val tileConfig = repository.tilesRegistry.firstOrNull { it.type == type }
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
                        context.updateWidget(data.widgetId, data)
                    }
                }
            }
        }
    }

    private fun snapshotWidgetIds(): Flow<List<Int>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(WidgetPrefs.getAllWidgetIds(context))
            delay(1000)
        }
    }.flowOn(bgDispatcher)

    fun updateState(widgetId: Int) {
        val type = WidgetPrefs.getWidgetAction(context, widgetId) ?: return
        scope.launch {
            val newState = repository.updateState(type)
            val tileConfig = repository.tilesRegistry.firstOrNull { it.type == type }
            withContext(Dispatchers.Main) {
                val data = TileData(
                    type,
                    newState,
                    getIconForTile(type, newState),
                    widgetId,
                    tileConfig?.getLabel?.invoke()
                )
                _tilesFlow.value = _tilesFlow.value + (widgetId to data)
                context.updateWidget(widgetId, data)
            }
        }
    }

    private fun dispose() {
        scope.cancel()
        bgDispatcher.close()
        repository.dispose()
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
}
