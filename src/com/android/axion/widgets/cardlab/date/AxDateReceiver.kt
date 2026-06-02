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

package com.android.axion.widgets.cardlab.date

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.provider.CalendarContract
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.WidgetUpdateService
import com.android.axion.widgets.provider.AodState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class AxDateReceiver : AxionWidgetProvider() {

    override fun refresh(context: Context, service: WidgetUpdateService) = update(context)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        update(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> update(context)
        }
    }

    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

        fun update(context: Context) {
            val event = queryNextEvent(context)
            val views = buildViews(context, event)
            updateAllWidgets(context, AxDateReceiver::class.java, views)
        }

        private fun buildViews(context: Context, event: NextEvent?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_date)
            val today = LocalDate.now()

            views.setTextViewText(R.id.date_day_number, today.dayOfMonth.toString())
            views.setTextViewText(
                R.id.date_day_of_week,
                today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            )
            views.setTextViewText(
                R.id.date_month_year,
                "%s %d"
                    .format(
                        today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        today.year,
                    ),
            )

            if (event != null) {
                views.setTextViewText(R.id.date_event_title, event.title)
                val eventTime =
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(event.startTime),
                        ZoneId.systemDefault(),
                    )
                views.setTextViewText(R.id.date_event_time, eventTime.format(timeFormatter))
            } else {
                views.setTextViewText(R.id.date_event_title, "No upcoming events")
                views.setTextViewText(R.id.date_event_time, "")
            }

            val calendarIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    data =
                        ContentUris.withAppendedId(
                            CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build(),
                            System.currentTimeMillis(),
                        )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    calendarIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            views.setOnClickPendingIntent(R.id.date_root, pendingIntent)

            if (AodState.isAod) {
                views.setInt(R.id.date_root, "setBackgroundResource", R.drawable.bg_widget_card_aod)
                views.setTextColor(R.id.date_day_number, Color.WHITE)
                views.setTextColor(R.id.date_day_of_week, Color.WHITE)
                views.setTextColor(R.id.date_month_year, Color.WHITE)
                views.setTextColor(R.id.date_event_title, Color.WHITE)
                views.setTextColor(R.id.date_event_time, Color.WHITE)
                views.setInt(R.id.date_divider, "setBackgroundColor", Color.WHITE)
            } else {
                views.setInt(R.id.date_root, "setBackgroundResource", R.color.battery_bg_color)
                val textColor = context.getColor(R.color.battery_device_primary_color)
                views.setTextColor(R.id.date_day_number, textColor)
                views.setTextColor(R.id.date_day_of_week, textColor)
                views.setTextColor(R.id.date_month_year, textColor)
                views.setTextColor(R.id.date_event_title, textColor)
                views.setTextColor(R.id.date_event_time, textColor)
                views.setInt(R.id.date_divider, "setBackgroundColor", textColor)
            }

            return views
        }

        private fun queryNextEvent(context: Context): NextEvent? {
            val now = System.currentTimeMillis()
            val endOfDay =
                LocalDate.now()
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

            val projection =
                arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                )
            val selection =
                "${CalendarContract.Events.DTSTART} >= ? AND " +
                    "${CalendarContract.Events.DTSTART} <= ? AND " +
                    "${CalendarContract.Events.VISIBLE} = 1"
            val selectionArgs = arrayOf(now.toString(), endOfDay.toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT 1"

            return runCatching {
                    context.contentResolver
                        .query(
                            CalendarContract.Events.CONTENT_URI,
                            projection,
                            selection,
                            selectionArgs,
                            sortOrder,
                        )
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                NextEvent(
                                    title = cursor.getString(1) ?: "",
                                    startTime = cursor.getLong(2),
                                )
                            } else null
                        }
                }
                .getOrNull()
        }
    }

    private data class NextEvent(val title: String, val startTime: Long)
}
