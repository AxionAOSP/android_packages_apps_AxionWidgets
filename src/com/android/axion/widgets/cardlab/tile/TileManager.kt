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
import com.android.axion.widgets.AxionApp
import com.android.axion.widgets.data.*
import com.android.axion.widgets.R
import java.util.concurrent.Executors

@Singleton
class TileManager @Inject constructor(
    private val context: Context,
    private val repository: TileRepository,
    private val tileConfigs: TileConfigs,
    private val scope: CoroutineScope
) {

    private val tilesRegistry get() = tileConfigs.tilesRegistry

    private val _tilesFlow = MutableStateFlow<Map<Int, TileData>>(emptyMap())
    var tilesFlow: Map<Int, TileData>
        get() = _tilesFlow.value
        set(value) {
            if (_tilesFlow.value != value) {
                _tilesFlow.value = value.toMap()
                value.values.forEach { data ->
                    context.updateWidget(data.widgetId, data)
                }
            }
        }

    fun updateState(widgetId: Int) {
        val type = WidgetPrefs.getWidgetAction(context, widgetId) ?: return
        scope.launch {
            val newState = repository.updateState(type)
            val tileConfig = tilesRegistry.firstOrNull { it.type == type }
            withContext(Dispatchers.Main) {
                val data = tileConfigs.createTileData(type, widgetId)
                _tilesFlow.value = _tilesFlow.value + (widgetId to data)
                context.updateWidget(widgetId, data)
            }
        }
    }

    fun getIconForTile(type: String, active: Boolean): Int {
        return tilesRegistry.firstOrNull { it.type == type }?.getIcon?.invoke(active)
            ?: R.drawable.ic_unknown
    }

    fun setTileForWidget(widgetId: Int, type: String) {
        val isActive = false
        val tileConfig = tilesRegistry.firstOrNull { it.type == type }
        val data = tileConfigs.createTileData(type, widgetId)
        _tilesFlow.value = _tilesFlow.value + (widgetId to data)
        context.updateWidget(widgetId, data)
    }

    companion object {
        fun get(context: Context): TileManager {
            val app = context.applicationContext as AxionApp
            return app.appComponent.tileManager()
        }
    }
}
