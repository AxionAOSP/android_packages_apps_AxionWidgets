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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.utils.WeakListenerManager

class BatteryStatusProvider(private val context: Context) {

    interface Callback {
        fun onBatteryStatusChanged(battery: QuickLookData.Battery?)
    }

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val listenerManager = WeakListenerManager<Callback>().apply {
        setLifecycleCallbacks(
            onActive = { startListening() },
            onInactive = { stopListening() }
        )
    }

    fun addCallback(cb: Callback) {
        listenerManager.addListener(cb)
    }

    fun removeCallback(cb: Callback) {
        listenerManager.removeListener(cb)
    }

    private fun startListening() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(batteryReceiver, filter)
    }

    private fun stopListening() {
        context.unregisterReceiver(batteryReceiver)
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED,
                Intent.ACTION_POWER_DISCONNECTED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    val batteryPct = (level * 100) / scale
                    val chargeTimeRemaining = batteryManager.computeChargeTimeRemaining()
                    val batteryData = QuickLookData.Battery(isCharging, batteryPct, chargeTimeRemaining)
                    listenerManager.notify { it.onBatteryStatusChanged(batteryData) }
                }
            }
        }
    }
}
