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

package com.android.axion.widgets.cardlab.countdown

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.WidgetUpdateService
import com.android.axion.widgets.provider.AodState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class AxCountdownReceiver : AxionWidgetProvider() {

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val config = CountdownPrefs.get(context, appWidgetId) ?: return
        val days = computeDaysRemaining(config.targetDateMillis)
        appWidgetManager.updateAppWidget(
            appWidgetId,
            buildViews(context, config.eventName, days, config.targetDateMillis),
        )
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { CountdownPrefs.remove(context, it) }
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

        fun update(context: Context) {
            doForAllWidgets(context, AxCountdownReceiver::class.java) { widgetId ->
                val config = CountdownPrefs.get(context, widgetId) ?: return@doForAllWidgets
                val days = computeDaysRemaining(config.targetDateMillis)
                val views = buildViews(context, config.eventName, days, config.targetDateMillis)
                AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
            }
        }

        fun updateWidget(context: Context, widgetId: Int, config: CountdownConfig) {
            val days = computeDaysRemaining(config.targetDateMillis)
            val views = buildViews(context, config.eventName, days, config.targetDateMillis)
            AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
        }

        private fun buildViews(
            context: Context,
            eventName: String,
            daysRemaining: Long,
            targetDateMillis: Long,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)
            views.setTextViewText(R.id.countdown_days, daysRemaining.toString())
            views.setTextViewText(R.id.countdown_label, if (daysRemaining == 1L) "day" else "days")
            views.setTextViewText(R.id.countdown_event_name, eventName)
            val targetDate =
                Instant.ofEpochMilli(targetDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            views.setTextViewText(R.id.countdown_target_date, targetDate.format(dateFormatter))

            if (AodState.isAod) {
                views.setInt(
                    R.id.countdown_root,
                    "setBackgroundResource",
                    R.drawable.bg_widget_card_aod,
                )
                views.setTextColor(R.id.countdown_days, Color.WHITE)
                views.setTextColor(R.id.countdown_label, Color.WHITE)
                views.setTextColor(R.id.countdown_event_name, Color.WHITE)
                views.setTextColor(R.id.countdown_target_date, Color.WHITE)
            } else {
                views.setInt(R.id.countdown_root, "setBackgroundResource", R.color.battery_bg_color)
                val textColor = context.getColor(R.color.battery_device_primary_color)
                views.setTextColor(R.id.countdown_days, textColor)
                views.setTextColor(R.id.countdown_label, textColor)
                views.setTextColor(R.id.countdown_event_name, textColor)
                views.setTextColor(R.id.countdown_target_date, textColor)
            }
            return views
        }

        private fun computeDaysRemaining(targetMillis: Long): Long {
            val today = LocalDate.now()
            val target =
                Instant.ofEpochMilli(targetMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            return ChronoUnit.DAYS.between(today, target).coerceAtLeast(0)
        }
    }
}
