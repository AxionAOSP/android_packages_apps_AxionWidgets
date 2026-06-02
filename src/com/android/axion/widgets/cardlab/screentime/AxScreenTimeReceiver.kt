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

package com.android.axion.widgets.cardlab.screentime

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.TypedValue
import android.view.View
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.WidgetUpdateService
import com.android.axion.widgets.data.UsageData
import com.android.axion.widgets.provider.AodState
import com.android.axion.widgets.provider.UsageStatsProvider

class AxScreenTimeReceiver : AxionWidgetProvider() {

    override fun refresh(context: Context, service: WidgetUpdateService) =
        update(context, null, true)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "TOGGLE_EXPRESSION") {
            isShowingExpression = !isShowingExpression
            update(context, lastUsageData)
        }
    }

    override fun requiredProviders() = listOf(UsageStatsProvider::class)

    companion object {
        private var isShowingExpression = true
        private var lastUsageData: UsageData? = null

        fun update(context: Context, udata: UsageData?, reset: Boolean = false) {
            lastUsageData = udata
            if (reset) isShowingExpression = true
            AxionWidgetProvider.updateWidget(
                context,
                AxScreenTimeReceiver::class.java,
                udata ?: UsageData(),
            ) { ctx, data ->
                AxionWidgetProvider.buildRemoteViews(ctx, R.layout.widget_screentime, data) { d ->
                    val aod = AodState.isAod
                    if (aod) {
                        setInt(
                            R.id.screen_time_container,
                            "setBackgroundResource",
                            R.drawable.bg_widget_card_aod,
                        )
                    } else {
                        setInt(
                            R.id.screen_time_container,
                            "setBackgroundResource",
                            R.drawable.bg_widget_card,
                        )
                    }

                    if (d is UsageData) {

                        val hours = ((d.totalTimeForeground / (1000 * 60 * 60)) % 24).toInt()
                        val minutes = ((d.totalTimeForeground / (1000 * 60)) % 60).toInt()

                        val expressionRes =
                            when {
                                hours >= 8 -> R.drawable.icon_screen_time_overtime_expression
                                hours >= 2 -> R.drawable.icon_screen_time_expression
                                else -> R.drawable.icon_screen_time_aod_expression_smile
                            }

                        if (isShowingExpression) {
                            setViewVisibility(R.id.text_container, View.GONE)
                            setViewVisibility(R.id.screen_time_meter, View.GONE)
                            setViewVisibility(R.id.iv_expression, View.VISIBLE)
                            setImageViewResource(R.id.iv_expression, expressionRes)
                            val iconTint =
                                if (aod) Color.WHITE
                                else ctx.getColor(R.color.battery_device_primary_color)
                            setInt(R.id.iv_expression, "setColorFilter", iconTint)
                        } else {
                            setViewVisibility(R.id.text_container, View.VISIBLE)
                            setViewVisibility(R.id.screen_time_meter, View.VISIBLE)
                            setViewVisibility(R.id.iv_expression, View.GONE)

                            val dotMeter = createDotMatrixMeter(ctx, d.level.coerceAtMost(100))
                            setImageViewBitmap(R.id.screen_time_meter, dotMeter)

                            setTextViewText(R.id.text_hours_number, hours.toString())
                            setTextViewText(R.id.text_minutes_number, minutes.toString())

                            val meterTint =
                                if (aod) Color.WHITE
                                else ctx.getColor(R.color.battery_device_primary_color)
                            setInt(R.id.screen_time_meter, "setColorFilter", meterTint)

                            val textColor =
                                if (aod) Color.WHITE
                                else ctx.getColor(R.color.battery_device_primary_color)
                            setTextColor(R.id.text_hours_number, textColor)
                            setTextColor(R.id.text_hours_label, textColor)
                            setTextColor(R.id.text_minutes_number, textColor)
                            setTextColor(R.id.text_minutes_label, textColor)
                        }
                    } else {
                        setViewVisibility(R.id.text_container, View.GONE)
                        setViewVisibility(R.id.screen_time_meter, View.VISIBLE)
                        setViewVisibility(R.id.iv_expression, View.GONE)
                    }

                    val toggleIntent =
                        Intent(ctx, AxScreenTimeReceiver::class.java).apply {
                            action = "TOGGLE_EXPRESSION"
                        }
                    val togglePending =
                        PendingIntent.getBroadcast(
                            ctx,
                            0,
                            toggleIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    setOnClickPendingIntent(R.id.screen_time_container, togglePending)
                }
            }
        }

        private fun createDotMatrixMeter(
            ctx: Context,
            progress: Int,
            rows: Int = 3,
            cols: Int = 20,
            dotSizeDp: Int = 8,
        ): Bitmap {
            val dotSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dotSizeDp.toFloat(),
                        ctx.resources.displayMetrics,
                    )
                    .toInt()

            val bitmapWidth = cols * dotSizePx * 2
            val bitmapHeight = rows * dotSizePx * 2

            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

            val totalDots = rows * cols
            val filledDots = (progress / 100f * totalDots).toInt()

            var counter = 0
            for (c in 0 until cols) {
                for (r in (rows - 1) downTo 0) {
                    paint.color = if (counter < filledDots) Color.WHITE else 0x66FFFFFF

                    val cx = (c * 2 + 1) * dotSizePx
                    val cy = (r * 2 + 1) * dotSizePx
                    canvas.drawCircle(cx.toFloat(), cy.toFloat(), dotSizePx.toFloat(), paint)

                    counter++
                }
            }

            return bitmap
        }
    }
}
