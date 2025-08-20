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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.*

@Singleton
class QuickLookWidgetInteractor @Inject constructor(
    @ApplicationContext internal val context: Context,
    private val dataManager: QuickLookDataManager
) {

    private val dateFormat =
        SimpleDateFormat(context.getString(R.string.date_format_pattern), Locale.getDefault())

    private fun setDisplayData(views: RemoteViews, data: DisplayData) {
        views.setTextOrHide(R.id.date_text, data.dateText)
        views.setTextOrHide(R.id.primary_text_info, data.primaryText)
        views.setTextOrHide(R.id.secondary_text_info, data.secondaryText)

        val iconIds = listOf(R.id.media_icon, R.id.secondary_icon, R.id.weather_icon)
        iconIds.forEach { views.setViewVisibility(it, View.GONE) }

        data.iconViewId?.let { id ->
            data.iconBitmap?.let { bitmap ->
                views.setIconOrHide(id, bitmap)
            }
        }
    }

    fun updateRemoteViews(): RemoteViews {
        val qlData = dataManager.getQuickLookData()
        val views = RemoteViews(context.packageName, R.layout.widget_quicklook)

        val displayData = when (qlData) {
            is QuickLookData.CalendarEvent -> {
                val title = qlData.title ?: context.getString(R.string.quick_look_widget_calendar_no_title)
                val desc = CalendarUtils.getCalendarDescription(context, qlData)
                DisplayData(dateText = dateFormat.format(Date()), primaryText = title, secondaryText = desc)
            }

            is QuickLookData.Media -> {
                val mediaTxt = context.getString(R.string.media_format, qlData.title, qlData.artist)
                    .takeIf { it.isNotBlank() } ?: ""
                if (mediaTxt.isNotEmpty()) {
                    val pm = context.packageManager
                    val iconBitmap = try { pm.getApplicationIcon(qlData.packageName ?: "").toBitmap() } catch (e: Exception) { null }
                    val b = iconBitmap ?: ContextCompat.getDrawable(context, R.drawable.ic_music_note)?.toBitmap()
                    DisplayData(dateText = dateFormat.format(Date()), primaryText = "", secondaryText = mediaTxt, iconViewId = R.id.media_icon, iconBitmap = b)
                } else DisplayData(dateText = dateFormat.format(Date()))
            }

            is QuickLookData.Battery -> {
                val iconBitmap = ContextCompat.getDrawable(context, R.drawable.ic_battery_charging)?.toBitmap()
                val isFull = qlData.level == 100 && qlData.isCharging
                val chargingStatus = context.getString(if (isFull) R.string.full_charge else R.string.charging)
                val chargingTime = qlData.chargingTimeRemaining?.let { (it + 59999) / 60000 }
                    ?.let { context.getString(R.string.minutes_left, it) }

                val secondaryText = when {
                    !isFull && qlData.isCharging && !chargingTime.isNullOrEmpty() ->
                        context.getString(R.string.charging_with_time, chargingStatus, chargingTime)
                    else -> chargingStatus
                }

                DisplayData(
                    dateText = dateFormat.format(Date()),
                    primaryText = context.getString(R.string.battery_level_format, qlData.level),
                    secondaryText = secondaryText,
                    iconViewId = R.id.secondary_icon,
                    iconBitmap = iconBitmap
                )
            }

            is QuickLookData.Weather -> {
                val icon = WeatherUtils.getWeatherIcon(context, qlData.conditionCode)?.toBitmap()
                DisplayData(dateText = dateFormat.format(Date()), primaryText = "${qlData.temp}°", secondaryText = qlData.condition ?: "", iconViewId = R.id.weather_icon, iconBitmap = icon)
            }

            else -> DisplayData(dateText = dateFormat.format(Date()), primaryText = context.getString(R.string.placeholder), secondaryText = "")
        }

        setDisplayData(views, displayData)

        views.setOnClickPendingIntent(R.id.secondary_info, QuickLookActions.getClickPendingIntent(context, qlData))
        QuickLookActions.getDateClickPendingIntent(context)?.let { views.setOnClickPendingIntent(R.id.date_text, it) }

        return views
    }
}
