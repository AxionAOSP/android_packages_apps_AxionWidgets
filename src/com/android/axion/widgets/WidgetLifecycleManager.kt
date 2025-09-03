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
package com.android.axion.widgets

import android.app.ActivityTaskManager
import android.app.TaskStackListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.RemoteException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetLifecycleManager @Inject constructor(
    private val context: Context
) {
    private val _widgetsActive = MutableStateFlow(false)
    val widgetsActive: StateFlow<Boolean> = _widgetsActive

    private val listeners = mutableSetOf<Any>()
    private var initialized = false

    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val taskListener = object : TaskStackListener() {
        override fun onTaskStackChanged() = checkFocusedTask()
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    updateWidgetsState()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    checkFocusedTask()
                }
            }
        }
    }

    private var isScreenOn = true
    private var isLauncherVisible = false

    private fun initialize() {
        if (initialized) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenReceiver, filter)

        try {
            ActivityTaskManager.getService().registerTaskStackListener(taskListener)
        } catch (e: RemoteException) {
        }

        checkFocusedTask()
        initialized = true
    }

    private fun checkFocusedTask() {
        val topPackage = try {
            ActivityTaskManager.getService().getFocusedRootTaskInfo()?.topActivity?.packageName
        } catch (e: RemoteException) {
            null
        }
        isLauncherVisible = topPackage == "com.android.launcher3"
        updateWidgetsState()
    }

    private fun updateWidgetsState() {
        val active = isScreenOn && isLauncherVisible
        job = scope.launch {
            _widgetsActive.emit(active)
        }
    }

    fun addListener(listener: Any) {
        listeners.add(listener)
        initialize()
        updateWidgetsState()
    }

    fun removeListener(listener: Any) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            dispose()
        } else {
            updateWidgetsState()
        }
    }

    fun dispose() {
        try {
            context.unregisterReceiver(screenReceiver)
            ActivityTaskManager.getService().unregisterTaskStackListener(taskListener)
        } catch (e: Exception) {
        }
        job?.cancel()
        job = null
    }
}
