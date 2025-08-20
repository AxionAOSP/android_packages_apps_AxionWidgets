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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import com.android.axion.widgets.WidgetLifecycleManager

data class TileStates(val states: Map<String, Boolean> = emptyMap())

@Singleton
class TileRepository @Inject constructor(
    private val context: Context,
    private val lifecycleManager: WidgetLifecycleManager
) {

    private val tileConfigs = TileConfigs(context)
    private val pollingBuffer = mutableMapOf<String, Boolean>()
    private val _tileStates = MutableStateFlow(TileStates())
    val tileStates: StateFlow<TileStates> = _tileStates.asStateFlow()

    val tilesRegistry get() = tileConfigs.tilesRegistry

    private val pollingExecutor = Executors.newSingleThreadScheduledExecutor()
    private var pollingFuture: ScheduledFuture<*>? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        val initialStates = tilesRegistry.associate { tile ->
            tile.type to runCatching { tile.observeState() }.getOrDefault(false)
        }
        _tileStates.value = TileStates(initialStates)
        pollingBuffer.putAll(initialStates)
        coroutineScope.launch {
            lifecycleManager.widgetsActive.collect { active ->
                if (active) startPolling() else stopPolling()
            }
        }
    }

    private fun startPolling() {
        if (pollingFuture?.isDone == false) return
        pollingFuture = pollingExecutor.scheduleWithFixedDelay({
            val currentStates = mutableMapOf<String, Boolean>()
            var hasChange = false
            for (tile in tilesRegistry) {
                val newState = runCatching { tile.observeState() }.getOrDefault(false)
                currentStates[tile.type] = newState
                if (pollingBuffer[tile.type] != newState) hasChange = true
            }
            if (hasChange) {
                pollingBuffer.clear()
                pollingBuffer.putAll(currentStates)
                _tileStates.value = TileStates(pollingBuffer.toMap())
            }
        }, 0, 500L, TimeUnit.MILLISECONDS)
    }

    private fun stopPolling() {
        pollingFuture?.cancel(false)
        pollingFuture = null
    }

    suspend fun updateState(type: String): Boolean {
        val tile = tilesRegistry.firstOrNull { it.type == type } ?: return false
        val newState = withContext(Dispatchers.Default) { tile.toggle() }
        _tileStates.update { it.copy(states = it.states + (type to newState)) }
        return newState
    }

    fun dispose() {
        stopPolling()
        coroutineScope.cancel()
        _tileStates.value = TileStates()
    }
}
