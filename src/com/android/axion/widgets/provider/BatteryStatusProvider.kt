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

    private var isRegistered = false

    fun addCallback(cb: Callback) {
        listenerManager.addListener(cb)
        updateCallback(cb)
    }

    fun removeCallback(cb: Callback) = listenerManager.removeListener(cb)

    private fun startListening() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
            isRegistered = true
        }
    }

    private fun stopListening() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (e: Exception) {
            }
            isRegistered = false
        }
    }

    private fun isChargingOrPlugged(intent: Intent): Boolean {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL
        val pluggedIn = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                        plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                        plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS ||
                        plugged == BatteryManager.BATTERY_PLUGGED_DOCK
        return charging || pluggedIn
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val batteryData = parseIntentToBattery(intent)
            listenerManager.notify { it.onBatteryStatusChanged(batteryData) }
        }
    }

    private fun parseIntentToBattery(intent: Intent): QuickLookData.Battery? {
        val isCharging = isChargingOrPlugged(intent)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100f / scale).toInt()
        } else {
            -1
        }
        val chargeTimeRemaining = batteryManager.computeChargeTimeRemaining()
        return QuickLookData.Battery(isCharging, batteryPct, chargeTimeRemaining)
    }

    private fun updateCallback(cb: Callback) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = context.registerReceiver(null, filter)
        if (sticky != null) {
            val batteryData = parseIntentToBattery(sticky)
            try {
                cb.onBatteryStatusChanged(batteryData)
            } catch (e: Exception) {
            }
        }
    }

    fun getBatteryDataSnapshot(): QuickLookData.Battery? {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = context.registerReceiver(null, filter) ?: return null
        return parseIntentToBattery(sticky)
    }
}
