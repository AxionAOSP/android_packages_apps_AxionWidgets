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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.View
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.WidgetUpdateService
import com.android.axion.widgets.provider.AodState
import java.time.LocalDate
import java.time.YearMonth

class AxYearProgressReceiver : AxionWidgetProvider() {

    override fun refresh(context: Context, service: WidgetUpdateService) = update(context)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        update(context)
    }

    companion object {
        fun update(context: Context) {
            val now = LocalDate.now()
            AxionWidgetProvider.updateWidget(context, AxYearProgressReceiver::class.java, now) {
                ctx,
                date ->
                AxionWidgetProvider.buildRemoteViews(ctx, R.layout.widget_year_progress, date) { d
                    ->
                    if (AodState.isAod) {
                        setInt(
                            R.id.year_progress_container,
                            "setBackgroundResource",
                            R.drawable.bg_widget_card_aod,
                        )
                        setTextColor(R.id.year_progress_label, Color.WHITE)
                    } else {
                        setInt(
                            R.id.year_progress_container,
                            "setBackgroundResource",
                            R.drawable.bg_widget_card,
                        )
                        setTextColor(
                            R.id.year_progress_label,
                            ctx.getColor(R.color.battery_device_primary_color),
                        )
                    }

                    val bitmap = createYearProgressBitmap(ctx, d)
                    setImageViewBitmap(R.id.year_progress_view, bitmap)
                    setViewVisibility(R.id.year_progress_preview_placeholder, View.GONE)

                    val dayOfYear = d.dayOfYear
                    val totalDays = d.lengthOfYear()
                    val labelText = "YEAR PROGRESS - $dayOfYear/$totalDays DAYS"
                    setTextViewText(R.id.year_progress_label, labelText)
                }
            }
        }

        private fun createYearProgressBitmap(ctx: Context, today: LocalDate): Bitmap {
            val res = ctx.resources
            val dm = res.displayMetrics

            val scale = 3f

            val dotSizeDp = 3.5f
            val gapDp = 3f
            val paddingDp = 4f
            val labelWidthDp = 22f

            val dotSize =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dotSizeDp, dm) * scale
            val gap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, gapDp, dm) * scale
            val padding =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, paddingDp, dm) * scale
            val labelWidth =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, labelWidthDp, dm) * scale

            val cols = 31
            val rows = 12

            val width = (labelWidth + cols * (dotSize + gap) - gap + 2 * padding).toInt()
            val height = (rows * (dotSize + gap) - gap + 2 * padding).toInt()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val year = today.year

            val textPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha = 180
                    textSize = 7f * dm.density * scale
                    textAlign = Paint.Align.LEFT
                    typeface = Typeface.create("nothingdot", Typeface.NORMAL)
                }

            val months =
                arrayOf(
                    "JAN",
                    "FEB",
                    "MAR",
                    "APR",
                    "MAY",
                    "JUN",
                    "JUL",
                    "AUG",
                    "SEP",
                    "OCT",
                    "NOV",
                    "DEC",
                )

            for (month in 1..12) {
                val labelY =
                    padding +
                        (month - 1) * (dotSize + gap) +
                        dotSize / 2f +
                        (textPaint.textSize / 3f)
                canvas.drawText(months[month - 1], padding, labelY, textPaint)

                val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
                for (day in 1..31) {
                    val cx = padding + labelWidth + (day - 1) * (dotSize + gap) + dotSize / 2f
                    val cy = padding + (month - 1) * (dotSize + gap) + dotSize / 2f

                    if (day > daysInMonth) continue

                    val date = LocalDate.of(year, month, day)
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE

                    when {
                        date.isBefore(today) -> {
                            paint.alpha = 255
                            canvas.drawCircle(cx, cy, dotSize / 2f, paint)
                        }
                        date.isEqual(today) -> {
                            paint.alpha = 255
                            canvas.drawCircle(cx, cy, dotSize / 2f, paint)

                            val holePaint =
                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                                }
                            canvas.drawCircle(cx, cy, dotSize / 4f, holePaint)
                        }
                        else -> {
                            paint.alpha = 100
                            canvas.drawCircle(cx, cy, dotSize / 2f, paint)
                        }
                    }
                }
            }
            return bitmap
        }
    }
}
