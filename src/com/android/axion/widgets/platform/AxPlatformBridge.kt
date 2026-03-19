/*
 * Copyright (C) 2025-2026 AxionOS Project
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

package com.android.axion.widgets.platform

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.axion.platform.AxPlatformClient
import com.android.axion.platform.IAxPlatformCallback
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@Singleton
class AxPlatformBridge @Inject constructor(context: Context) {

    companion object {
        private const val TAG = "AxPlatformBridge"
    }

    private val client = AxPlatformClient.getInstance().also { it.init(context) }
    private val handler = Handler(Looper.getMainLooper())
    private val stateFlows = mutableMapOf<String, MutableSharedFlow<Bundle>>()
    private var registered = false

    private val callback =
        object : IAxPlatformCallback.Stub() {
            override fun onStateChanged(key: String, state: Bundle) {
                handler.post { getOrCreateFlow(key).tryEmit(state) }
            }
        }

    fun connect() {
        if (registered) return
        client.registerCallback(callback)
        registered = true
        Log.d(TAG, "Connected to AxPlatform service")
    }

    fun disconnect() {
        if (!registered) return
        client.unregisterCallback(callback)
        registered = false
    }

    fun stateFlow(key: String): Flow<Bundle> {
        connect()
        return getOrCreateFlow(key).onStart {
            val cached = client.getState(key)
            if (!cached.isEmpty) emit(cached)
        }
    }

    fun getState(key: String): Bundle = client.getState(key)

    fun toggle(feature: String) = client.toggle(feature)

    fun setEnabled(feature: String, enabled: Boolean) = client.setEnabled(feature, enabled)

    fun setValue(feature: String, value: Int) = client.setValue(feature, value)

    fun performAction(feature: String, param: String) = client.performAction(feature, param)

    fun connectWifi(key: String) = client.connectWifi(key)

    fun connectBluetoothDevice(address: String) = client.connectBluetoothDevice(address)

    fun getSupportedFeatures(): Array<String> = client.getSupportedFeatures()

    fun isFeatureActive(feature: String): Boolean = client.isFeatureActive(feature)

    fun isFeatureAvailable(feature: String): Boolean = client.isFeatureAvailable(feature)

    val isDarkMode: Flow<Boolean> =
        stateFlow(AxPlatformClient.KEY_CONFIG)
            .map { it.getBoolean("isDarkMode", false) }
            .distinctUntilChanged()

    val orientation: Flow<Int> =
        stateFlow(AxPlatformClient.KEY_CONFIG)
            .map { it.getInt("orientation", 1) }
            .distinctUntilChanged()

    val fontScale: Flow<Float> =
        stateFlow(AxPlatformClient.KEY_CONFIG)
            .map { it.getFloat("fontScale", 1.0f) }
            .distinctUntilChanged()

    val isKeyguardShowing: Flow<Boolean> =
        stateFlow(AxPlatformClient.KEY_KEYGUARD)
            .map { it.getBoolean("isShowing", false) }
            .distinctUntilChanged()

    val isKeyguardGoingAway: Flow<Boolean> =
        stateFlow(AxPlatformClient.KEY_KEYGUARD)
            .map { it.getBoolean("isGoingAway", false) }
            .distinctUntilChanged()

    val isDeviceUnlocked: Flow<Boolean> =
        stateFlow(AxPlatformClient.KEY_KEYGUARD)
            .map { it.getBoolean("isUnlocked", false) }
            .distinctUntilChanged()

    val isDozing: Flow<Boolean> =
        stateFlow(AxPlatformClient.KEY_DOZE)
            .map { it.getBoolean("isDozing", false) }
            .distinctUntilChanged()

    val dozeAmount: Flow<Float> =
        stateFlow(AxPlatformClient.KEY_DOZE)
            .map { it.getFloat("dozeAmount", 0f) }
            .distinctUntilChanged()

    val isAodEnabled: Flow<Boolean> =
        stateFlow(AxPlatformClient.KEY_DOZE)
            .map { it.getBoolean("aodEnabled", false) }
            .distinctUntilChanged()

    val batteryLevel: Flow<Int> =
        stateFlow(AxPlatformClient.KEY_BATTERY)
            .map { it.getInt("level", -1) }
            .distinctUntilChanged()

    val isBatteryCharging: Flow<Boolean> =
        stateFlow(AxPlatformClient.KEY_BATTERY)
            .map { it.getBoolean("isCharging", false) }
            .distinctUntilChanged()

    val isPowerSave: Flow<Boolean> =
        stateFlow(AxPlatformClient.KEY_BATTERY)
            .map { it.getBoolean("powerSave", false) }
            .distinctUntilChanged()

    val isMediaPlaying: Flow<Boolean> =
        stateFlow(AxPlatformClient.KEY_MEDIA)
            .map { it.getBoolean("isPlaying", false) }
            .distinctUntilChanged()

    fun featureActive(feature: String): Flow<Boolean> =
        stateFlow(feature).map { it.getBoolean("active", false) }.distinctUntilChanged()

    private fun getOrCreateFlow(key: String): MutableSharedFlow<Bundle> {
        return stateFlows.getOrPut(key) {
            MutableSharedFlow(
                replay = 1,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
    }
}
