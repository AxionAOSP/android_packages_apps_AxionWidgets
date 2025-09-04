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

import android.app.UiModeManager
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.android.axion.widgets.R

class TileConfigs(private val context: Context) {

    private val wifiManager by lazy { context.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val btAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val uiModeManager by lazy { context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager }
    private val telephonyManager by lazy { context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private val defaultDataSubId: Int
        get() = SubscriptionManager.getDefaultDataSubscriptionId()

    private val torchStates = mutableMapOf<String, Boolean>()

    init {
        cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                torchStates[cameraId] = enabled
            }
        }, null)
    }

    private fun isTorchActive(): Boolean = torchStates.values.any { it }

    private fun toggleTorch(): Boolean {
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        val newState = !(torchStates[cameraId] ?: false)
        cameraManager.setTorchMode(cameraId, newState)
        torchStates[cameraId] = newState
        return newState
    }

    val tilesRegistry: List<TileConfig> by lazy {
        val tiles = mutableListOf<TileConfig>()

        tiles.add(
            TileConfig.from(
                getTileType(R.string.wifi),
                { wifiManager.isWifiEnabled },
                {
                    wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                    wifiManager.isWifiEnabled
                },
                iconProvider = { if (wifiManager.isWifiEnabled) R.drawable.ic_wifi_on else R.drawable.ic_wifi_off },
                labelProvider = {
                    if (wifiManager.isWifiEnabled) {
                        wifiManager.connectionInfo.ssid.removePrefix("\"").removeSuffix("\"")
                    } else {
                        getTileType(R.string.wifi)
                    }
                },
                spec = "wifi"
            )
        )

        tiles.add(
            TileConfig.from(
                getTileType(R.string.bluetooth),
                { btAdapter?.isEnabled == true },
                {
                    if (btAdapter?.isEnabled == true) btAdapter.disable() else btAdapter?.enable()
                    btAdapter?.isEnabled == true
                },
                iconProvider = { if (btAdapter?.isEnabled == true) R.drawable.ic_bluetooth_on else R.drawable.ic_bluetooth_off },
                labelProvider = {
                    if (btAdapter?.isEnabled == true) {
                        btAdapter.bondedDevices.joinToString(", ") { it.name }
                    } else {
                        getTileType(R.string.bluetooth)
                    }
                },
                spec = "bluetooth"
            )
        )

        tiles.add(
            TileConfig.from(
                getTileType(R.string.airplane_mode),
                { Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1 },
                {
                    val state = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 1
                    Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (state) 1 else 0)
                    context.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply { putExtra("state", state) })
                    state
                },
                iconProvider = { 
                    if (Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1)
                        R.drawable.ic_airplane_on else R.drawable.ic_airplane_off 
                },
                spec = "airplane"
            )
        )

        tiles.add(
            TileConfig.from(
                getTileType(R.string.dark_theme),
                { uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES },
                {
                    uiModeManager.nightMode = if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES)
                        UiModeManager.MODE_NIGHT_NO else UiModeManager.MODE_NIGHT_YES
                    uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
                },
                iconProvider = { if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES) R.drawable.ic_dark_theme_on else R.drawable.ic_dark_theme_off },
                spec = "dark_theme"
            )
        )

        tiles.add(
            TileConfig.from(
                getTileType(R.string.torch),
                { isTorchActive() },
                { toggleTorch() },
                iconProvider = { if (isTorchActive()) R.drawable.ic_torch_on else R.drawable.ic_torch_off },
                spec = "torch"
            )
        )

        tiles.add(
            TileConfig.from(
                getTileType(R.string.dnd),
                {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                },
                {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val nextFilter = when (notificationManager.currentInterruptionFilter) {
                        NotificationManager.INTERRUPTION_FILTER_ALL -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> NotificationManager.INTERRUPTION_FILTER_ALARMS
                        NotificationManager.INTERRUPTION_FILTER_ALARMS -> NotificationManager.INTERRUPTION_FILTER_NONE
                        NotificationManager.INTERRUPTION_FILTER_NONE -> NotificationManager.INTERRUPTION_FILTER_ALL
                        else -> NotificationManager.INTERRUPTION_FILTER_ALL
                    }
                    notificationManager.setInterruptionFilter(nextFilter)
                    nextFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                },
                iconProvider = { 
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    when (nm.currentInterruptionFilter) {
                        NotificationManager.INTERRUPTION_FILTER_ALL -> R.drawable.ic_dnd_off
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> R.drawable.ic_dnd_on
                        NotificationManager.INTERRUPTION_FILTER_ALARMS -> R.drawable.ic_alarm
                        NotificationManager.INTERRUPTION_FILTER_NONE -> R.drawable.ic_dnd_total_silence
                        else -> R.drawable.ic_dnd_off
                    }
                },
                labelProvider = {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    when (notificationManager.currentInterruptionFilter) {
                        NotificationManager.INTERRUPTION_FILTER_ALL -> getTileType(R.string.dnd)
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> getTileType(R.string.priority)
                        NotificationManager.INTERRUPTION_FILTER_ALARMS -> getTileType(R.string.alarms_only)
                        NotificationManager.INTERRUPTION_FILTER_NONE -> getTileType(R.string.total_silence)
                        else -> getTileType(R.string.dnd)
                    }
                },
                spec = "dnd"
            )
        )

        tiles.add(
            TileConfig.from(
                getTileType(R.string.auto_rotate),
                { Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1 },
                {
                    val newState = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) != 1
                    Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (newState) 1 else 0)
                    newState
                },
                iconProvider = { if (Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1) R.drawable.ic_auto_rotate_on else R.drawable.ic_auto_rotate_off },
                spec = "auto_rotate"
            )
        )

        tiles.add(
            TileConfig.from(
                getTileType(R.string.ringer_normal),
                {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
                },
                {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val nextMode = when (audioManager.ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                        AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                        AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_NORMAL
                        else -> AudioManager.RINGER_MODE_NORMAL
                    }
                    audioManager.ringerMode = nextMode
                    nextMode != AudioManager.RINGER_MODE_NORMAL
                },
                iconProvider = { 
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    when (audioManager.ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> R.drawable.ic_ringer_off
                        AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_ringer_vibrate
                        AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_ringer_silent
                        else -> R.drawable.ic_ringer_off
                    }
                },
                labelProvider = {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    when (audioManager.ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> getTileType(R.string.ringer_normal)
                        AudioManager.RINGER_MODE_VIBRATE -> getTileType(R.string.ringer_vibrate)
                        AudioManager.RINGER_MODE_SILENT -> getTileType(R.string.ringer_silent)
                        else -> getTileType(R.string.ringer_normal)
                    }
                },
                spec = "ringer"
            )
        )

        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            tiles.add(
                TileConfig.from(
                    getTileType(R.string.mobile_data),
                    {
                        val subId = defaultDataSubId
                        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                            telephonyManager.createForSubscriptionId(subId).isDataEnabled
                        } else false
                    },
                    {
                        val subId = defaultDataSubId
                        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                            val subTm = telephonyManager.createForSubscriptionId(subId)
                            try {
                                subTm.setDataEnabled(!subTm.isDataEnabled)
                            } catch (_: Exception) {}
                            subTm.isDataEnabled
                        } else false
                    },
                    iconProvider = { 
                        val subId = defaultDataSubId
                        val enabled = if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                            telephonyManager.createForSubscriptionId(subId).isDataEnabled
                        } else false
                        if (enabled) R.drawable.ic_mobile_data_on else R.drawable.ic_mobile_data_off
                    },
                    labelProvider = {
                        val subId = defaultDataSubId
                        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                            val subInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(subId)
                            subInfo?.carrierName?.toString() ?: getTileType(R.string.mobile_data)
                        } else {
                            getTileType(R.string.mobile_data)
                        }
                    },
                    spec = "mobile_data"
                )
            )
        }
        tiles
    }

    private fun getTileType(resId: Int): String {
        val config = context.resources.configuration
        val locale = java.util.Locale.ENGLISH
        val newConfig = android.content.res.Configuration(config)
        newConfig.setLocale(locale)
        return context.createConfigurationContext(newConfig)
            .resources
            .getString(resId)
    }
}
