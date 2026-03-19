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

package com.android.axion.widgets.cardlab.pedometer

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.data.PedometerData
import com.android.axion.widgets.provider.AodState
import com.android.axion.widgets.provider.PedometerProvider
import java.text.NumberFormat
import kotlinx.coroutines.flow.StateFlow

class AxPedometerReceiver : AxionWidgetProvider() {

    override fun requiredProviders() = listOf(PedometerProvider::class)

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val data =
            PedometerProvider.get(context).let { provider ->
                (provider.dataFlow as? StateFlow)?.value
            }
        val goal = PedometerPrefs.getGoal(context, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, data, goal))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { PedometerPrefs.removeWidget(context, it) }
    }

    companion object {
        private val numberFormat = NumberFormat.getNumberInstance()

        fun update(context: Context, data: PedometerData?) {
            doForAllWidgets(context, AxPedometerReceiver::class.java) { widgetId ->
                val goal = PedometerPrefs.getGoal(context, widgetId)
                val views = buildViews(context, data, goal)
                AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
            }
        }

        fun updateWidget(context: Context, widgetId: Int) {
            val data =
                PedometerProvider.get(context).let { provider ->
                    (provider.dataFlow as? StateFlow)?.value
                }
            val goal = PedometerPrefs.getGoal(context, widgetId)
            val views = buildViews(context, data, goal)
            AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
        }

        private fun buildViews(context: Context, data: PedometerData?, goal: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_pedometer)
            val steps = data?.steps ?: 0
            val aod = AodState.isAod

            val progressBitmap =
                StepProgressRenderer.render(
                    context,
                    steps,
                    goal,
                    R.color.battery_device_primary_color,
                    aod,
                )
            views.setImageViewBitmap(R.id.pedometer_progress, progressBitmap)

            val figureBitmap =
                StepProgressRenderer.renderWalkingFigure(
                    context,
                    steps,
                    R.color.battery_device_primary_color,
                    aod,
                )
            views.setImageViewBitmap(R.id.pedometer_walking_figure, figureBitmap)

            views.setTextViewText(R.id.pedometer_steps, numberFormat.format(steps))
            views.setTextViewText(R.id.pedometer_goal, "/ ${numberFormat.format(goal)}")

            if (aod) {
                views.setInt(
                    R.id.pedometer_root,
                    "setBackgroundResource",
                    R.drawable.bg_widget_card_aod,
                )
                views.setTextColor(R.id.pedometer_steps, Color.WHITE)
                views.setTextColor(R.id.pedometer_steps_label, Color.WHITE)
                views.setTextColor(R.id.pedometer_goal, Color.WHITE)
            } else {
                views.setInt(R.id.pedometer_root, "setBackgroundResource", R.color.battery_bg_color)
                val textColor = context.getColor(R.color.battery_device_primary_color)
                views.setTextColor(R.id.pedometer_steps, textColor)
                views.setTextColor(R.id.pedometer_steps_label, textColor)
                views.setTextColor(R.id.pedometer_goal, textColor)
            }
            return views
        }
    }
}
