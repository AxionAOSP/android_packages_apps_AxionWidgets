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

package com.android.axion.widgets.cardlab.compass

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.data.CompassData
import com.android.axion.widgets.provider.AodState
import com.android.axion.widgets.provider.CompassProvider

class AxCompassReceiver : AxionWidgetProvider() {

    override fun requiredProviders() = listOf(CompassProvider::class)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        update(context, null)
    }

    companion object {
        fun update(context: Context, data: CompassData?) {
            doForAllWidgets(context, AxCompassReceiver::class.java) { widgetId ->
                val views = buildViews(context, data)
                AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
            }
        }

        private fun buildViews(context: Context, data: CompassData?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_compass)
            val azimuth = data?.azimuth ?: 0f
            val aod = AodState.isAod

            if (aod) {
                views.setInt(
                    R.id.compass_root,
                    "setBackgroundResource",
                    R.drawable.bg_widget_card_aod,
                )
                views.setViewPadding(R.id.compass_root, 0, 0, 0, 0)
            } else {
                views.setInt(R.id.compass_root, "setBackgroundResource", R.drawable.bg_widget_card)
            }

            val bitmap =
                CompassRenderer.render(
                    context,
                    azimuth,
                    R.color.battery_device_primary_color,
                    false,
                    aod,
                )
            views.setImageViewBitmap(R.id.compass_image, bitmap)
            return views
        }
    }
}
