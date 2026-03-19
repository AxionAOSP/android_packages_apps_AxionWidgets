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
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.manager.QuickLookDataManager
import com.android.axion.widgets.provider.AodState
import com.android.axion.widgets.provider.BatteryStatusProvider

class AxBatteryReceiver : AxionWidgetProvider() {

    override fun requiredProviders() = listOf(BatteryStatusProvider::class)

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val data = QuickLookDataManager.get(context).batteryData ?: return
        val views = buildViews(context, appWidgetId, data)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun update(context: Context, qldata: QuickLookData?) {
            val data = qldata ?: QuickLookData.Empty
            AxionWidgetProvider.doForAllWidgets(context, AxBatteryReceiver::class.java) { widgetId
                ->
                val views = buildViews(context, widgetId, data)
                AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
            }
        }

        private fun buildViews(context: Context, widgetId: Int, d: QuickLookData): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_battery)
            val aod = AodState.isAod

            views.setInt(
                R.id.battery_card_root,
                "setBackgroundResource",
                if (aod) R.drawable.bg_widget_card_aod else R.drawable.bg_widget_card,
            )

            val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)

            if (minW > 0 && minH > 0) {
                val baseSize = minOf(minW, minH).toFloat()

                val padPx =
                    TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            baseSize * 0.1f,
                            context.resources.displayMetrics,
                        )
                        .toInt()
                views.setViewPadding(R.id.battery_card_root, padPx, padPx, padPx, padPx)

                views.setViewLayoutMargin(
                    R.id.battery_container,
                    5,
                    baseSize * 0.24f,
                    TypedValue.COMPLEX_UNIT_DIP,
                )
                views.setViewLayoutMargin(
                    R.id.battery_container,
                    3,
                    baseSize * 0.19f,
                    TypedValue.COMPLEX_UNIT_DIP,
                )
            }

            if (aod) {
                views.setTextColor(R.id.battery_percentage, Color.WHITE)
            } else {
                views.setTextColor(
                    R.id.battery_percentage,
                    context.getColor(R.color.battery_device_primary_color),
                )
            }

            if (d is QuickLookData.Battery) {
                val batteryBg = createBatteryBg(context, d.level, aod)

                if (d.level <= 20) {
                    views.setViewVisibility(R.id.battery_bg_view_low, View.VISIBLE)
                    views.setImageViewBitmap(R.id.battery_bg_view_low, batteryBg)
                    views.setViewVisibility(R.id.battery_bg_view, View.GONE)
                    views.setImageViewBitmap(R.id.battery_bg_view, null)
                } else {
                    views.setViewVisibility(R.id.battery_bg_view_low, View.GONE)
                    views.setImageViewBitmap(R.id.battery_bg_view_low, null)
                    views.setViewVisibility(R.id.battery_bg_view, View.VISIBLE)
                    views.setImageViewBitmap(R.id.battery_bg_view, batteryBg)
                }

                views.setTextViewText(R.id.battery_percentage, "${d.level}%")
                views.setViewVisibility(
                    R.id.battery_view_bottom_left,
                    if (!aod && d.isCharging) View.VISIBLE else View.GONE,
                )
            } else {
                views.setTextViewText(R.id.battery_percentage, "")
                views.setViewVisibility(R.id.battery_view_bottom_left, View.INVISIBLE)
            }

            val intent =
                Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            views.setOnClickPendingIntent(R.id.battery_card_root, pendingIntent)

            return views
        }

        private fun createBatteryBg(ctx: Context, batteryLevel: Int, aod: Boolean = false): Bitmap {
            val sizeDp = 128
            val px =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        sizeDp.toFloat(),
                        ctx.resources.displayMetrics,
                    )
                    .toInt()

            return Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                val paint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = if (aod) Color.WHITE else Color.GREEN
                        if (aod) alpha = 80
                    }
                val rect = RectF(0f, 0f, px.toFloat(), px.toFloat())
                val sweepAngle = (batteryLevel / 100f) * 360f
                canvas.drawArc(rect, -90f, sweepAngle, true, paint)
            }
        }
    }
}
