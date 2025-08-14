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
import android.telephony.TelephonyManager
import com.android.axion.widgets.R

class TileConfigs(private val context: Context) {

    private val wifiManager by lazy { context.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val btAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val uiModeManager by lazy { context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager }
    private val telephonyManager by lazy { context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

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
                context.getString(R.string.wifi),
                { wifiManager.isWifiEnabled },
                {
                    wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                    wifiManager.isWifiEnabled
                },
                context,
                labelProvider = {
                    if (wifiManager.isWifiEnabled) {
                        wifiManager.connectionInfo.ssid.removePrefix("\"").removeSuffix("\"")
                    } else {
                        context.getString(R.string.wifi)
                    }
                },
                spec = "wifi"
            )
        )

        tiles.add(
            TileConfig.from(
                context.getString(R.string.bluetooth),
                { btAdapter?.isEnabled == true },
                {
                    if (btAdapter?.isEnabled == true) btAdapter.disable() else btAdapter?.enable()
                    btAdapter?.isEnabled == true
                },
                context,
                labelProvider = {
                    if (btAdapter?.isEnabled == true) {
                        btAdapter.bondedDevices.joinToString(", ") { it.name }
                    } else {
                        context.getString(R.string.bluetooth)
                    }
                },
                spec = "bluetooth"
            )
        )

        tiles.add(
            TileConfig.from(
                context.getString(R.string.airplane_mode),
                { Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1 },
                {
                    val state = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 1
                    Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (state) 1 else 0)
                    context.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply { putExtra("state", state) })
                    state
                },
                context,
                spec = "airplane"
            )
        )

        tiles.add(
            TileConfig.from(
                context.getString(R.string.dark_theme),
                { uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES },
                {
                    uiModeManager.nightMode = if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES)
                        UiModeManager.MODE_NIGHT_NO else UiModeManager.MODE_NIGHT_YES
                    uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
                },
                context,
                spec = "dark_theme"
            )
        )

        tiles.add(
            TileConfig.from(
                context.getString(R.string.torch),
                { isTorchActive() },
                { toggleTorch() },
                context,
                spec = "torch"
            )
        )

        tiles.add(
            TileConfig.from(
                context.getString(R.string.dnd),
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
                context,
                labelProvider = {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    when (notificationManager.currentInterruptionFilter) {
                        NotificationManager.INTERRUPTION_FILTER_ALL -> context.getString(R.string.dnd)
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> context.getString(R.string.priority)
                        NotificationManager.INTERRUPTION_FILTER_ALARMS -> context.getString(R.string.alarms_only)
                        NotificationManager.INTERRUPTION_FILTER_NONE -> context.getString(R.string.total_silence)
                        else -> context.getString(R.string.dnd)
                    }
                },
                spec = "dnd"
            )
        )

        tiles.add(
            TileConfig.from(
                context.getString(R.string.auto_rotate),
                { Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1 },
                {
                    val newState = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) != 1
                    Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (newState) 1 else 0)
                    newState
                },
                context,
                spec = "auto_rotate"
            )
        )

        tiles.add(
            TileConfig.from(
                context.getString(R.string.ringer_normal),
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
                context,
                labelProvider = {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    when (audioManager.ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> context.getString(R.string.ringer_normal)
                        AudioManager.RINGER_MODE_VIBRATE -> context.getString(R.string.ringer_vibrate)
                        AudioManager.RINGER_MODE_SILENT -> context.getString(R.string.ringer_silent)
                        else -> context.getString(R.string.ringer_unknown)
                    }
                },
                spec = "ringer"
            )
        )

        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            tiles.add(
                TileConfig.from(
                    context.getString(R.string.mobile_data),
                    { telephonyManager.isDataEnabled },
                    {
                        try { telephonyManager.setDataEnabled(!telephonyManager.isDataEnabled) } catch (_: Exception) {}
                        telephonyManager.isDataEnabled
                    },
                    context,
                    labelProvider = {
                        if (!telephonyManager.isDataEnabled) {
                            context.getString(R.string.mobile_data)
                        } else {
                            telephonyManager.networkOperatorName ?: context.getString(R.string.mobile_data)
                        }
                    },
                    spec = "mobile_data"
                )
            )
        }

        tiles
    }
}
