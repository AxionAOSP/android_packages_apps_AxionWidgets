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

import android.app.PendingIntent
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

class AxDigitalClockReceiver : AxionWidgetProvider() {

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
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context))
    }

    companion object {
        fun update(context: Context) {
            updateAllWidgets(context, AxDigitalClockReceiver::class.java, buildViews(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_digital_clock)

            if (AodState.isAod) {
                views.setInt(
                    R.id.digital_clock_root,
                    "setBackgroundResource",
                    R.drawable.bg_widget_card_aod,
                )
                views.setTextColor(R.id.digital_clock_time, Color.WHITE)
                views.setTextColor(R.id.digital_clock_date, Color.WHITE)
            } else {
                views.setInt(
                    R.id.digital_clock_root,
                    "setBackgroundResource",
                    R.color.battery_bg_color,
                )
                views.setTextColor(
                    R.id.digital_clock_time,
                    context.getColor(R.color.battery_device_primary_color),
                )
                views.setTextColor(
                    R.id.digital_clock_date,
                    context.getColor(R.color.battery_device_primary_color),
                )
            }

            val intent =
                Intent("android.intent.action.SHOW_ALARMS").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            val pi =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            views.setOnClickPendingIntent(R.id.digital_clock_root, pi)

            return views
        }
    }
}
