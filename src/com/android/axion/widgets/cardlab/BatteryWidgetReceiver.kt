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
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.view.View
import android.graphics.*
import android.util.TypedValue
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.provider.BatteryStatusProvider

class BatteryWidgetReceiver : AxionWidgetProvider() {

    override fun requiredProviders() = listOf(
        BatteryStatusProvider::class
    )

    companion object {
        fun update(context: Context, qldata: QuickLookData?) {
            AxionWidgetProvider.updateWidget(
                context,
                BatteryWidgetReceiver::class.java,
                qldata ?: QuickLookData.Empty
            ) { ctx, data ->
                AxionWidgetProvider.buildRemoteViews(ctx, R.layout.widget_battery, data) { d ->
                    if (d is QuickLookData.Battery) {
                        val batteryBg = createBatteryBg(ctx, d.level)

                        if (d.level <= 20) {
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

                        setTextViewText(R.id.battery_percentage, "${d.level}%")
                        setViewVisibility(R.id.battery_view_bottom_left, if (d.isCharging) View.VISIBLE else View.GONE)
                    } else {
                        setTextViewText(R.id.battery_percentage, "")
                        setViewVisibility(R.id.battery_view_bottom_left, View.INVISIBLE)
                    }

                    val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        ctx,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.battery_card_root, pendingIntent)
                }
            }
        }

        private fun createBatteryBg(ctx: Context, batteryLevel: Int): Bitmap {
            val sizeDp = 48
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                sizeDp.toFloat(),
                ctx.resources.displayMetrics
            ).toInt()

            return Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.GREEN
                }
                val rect = RectF(0f, 0f, px.toFloat(), px.toFloat())
                val sweepAngle = (batteryLevel / 100f) * 360f
                canvas.drawArc(rect, -90f, sweepAngle, true, paint)
            }
        }
    }
}
