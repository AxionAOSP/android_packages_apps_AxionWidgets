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
import com.android.axion.platform.AxPlatformFeature
import com.android.axion.widgets.R

object TileIcons {

    private data class IconPair(val active: Int, val inactive: Int)

    private val iconMap =
        mapOf(
            AxPlatformFeature.WIFI to IconPair(R.drawable.ic_wifi_on, R.drawable.ic_wifi_off),
            AxPlatformFeature.MOBILE_DATA to
                IconPair(R.drawable.ic_mobile_data_on, R.drawable.ic_mobile_data_off),
            AxPlatformFeature.BLUETOOTH to
                IconPair(R.drawable.ic_bluetooth_on, R.drawable.ic_bluetooth_off),
            AxPlatformFeature.AIRPLANE_MODE to
                IconPair(R.drawable.ic_airplane_on, R.drawable.ic_airplane_off),
            AxPlatformFeature.DARK_MODE to
                IconPair(R.drawable.ic_dark_theme_on, R.drawable.ic_dark_theme_off),
            AxPlatformFeature.FLASHLIGHT to
                IconPair(R.drawable.ic_torch_on, R.drawable.ic_torch_off),
            AxPlatformFeature.ZEN to IconPair(R.drawable.ic_dnd_on, R.drawable.ic_dnd_off),
            AxPlatformFeature.ROTATION to
                IconPair(R.drawable.ic_auto_rotate_on, R.drawable.ic_auto_rotate_off),
            AxPlatformFeature.HOTSPOT to
                IconPair(R.drawable.ic_hotspot_on, R.drawable.ic_hotspot_off),
            AxPlatformFeature.LOCATION to
                IconPair(R.drawable.ic_location_on, R.drawable.ic_location_off),
            AxPlatformFeature.BATTERY_SAVER to
                IconPair(R.drawable.ic_battery_saver_on, R.drawable.ic_battery_saver_off),
            AxPlatformFeature.COLOR_INVERSION to
                IconPair(R.drawable.ic_color_inversion_on, R.drawable.ic_color_inversion_off),
            AxPlatformFeature.COLOR_CORRECTION to
                IconPair(R.drawable.ic_color_correction_on, R.drawable.ic_color_correction_off),
            AxPlatformFeature.NIGHT_LIGHT to
                IconPair(R.drawable.ic_night_light_on, R.drawable.ic_night_light_off),
            AxPlatformFeature.NFC to IconPair(R.drawable.ic_nfc_on, R.drawable.ic_nfc_off),
            AxPlatformFeature.CAST to IconPair(R.drawable.ic_cast_on, R.drawable.ic_cast_off),
            AxPlatformFeature.DATA_SAVER to
                IconPair(R.drawable.ic_data_saver_on, R.drawable.ic_data_saver_off),
            AxPlatformFeature.REDUCE_BRIGHTNESS to
                IconPair(R.drawable.ic_extra_dim_on, R.drawable.ic_extra_dim_off),
            AxPlatformFeature.AOD to IconPair(R.drawable.ic_aod_on, R.drawable.ic_aod_off),
            AxPlatformFeature.AMBIENT_DISPLAY to
                IconPair(R.drawable.ic_aod_on, R.drawable.ic_aod_off),
            AxPlatformFeature.SCREEN_RECORD to
                IconPair(R.drawable.ic_screenrecord, R.drawable.ic_screenrecord),
            AxPlatformFeature.SCREENSHOT to
                IconPair(R.drawable.ic_screenshot, R.drawable.ic_screenshot),
            AxPlatformFeature.WORK_PROFILE to
                IconPair(R.drawable.ic_work_on, R.drawable.ic_work_off),
            AxPlatformFeature.CAFFEINE to
                IconPair(R.drawable.ic_caffeine_on, R.drawable.ic_caffeine_off),
            AxPlatformFeature.HEADS_UP to
                IconPair(R.drawable.ic_heads_up_on, R.drawable.ic_heads_up_off),
            AxPlatformFeature.READING_MODE to
                IconPair(R.drawable.ic_reading_mode_on, R.drawable.ic_reading_mode_off),
            AxPlatformFeature.ALARM to IconPair(R.drawable.ic_alarm, R.drawable.ic_alarm),
        )

    fun getIcon(spec: String, active: Boolean): Int = getIcon(spec, active, null)

    fun getIcon(spec: String, active: Boolean, ringerMode: Int?): Int {
        if (isRingerSpec(spec)) return getRingerIcon(active, ringerMode)
        val pair = iconMap[resolve(spec)]
        return if (active) pair?.active ?: R.drawable.ic_unknown
        else pair?.inactive ?: R.drawable.ic_unknown
    }

    fun hasIcon(spec: String): Boolean = isRingerSpec(spec) || iconMap.containsKey(resolve(spec))

    fun isRingerSpec(spec: String): Boolean = resolve(spec) == AxPlatformFeature.RINGER_MODE

    private fun resolve(spec: String): String? = AxPlatformFeature.resolve(spec)

    private fun getRingerIcon(active: Boolean, ringerMode: Int?): Int =
        when (ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> R.drawable.ic_ringer_on
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_ringer_vibrate
            AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_ringer_silent
            else -> if (active) R.drawable.ic_ringer_on else R.drawable.ic_ringer_off
        }
}
