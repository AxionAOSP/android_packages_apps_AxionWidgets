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
package com.android.axion.widgets.manager

import android.content.Context
import com.android.axion.widgets.AxionApp
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.manager.BatteryDataManager
import com.android.axion.widgets.WidgetLifecycleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryDataManager: BatteryDataManager,
    private val lifecycleManager: WidgetLifecycleManager
) {

    interface Callback {
        fun onBatteryUpdated(data: QuickLookData.Battery?)
    }

    private val listeners = mutableSetOf<Callback>()
    private val coroutineScope: CoroutineScope = MainScope()

    private val _batteryState = MutableStateFlow<QuickLookData.Battery?>(null)
    val batteryState: StateFlow<QuickLookData.Battery?> = _batteryState.asStateFlow()

    private var batteryFlowJob: Job? = null

    fun init() {
        coroutineScope.launch {
            lifecycleManager.addListener(this)
            lifecycleManager.widgetsActive.collect { active ->
                if (active) start() else pause()
            }
        }
        start()
    }

    private fun start() {
        batteryFlowJob?.cancel()
        batteryFlowJob = batteryDataManager.batteryFlow
            .onEach { battery ->
                _batteryState.value = battery
                listeners.forEach { it.onBatteryUpdated(battery) }
            }
            .launchIn(coroutineScope)
    }

    private fun pause() {
        batteryFlowJob?.cancel()
        batteryFlowJob = null
    }

    fun addListener(listener: Callback) {
        listeners.add(listener)
        listener.onBatteryUpdated(_batteryState.value)
    }

    fun getBatteryData(): QuickLookData.Battery? = _batteryState.value

    fun dispose() {
        pause()
        listeners.clear()
        lifecycleManager.removeListener(this)
    }

    companion object {
        fun get(context: Context): BatteryWidgetManager {
            val app = context.applicationContext as AxionApp
            return app.appComponent.batteryWidgetManager()
        }
    }
}
