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

package com.android.axion.widgets.cardlab.clock

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.WidgetUpdateService
import com.android.axion.widgets.provider.AodState

class AxWorldClockReceiver : AxionWidgetProvider() {

    override fun refresh(context: Context, service: WidgetUpdateService) = update(context)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        update(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val config = WorldClockPrefs.get(context, appWidgetId) ?: return
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, config))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WorldClockPrefs.remove(context, it) }
    }

    companion object {
        fun update(context: Context) {
            doForAllWidgets(context, AxWorldClockReceiver::class.java) { widgetId ->
                val config = WorldClockPrefs.get(context, widgetId) ?: return@doForAllWidgets
                AppWidgetManager.getInstance(context)
                    .updateAppWidget(widgetId, buildViews(context, config))
            }
        }

        fun updateWidget(context: Context, widgetId: Int, config: WorldClockConfig) {
            AppWidgetManager.getInstance(context)
                .updateAppWidget(widgetId, buildViews(context, config))
        }

        private fun buildViews(context: Context, config: WorldClockConfig): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_world_clock)
            views.setTextViewText(R.id.world_clock_city, config.cityName.uppercase())
            views.setString(R.id.world_clock_time, "setTimeZone", config.timezoneId)

            if (AodState.isAod) {
                views.setInt(
                    R.id.world_clock_root,
                    "setBackgroundResource",
                    R.drawable.bg_widget_card_aod,
                )
                views.setTextColor(R.id.world_clock_city, Color.WHITE)
                views.setTextColor(R.id.world_clock_time, Color.WHITE)
            } else {
                views.setInt(
                    R.id.world_clock_root,
                    "setBackgroundResource",
                    R.color.battery_bg_color,
                )
                views.setTextColor(
                    R.id.world_clock_city,
                    context.getColor(R.color.battery_device_primary_color),
                )
                views.setTextColor(
                    R.id.world_clock_time,
                    context.getColor(R.color.battery_device_primary_color),
                )
            }
            return views
        }
    }
}
