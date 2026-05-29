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

package com.android.axion.widgets.data

import android.graphics.Bitmap
import android.net.Uri

sealed class QuickLookData {
    data class Weather(
        val temp: String?,
        val condition: String?,
        val conditionCode: Int,
        val iconBytes: ByteArray? = null,
    ) : QuickLookData()

    data class Media(
        val title: String?,
        val artist: String?,
        val packageName: String?,
        val active: Boolean,
    ) : QuickLookData()

    data class CalendarEvent(
        val id: Long,
        val title: String,
        val startTime: Long,
        val endTime: Long,
        val location: String,
        val desc: String,
    ) : QuickLookData()

    data class Battery(
        val isCharging: Boolean,
        val level: Int,
        val chargingTimeRemaining: Long? = null,
    ) : QuickLookData()

    data class Message(val text: String) : QuickLookData()

    object Empty : QuickLookData()
}

data class DisplayData(
    val dateText: String? = null,
    val primaryText: String? = null,
    val secondaryText: String? = null,
    val iconViewId: Int? = null,
    val iconBitmap: Bitmap? = null,
)

data class TileData(
    val spec: String,
    val isActive: Boolean,
    val iconRes: Int,
    val widgetId: Int,
    val label: String? = null,
    val secondaryLabel: String? = null,
    val ringerMode: Int? = null,
    val hasVibrator: Boolean = true,
)

data class UsageData(
    val totalTimeForeground: Long = 0L,
    val formatted: String = "",
    val level: Int = -1,
)

data class PhotoWidgetData(
    val widgetId: Int,
    val bitmap: Bitmap?,
    val uris: List<Uri>,
    val grayscale: Boolean,
)

typealias PhotoWidgetDataList = List<PhotoWidgetData>

typealias TilesData = Map<Int, TileData>

data class MediaPlayerData(
    val title: String?,
    val artist: String?,
    val albumArt: Bitmap?,
    val isPlaying: Boolean,
    val duration: Long,
    val position: Long,
    val packageName: String?,
)

data class PedometerData(val steps: Int)

data class CompassData(val azimuth: Float)

typealias WeatherData = QuickLookData.Weather

typealias MediaData = QuickLookData.Media

typealias CalendarData = QuickLookData.CalendarEvent

typealias BatteryData = QuickLookData.Battery
