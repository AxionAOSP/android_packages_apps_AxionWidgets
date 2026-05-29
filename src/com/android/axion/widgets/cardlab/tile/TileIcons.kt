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

import android.media.AudioManager
import com.android.axion.widgets.R

object TileIcons {

    private data class IconPair(val active: Int, val inactive: Int)

    private val ringerSpecs = setOf("ringer", "ringer_mode", "sound", "sound_mode")

    private val iconMap =
        mapOf(
            "wifi" to IconPair(R.drawable.ic_wifi_on, R.drawable.ic_wifi_off),
            "internet" to IconPair(R.drawable.ic_wifi_on, R.drawable.ic_wifi_off),
            "bt" to IconPair(R.drawable.ic_bluetooth_on, R.drawable.ic_bluetooth_off),
            "bluetooth" to IconPair(R.drawable.ic_bluetooth_on, R.drawable.ic_bluetooth_off),
            "airplane" to IconPair(R.drawable.ic_airplane_on, R.drawable.ic_airplane_off),
            "cell" to IconPair(R.drawable.ic_mobile_data_on, R.drawable.ic_mobile_data_off),
            "mobiledata" to IconPair(R.drawable.ic_mobile_data_on, R.drawable.ic_mobile_data_off),
            "dark" to IconPair(R.drawable.ic_dark_theme_on, R.drawable.ic_dark_theme_off),
            "ui_mode_night" to IconPair(R.drawable.ic_dark_theme_on, R.drawable.ic_dark_theme_off),
            "flashlight" to IconPair(R.drawable.ic_torch_on, R.drawable.ic_torch_off),
            "dnd" to IconPair(R.drawable.ic_dnd_on, R.drawable.ic_dnd_off),
            "rotation" to IconPair(R.drawable.ic_auto_rotate_on, R.drawable.ic_auto_rotate_off),
            "hotspot" to IconPair(R.drawable.ic_hotspot_on, R.drawable.ic_hotspot_off),
            "location" to IconPair(R.drawable.ic_location_on, R.drawable.ic_location_off),
            "battery" to IconPair(R.drawable.ic_battery_saver_on, R.drawable.ic_battery_saver_off),
            "saver" to IconPair(R.drawable.ic_battery_saver_on, R.drawable.ic_battery_saver_off),
            "inversion" to
                IconPair(R.drawable.ic_color_inversion_on, R.drawable.ic_color_inversion_off),
            "color_correction" to
                IconPair(R.drawable.ic_color_correction_on, R.drawable.ic_color_correction_off),
            "night" to IconPair(R.drawable.ic_night_light_on, R.drawable.ic_night_light_off),
            "nfc" to IconPair(R.drawable.ic_nfc_on, R.drawable.ic_nfc_off),
            "cast" to IconPair(R.drawable.ic_cast_on, R.drawable.ic_cast_off),
            "data_saver" to IconPair(R.drawable.ic_data_saver_on, R.drawable.ic_data_saver_off),
            "reduce_brightness" to
                IconPair(R.drawable.ic_extra_dim_on, R.drawable.ic_extra_dim_off),
            "extra_dim" to IconPair(R.drawable.ic_extra_dim_on, R.drawable.ic_extra_dim_off),
            "aod" to IconPair(R.drawable.ic_aod_on, R.drawable.ic_aod_off),
            "ambient_display" to IconPair(R.drawable.ic_aod_on, R.drawable.ic_aod_off),
            "screenrecord" to IconPair(R.drawable.ic_screenrecord, R.drawable.ic_screenrecord),
            "screenshot" to IconPair(R.drawable.ic_screenshot, R.drawable.ic_screenshot),
            "work" to IconPair(R.drawable.ic_work_on, R.drawable.ic_work_off),
            "caffeine" to IconPair(R.drawable.ic_caffeine_on, R.drawable.ic_caffeine_off),
            "heads_up" to IconPair(R.drawable.ic_heads_up_on, R.drawable.ic_heads_up_off),
            "reading_mode" to
                IconPair(R.drawable.ic_reading_mode_on, R.drawable.ic_reading_mode_off),
        )

    fun getIcon(spec: String, active: Boolean): Int {
        return getIcon(spec, active, null)
    }

    fun getIcon(spec: String, active: Boolean, ringerMode: Int?): Int {
        val normalizedSpec = spec.lowercase()
        if (isRingerSpec(normalizedSpec)) {
            return getRingerIcon(active, ringerMode)
        }
        val pair = iconMap.entries.firstOrNull { (key, _) -> normalizedSpec.contains(key) }?.value
        return if (active) pair?.active ?: R.drawable.ic_unknown
        else pair?.inactive ?: R.drawable.ic_unknown
    }

    fun hasIcon(spec: String): Boolean {
        val normalizedSpec = spec.lowercase()
        return isRingerSpec(normalizedSpec) || iconMap.keys.any { normalizedSpec.contains(it) }
    }

    fun isRingerSpec(spec: String): Boolean {
        val normalizedSpec = spec.lowercase()
        return ringerSpecs.any { normalizedSpec.contains(it) }
    }

    private fun getRingerIcon(active: Boolean, ringerMode: Int?): Int =
        when (ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> R.drawable.ic_ringer_on
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_ringer_vibrate
            AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_ringer_silent
            else -> if (active) R.drawable.ic_ringer_on else R.drawable.ic_ringer_off
        }
}
