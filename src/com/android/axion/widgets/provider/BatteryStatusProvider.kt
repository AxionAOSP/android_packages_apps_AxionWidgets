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
package com.android.axion.widgets.provider

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.utils.broadcastFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryStatusProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CoroutineScope
) : AxionProvider<QuickLookData.Battery> {

    private val PLUGGED_TYPES = setOf(
        BatteryManager.BATTERY_PLUGGED_AC,
        BatteryManager.BATTERY_PLUGGED_USB,
        BatteryManager.BATTERY_PLUGGED_WIRELESS,
        BatteryManager.BATTERY_PLUGGED_DOCK
    )

    private val CHARGING_TYPES = setOf(
        BatteryManager.BATTERY_STATUS_CHARGING,
        BatteryManager.BATTERY_STATUS_FULL
    )

    private val batteryManager by lazy { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }

    override val dataFlow: Flow<QuickLookData.Battery?> = context.broadcastFlow(
        filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        scope = scope
    ) { intent ->
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        val isCharging = status in CHARGING_TYPES
        val isPluggedIn = plugged in PLUGGED_TYPES
        val isPowered = isCharging || isPluggedIn

        val batteryPct = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            .takeIf { it >= 0 }?.let { level ->
                intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    .takeIf { it > 0 }?.let { scale -> (level * 100f / scale).toInt() } ?: -1
            } ?: -1

        val chargeTimeRemaining = batteryManager.computeChargeTimeRemaining()

        QuickLookData.Battery(isPowered, batteryPct, chargeTimeRemaining)
    }
}
