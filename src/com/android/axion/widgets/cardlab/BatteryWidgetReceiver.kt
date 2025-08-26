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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.android.axion.widgets.R
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.manager.BatteryWidgetManager

class BatteryWidgetReceiver : AppWidgetProvider(), BatteryWidgetManager.Callback {

    private lateinit var batteryWidgetManager: BatteryWidgetManager
    private lateinit var appContext: Context

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        init(context)
    }

    override fun onDisabled(context: Context) {
        if (::batteryWidgetManager.isInitialized) {
            batteryWidgetManager.removeListener(this)
        }
    }

    private fun init(context: Context) {
        if (::batteryWidgetManager.isInitialized) return
        appContext = context.applicationContext
        batteryWidgetManager = BatteryWidgetManager.get(context)
        batteryWidgetManager.addListener(this)
    }

    override fun onBatteryUpdated(data: QuickLookData.Battery?) {
        updateWidget(appContext, data)
    }

    private fun updateWidget(context: Context, data: QuickLookData.Battery?) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, BatteryWidgetReceiver::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_battery).apply {
                setOnClickPendingIntent(R.id.battery_card_root, pendingIntent)

                if (data != null) {
                    val batteryBg = createBatteryBg(context, data.level)
                    if (data.level <= 20) {
                        setViewVisibility(R.id.battery_bg_view_low, View.VISIBLE)
                        setImageViewBitmap(R.id.battery_bg_view_low, batteryBg)
                        setViewVisibility(R.id.battery_bg_view, View.GONE)
                        setImageViewBitmap(R.id.battery_bg_view, null)
                    } else {
                        setViewVisibility(R.id.battery_bg_view_low, View.GONE)
                        setImageViewBitmap(R.id.battery_bg_view_low, null)
                        setViewVisibility(R.id.battery_bg_view, View.VISIBLE)
                        setImageViewBitmap(R.id.battery_bg_view, batteryBg)
                    }

                    setViewVisibility(R.id.battery_percentage, View.VISIBLE)
                    setTextViewText(R.id.battery_percentage, "${data.level}%")
                    setViewVisibility(
                        R.id.battery_view_bottom_left,
                        if (data.isCharging) View.VISIBLE else View.GONE
                    )
                } else {
                    setTextViewText(R.id.battery_percentage, "100%")
                    setViewVisibility(R.id.battery_view_bottom_left, View.INVISIBLE)
                }
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

        return Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                isDither = true
            }
            val rect = RectF(0f, 0f, px.toFloat(), px.toFloat())
            val sweepAngle = (batteryLevel / 100f) * 360f
            canvas.drawArc(rect, -90f, sweepAngle, true, paint)
        }
    }
}
