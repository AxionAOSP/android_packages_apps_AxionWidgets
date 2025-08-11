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
package com.android.axion.widgets.cardlab

import android.appwidget.*
import android.content.*
import android.graphics.*
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.android.axion.widgets.R
import com.android.axion.widgets.data.*
import com.android.axion.widgets.manager.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class BatteryWidgetReceiver : AppWidgetProvider() {

    private var listening = false
    private val coroutineScope = MainScope()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        init(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        cleanup()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cleanup()
    }

    fun init(context: Context) {
        if (listening) return
        listening = true
        BatteryDataManager.batteryFlow(context)
            .onEach { batteryData ->
                updateWidget(context, batteryData)
            }
            .launchIn(coroutineScope)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
            coroutineScope.launch {
                BatteryDataManager.refresh()
            }
        }
    }

    private fun updateWidget(context: Context, data: QuickLookData.Battery?) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, BatteryWidgetReceiver::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_battery)
            if (data != null) {
                val batteryBg = createBatteryBg(context, data.level)
                views.setImageViewBitmap(R.id.battery_bg_view, batteryBg)
                views.setViewVisibility(R.id.battery_percentage, View.VISIBLE)
                if (data.isCharging) {
                    views.setViewVisibility(R.id.battery_view_bottom_left, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.battery_view_bottom_left, View.INVISIBLE)
                }
                views.setTextViewText(R.id.battery_percentage, "${data.level}%")
            } else {
                views.setTextViewText(R.id.battery_percentage, "100%")
                views.setViewVisibility(R.id.battery_view_bottom_left, View.INVISIBLE)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createBatteryBg(context: Context, batteryLevel: Int): Bitmap {
        val sizeDp = 48
        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            sizeDp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            isDither = true
            color = if (batteryLevel <= 20) {
                context.getColor(R.color.battery_progressbar_color_low_battery_state)
            } else {
                context.getColor(R.color.battery_progressbar_color)
            }
        }
        val rect = RectF(0f, 0f, px.toFloat(), px.toFloat())
        val sweepAngle = (batteryLevel / 100f) * 360f
        canvas.drawArc(rect, -90f, sweepAngle, true, paint)
        return bitmap
    }

    fun cleanup() {
        if (!listening) return
        listening = false
        coroutineScope.cancel()
    }
}
