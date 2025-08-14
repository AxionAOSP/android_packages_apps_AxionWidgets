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

data class TileStates(val states: Map<String, Boolean> = emptyMap())

object TileRepository {

    private var context: Context? = null
    private var tileConfigs: TileConfigs? = null
    private var scope: CoroutineScope? = null
    private var pollingJob: Job? = null

    private val _tileStates = MutableStateFlow(TileStates())
    val tileStates: StateFlow<TileStates> = _tileStates.asStateFlow()

    val tilesRegistry get() = tileConfigs?.tilesRegistry ?: emptyList()

    fun bind(appContext: Context) {
        if (context != null) return
        context = appContext.applicationContext
        tileConfigs = TileConfigs(context!!)
        val initialStates = runBlocking {
            tileConfigs!!.tilesRegistry.associate { tile ->
                tile.type to runCatching { tile.observeState() }.getOrDefault(false)
            }
        }
        _tileStates.value = TileStates(initialStates)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        pollingJob = scope!!.launch {
            while (isActive) {
                val newStates = mutableMapOf<String, Boolean>()
                tilesRegistry.forEach { tile ->
                    val state = runCatching { tile.observeState() }.getOrDefault(false)
                    newStates[tile.type] = state
                }
                _tileStates.value = TileStates(newStates)
                delay(500L)
            }
        }
    }

    suspend fun updateState(type: String): Boolean {
        val tile = tilesRegistry.firstOrNull { it.type == type } ?: return false
        val newState = withContext(Dispatchers.Default) { tile.toggle() }
        _tileStates.value = _tileStates.value.copy(
            states = _tileStates.value.states + (type to newState)
        )
        return newState
    }

    fun dispose() {
        pollingJob?.cancel()
        pollingJob = null
        scope?.cancel()
        scope = null
        context = null
        tileConfigs = null
        _tileStates.value = TileStates()
    }
}
