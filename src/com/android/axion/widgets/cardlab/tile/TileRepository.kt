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

import android.app.ActivityTaskManager
import android.app.TaskStackListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.RemoteException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TileStates(val states: Map<String, Boolean> = emptyMap())

@Singleton
class TileRepository @Inject constructor(
    private val context: Context
) {

    private var tileConfigs: TileConfigs? = null
    private var isScreenOn = true
    private var isUserPresent = true

    private val pollingBuffer = mutableMapOf<String, Boolean>()
    private val _tileStates = MutableStateFlow(TileStates())
    val tileStates: StateFlow<TileStates> = _tileStates.asStateFlow()

    val tilesRegistry get() = tileConfigs?.tilesRegistry ?: emptyList()

    private val pollingExecutor = Executors.newSingleThreadScheduledExecutor()
    private var pollingFuture: ScheduledFuture<*>? = null

    private val debounceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var debounceJob: Job? = null

    private val taskListener = object : TaskStackListener() {
        override fun onTaskStackChanged() {
            debounceCheckFocusedTask()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    stopPolling()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    checkFocusedTask()
                }
                Intent.ACTION_USER_PRESENT -> {
                    isUserPresent = true
                    checkFocusedTask()
                }
            }
        }
    }

    init {
        tileConfigs = TileConfigs(context)
        val initialStates = tileConfigs!!.tilesRegistry.associate { tile ->
            tile.type to runCatching { tile.observeState() }.getOrDefault(false)
        }
        _tileStates.value = TileStates(initialStates)
        pollingBuffer.putAll(initialStates)

        try {
            ActivityTaskManager.getService().registerTaskStackListener(taskListener)
        } catch (_: RemoteException) { }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(screenReceiver, filter)

        checkFocusedTask()
    }

    private fun debounceCheckFocusedTask(delayMs: Long = 800L) {
        debounceJob?.cancel()
        debounceJob = debounceScope.launch {
            delay(delayMs)
            checkFocusedTask()
        }
    }

    private fun checkFocusedTask() {
        val topPackage = getFocusedRootTaskPackage()
        if (topPackage == "com.android.launcher3" && isScreenOn && isUserPresent) {
            startPolling()
        } else {
            stopPolling()
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

    private fun getFocusedRootTaskPackage(): String? {
        return try {
            ActivityTaskManager.getService().getFocusedRootTaskInfo()?.topActivity?.packageName
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateState(type: String): Boolean {
        val tile = tilesRegistry.firstOrNull { it.type == type } ?: return false
        val newState = withContext(Dispatchers.Default) { tile.toggle() }
        _tileStates.update { it.copy(states = it.states + (type to newState)) }
        return newState
    }

    fun dispose() {
        stopPolling()
        debounceJob?.cancel()
        debounceJob = null
        debounceScope.cancel()
        pollingExecutor.shutdownNow()
        context.unregisterReceiver(screenReceiver)
        _tileStates.value = TileStates()
        try {
            ActivityTaskManager.getService().unregisterTaskStackListener(taskListener)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}
