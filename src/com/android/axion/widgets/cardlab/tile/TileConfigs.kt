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
import android.provider.Settings.Global.AIRPLANE_MODE_ON
import android.provider.Settings.System.ACCELEROMETER_ROTATION
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.android.axion.widgets.R
import com.android.axion.widgets.data.*
import com.android.axion.widgets.utils.SafeCloseable
import com.android.axion.widgets.utils.Tracker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileConfigs @Inject constructor(private val ctx: Context) {

    private val wm by lazy { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val bt by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val um by lazy { ctx.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager }
    private val tm by lazy { ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    private val cm by lazy { ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val nm by lazy { ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val am by lazy { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private val subId: Int get() = SubscriptionManager.getDefaultDataSubscriptionId()
    private val subTm get() = tm.createForSubscriptionId(subId)
    private val validSub get() = subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private val torchStates = mutableMapOf<String, Boolean>()
    
    private val resolver = ctx.contentResolver

    val DND_ALL = NotificationManager.INTERRUPTION_FILTER_ALL
    val dndModes = listOf(
        DndMode(DND_ALL, R.drawable.ic_dnd_off, R.string.dnd),
        DndMode(NotificationManager.INTERRUPTION_FILTER_PRIORITY, R.drawable.ic_dnd_on, R.string.priority),
        DndMode(NotificationManager.INTERRUPTION_FILTER_ALARMS, R.drawable.ic_alarm, R.string.alarms_only),
        DndMode(NotificationManager.INTERRUPTION_FILTER_NONE, R.drawable.ic_dnd_total_silence, R.string.total_silence)
    )

    val ringerModes = listOf(
        RingerModeInfo(AudioManager.RINGER_MODE_NORMAL, R.drawable.ic_ringer_off, string(R.string.ringer_normal)),
        RingerModeInfo(AudioManager.RINGER_MODE_VIBRATE, R.drawable.ic_ringer_vibrate, string(R.string.ringer_vibrate)),
        RingerModeInfo(AudioManager.RINGER_MODE_SILENT, R.drawable.ic_ringer_silent, string(R.string.ringer_silent))
    )

    init {
        TorchCallback(cm) { cameraId, enabled ->
            torchStates[cameraId] = enabled
        }
    }

    private fun isTorchActive() = torchStates.values.any { it }

    private fun toggleTorch(): Boolean {
        val cameraId = cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        val newState = !(torchStates[cameraId] ?: false)
        cm.setTorchMode(cameraId, newState)
        torchStates[cameraId] = newState
        return newState
    }

    private fun string(resId: Int) = ctx.getString(resId)
    private fun putGlobal(key: String, value: Int) = Settings.Global.putInt(resolver, key, value)
    private fun putSystem(key: String, value: Int) = Settings.System.putInt(resolver, key, value)
    private fun getGlobal(key: String, def: Int): Int = Settings.Global.getInt(resolver, key, def)
    private fun getSystem(key: String, def: Int): Int = Settings.System.getInt(resolver, key, def)
    private fun <T> nextMode(current: T, modes: List<T>): T {
        val index = modes.indexOfFirst { it == current }
        return modes[(index + 1) % modes.size]
    }

    val tilesRegistry: List<TileConfig> by lazy {
        val tiles = mutableListOf<TileConfig>()

        tiles += TileConfig.from(
            type = "Wifi",
            getter = { wm.isWifiEnabled },
            setter = { wm.isWifiEnabled = !wm.isWifiEnabled; wm.isWifiEnabled },
            iconActive = R.drawable.ic_wifi_on,
            iconInactive = R.drawable.ic_wifi_off,
            labelProvider = { wm.connectionInfo.ssid.trim('"') },
            spec = "wifi"
        )

        tiles += TileConfig.from(
            type = "Bluetooth",
            getter = { bt?.isEnabled == true },
            setter = { if (bt?.isEnabled == true) bt.disable() else bt?.enable(); bt?.isEnabled == true },
            iconActive = R.drawable.ic_bluetooth_on,
            iconInactive = R.drawable.ic_bluetooth_off,
            labelProvider = { bt?.bondedDevices?.joinToString(", ") { it.name } ?: string(R.string.bluetooth) },
            spec = "bluetooth"
        )

        tiles += TileConfig.from(
            type = "Airplane",
            getter = { getGlobal(AIRPLANE_MODE_ON, 0) == 1 },
            setter = {
                val current = getGlobal(AIRPLANE_MODE_ON, 0) == 1
                val newState = !current
                putGlobal(AIRPLANE_MODE_ON, if (newState) 1 else 0)
                ctx.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply { putExtra("state", newState) })
                newState
            },
            iconActive = R.drawable.ic_airplane_on,
            iconInactive = R.drawable.ic_airplane_off,
            spec = "airplane"
        )

        tiles += TileConfig.from(
            type = "Dark Theme",
            getter = { um.nightMode == UiModeManager.MODE_NIGHT_YES },
            setter = {
                um.nightMode = if (um.nightMode == UiModeManager.MODE_NIGHT_YES) UiModeManager.MODE_NIGHT_NO
                    else UiModeManager.MODE_NIGHT_YES
                um.nightMode == UiModeManager.MODE_NIGHT_YES
            },
            iconActive = R.drawable.ic_dark_theme_on,
            iconInactive = R.drawable.ic_dark_theme_off,
            spec = "dark_theme"
        )

        tiles += TileConfig.from(
            type = "Torch",
            getter = { isTorchActive() },
            setter = { toggleTorch() },
            iconActive = R.drawable.ic_torch_on,
            iconInactive = R.drawable.ic_torch_off,
            spec = "torch"
        )

        tiles += TileConfig.from(
            type = "DND",
            getter = { nm.currentInterruptionFilter != DND_ALL },
            setter = {
                val next = nextMode(dndModes.first { it.filter == nm.currentInterruptionFilter }, dndModes)
                nm.setInterruptionFilter(next.filter)
                next.filter != DND_ALL
            },
            iconProvider = { dndModes.first { it.filter == nm.currentInterruptionFilter }.iconRes },
            labelProvider = { string(dndModes.first { it.filter == nm.currentInterruptionFilter }.labelRes) },
            spec = "dnd"
        )

        tiles += TileConfig.from(
            type = "Auto Rotate",
            getter = { getSystem(ACCELEROMETER_ROTATION, 0) == 1 },
            setter = {
                val enabled = getSystem(ACCELEROMETER_ROTATION, 0) == 1
                val newState = !enabled
                putSystem(ACCELEROMETER_ROTATION, if (newState) 1 else 0)
                newState
            },
            iconActive = R.drawable.ic_auto_rotate_on,
            iconInactive = R.drawable.ic_auto_rotate_off,
            spec = "auto_rotate"
        )

        tiles += TileConfig.from(
            type = "Ringer",
            getter = { am.ringerMode != AudioManager.RINGER_MODE_NORMAL },
            setter = {
                val next = nextMode(ringerModes.first { it.mode == am.ringerMode }, ringerModes)
                am.ringerModeInternal = next.mode
                next.mode != AudioManager.RINGER_MODE_NORMAL
            },
            iconProvider = { ringerModes.first { it.mode == am.ringerMode }.icon },
            labelProvider = { ringerModes.first { it.mode == am.ringerMode }.label },
            spec = "ringer"
        )

        if (ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            tiles += TileConfig.from(
                type = "Mobile Data",
                getter = { validSub && subTm.isDataEnabled },
                setter = {
                    if (validSub) runCatching { subTm.setDataEnabled(!subTm.isDataEnabled) }
                    validSub && subTm.isDataEnabled
                },
                iconActive = R.drawable.ic_mobile_data_on,
                iconInactive = R.drawable.ic_mobile_data_off,
                labelProvider = {
                    if (validSub)
                        SubscriptionManager.from(ctx).getActiveSubscriptionInfo(subId)?.carrierName?.toString()
                            ?: string(R.string.mobile_data)
                    else string(R.string.mobile_data)
                },
                spec = "mobile_data"
            )
        }
        tiles
    }
}

fun TileConfigs.createTileData(type: String, widgetId: Int): TileData {
    val tileConfig = tilesRegistry.firstOrNull { it.type == type }
    return TileData(
        type = type,
        isActive = tileConfig?.observeState?.invoke() ?: false,
        iconRes = tileConfig?.getIcon?.invoke(tileConfig.observeState()) ?: R.drawable.ic_unknown,
        widgetId = widgetId,
        label = tileConfig?.getLabel?.invoke()
    )
}

class TorchCallback(
    private val cm: CameraManager,
    private val onTorchChanged: (cameraId: String, enabled: Boolean) -> Unit
) : CameraManager.TorchCallback(), SafeCloseable {

    init {
        Tracker.get().addCloseable(this)
        cm.registerTorchCallback(this, null)
    }

    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
        onTorchChanged(cameraId, enabled)
    }

    override fun close() {
        cm.unregisterTorchCallback(this)
    }
}
