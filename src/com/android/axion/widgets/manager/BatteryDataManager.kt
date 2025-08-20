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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    batteryStatusProvider: BatteryStatusProvider
) {

    private val coroutineScope: CoroutineScope = MainScope()

    private val batteryStatusProvider: BatteryStatusProvider by lazy {
        BatteryStatusProvider(context).also {
            it.addCallback(batteryCallback)
        }
    }

    private val _batteryFlow = MutableStateFlow<QuickLookData.Battery?>(null)

    val batteryFlow: StateFlow<QuickLookData.Battery?> = _batteryFlow
        .onSubscription { startIfNeeded() }
        .onCompletion { stopIfUnused() }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = _batteryFlow.value
        )

    private val batteryCallback = object : BatteryStatusProvider.Callback {
        override fun onBatteryStatusChanged(battery: QuickLookData.Battery?) {
            _batteryFlow.value = battery
        }
    }

    private fun startIfNeeded() {
        batteryStatusProvider.addCallback(batteryCallback)
    }

    private fun stopIfUnused() {
        if (_batteryFlow.subscriptionCount.value == 0) {
            batteryStatusProvider.removeCallback(batteryCallback)
        }
    }

    fun refresh() {
        _batteryFlow.value = _batteryFlow.value
    }

    fun cleanup() {
        batteryStatusProvider.removeCallback(batteryCallback)
    }
}
