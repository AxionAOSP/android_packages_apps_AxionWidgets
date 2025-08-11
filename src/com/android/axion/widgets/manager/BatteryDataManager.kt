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
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.provider.BatteryStatusProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.*

object BatteryDataManager {

    private var appContext: Context? = null
    private var batteryStatusProvider: BatteryStatusProvider? = null

    private val coroutineScope: CoroutineScope = MainScope()

    private val _batteryFlow = MutableStateFlow<QuickLookData.Battery?>(null)

    val batteryFlow: (Context) -> StateFlow<QuickLookData.Battery?> = { context ->
        ensureInitialized(context)
        _batteryFlow
            .onSubscription { startIfNeeded() }
            .onCompletion { stopIfUnused() }
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = _batteryFlow.value
            )
    }

    private fun ensureInitialized(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    private fun startIfNeeded() {
        if (batteryStatusProvider == null && appContext != null) {
            batteryStatusProvider = BatteryStatusProvider(appContext!!).also {
                it.addCallback(batteryCallback)
            }
        }
    }

    private fun stopIfUnused() {
        if (_batteryFlow.subscriptionCount.value > 0) {
            batteryStatusProvider?.removeCallback(batteryCallback)
            batteryStatusProvider = null
        }
    }

    private val batteryCallback = object : BatteryStatusProvider.Callback {
        override fun onBatteryStatusChanged(battery: QuickLookData.Battery?) {
            _batteryFlow.value = battery
        }
    }

    fun cleanup() {
        batteryStatusProvider?.removeCallback(batteryCallback)
        batteryStatusProvider = null
        appContext = null
    }
    
    fun refresh() {
        _batteryFlow.value = _batteryFlow.value
    }
}
