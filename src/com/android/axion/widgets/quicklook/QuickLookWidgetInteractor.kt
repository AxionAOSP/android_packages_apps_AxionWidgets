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
package com.android.axion.widgets.quicklook

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.widgets.R
import com.android.axion.widgets.data.*
import com.android.axion.widgets.manager.QuickLookDataManager
import com.android.axion.widgets.utils.*
import java.text.SimpleDateFormat
import java.util.*

class QuickLookWidgetInteractor(private val context: Context) {

    private val dateFormat =
        SimpleDateFormat(context.getString(R.string.date_format_pattern), Locale.getDefault())

    private fun setDisplayData(views: RemoteViews, data: DisplayData) {
        views.setTextOrHide(R.id.date_text, data.dateText)
        views.setTextOrHide(R.id.primary_text_info, data.primaryText)
        views.setTextOrHide(R.id.secondary_text_info, data.secondaryText)
        val iconIds = listOf(R.id.media_icon, R.id.secondary_icon, R.id.weather_icon)
        iconIds.forEach { views.setViewVisibility(it, View.GONE) }
        if (data.iconViewId != null && data.iconBitmap != null) {
            views.setIconOrHide(data.iconViewId, data.iconBitmap)
        }
    }

    fun updateRemoteViews(qlData: QuickLookData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quicklook)

        val displayData = when (qlData) {
                is QuickLookData.CalendarEvent -> {
                    val title = qlData.title
                        ?: context.getString(R.string.quick_look_widget_calendar_no_title)
                    val desc = CalendarUtils.getCalendarDescription(context, qlData)
                    DisplayData(
                        dateText = dateFormat.format(Date()),
                        primaryText = title,
                        secondaryText = desc
                    )
                }

                is QuickLookData.Media -> {
                    val mediaTxt = context.getString(
                        R.string.media_format,
                        qlData.title,
                        qlData.artist
                    ).takeIf { it.isNotBlank() } ?: ""
                    if (mediaTxt.isNotEmpty()) {
                        val pm = context.packageManager
                        val iconBitmap = try {
                            pm.getApplicationIcon(qlData.packageName ?: "").toBitmap()
                        } catch (e: Exception) {
                            null
                        }
                        val b = iconBitmap ?: ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_music_note
                        )?.toBitmap()
                        DisplayData(
                            dateText = dateFormat.format(Date()),
                            primaryText = "",
                            secondaryText = mediaTxt,
                            iconViewId = R.id.media_icon,
                            iconBitmap = b
                        )
                    } else {
                        DisplayData(
                            dateText = dateFormat.format(Date())
                        )
                    }
                }

                is QuickLookData.Battery -> {
                    val iconResId = R.drawable.ic_battery_charging
                    val chargingStatus = context.getString(R.string.charging)
                    val chargingTime = qlData.chargingTimeRemaining?.let {
                        val minutes = (it + 59999) / 60000
                        context.getString(R.string.minutes_left, minutes)
                    } ?: ""
                    val secondary = if (qlData.isCharging && chargingTime.isNotEmpty()) {
                        context.getString(
                            R.string.charging_with_time,
                            chargingStatus,
                            chargingTime
                        )
                    } else {
                        chargingStatus
                    }
                    DisplayData(
                        dateText = dateFormat.format(Date()),
                        primaryText = context.getString(
                            R.string.battery_level_format,
                            qlData.level
                        ),
                        secondaryText = secondary,
                        iconViewId = R.id.secondary_icon,
                        iconBitmap = ContextCompat.getDrawable(context, iconResId)?.toBitmap()
                    )
                }

                is QuickLookData.Weather -> {
                    val icon =
                        WeatherUtils.getWeatherIcon(context, qlData.conditionCode)?.toBitmap()
                    DisplayData(
                        dateText = dateFormat.format(Date()),
                        primaryText = "${qlData.temp}°",
                        secondaryText = qlData.condition ?: "",
                        iconViewId = R.id.weather_icon,
                        iconBitmap = icon
                    )
                }

                else -> {
                    DisplayData(
                        dateText = dateFormat.format(Date()),
                        primaryText = context.getString(R.string.placeholder),
                        secondaryText = ""
                    )
                }
            }

        setDisplayData(views, displayData)

        val pendingIntent = QuickLookActions.getClickPendingIntent(context, qlData)
        views.setOnClickPendingIntent(R.id.secondary_info, pendingIntent)

        val dateClickIntent = QuickLookActions.getDateClickPendingIntent(context)
        if (dateClickIntent != null) {
            views.setOnClickPendingIntent(R.id.date_text, dateClickIntent)
        }

        return views
    }
}
