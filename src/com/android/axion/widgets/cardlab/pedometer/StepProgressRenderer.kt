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

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import androidx.core.content.ContextCompat

object StepProgressRenderer {

    fun render(
        context: Context,
        steps: Int,
        goal: Int,
        primaryColorRes: Int,
        aod: Boolean = false,
    ): Bitmap {
        val sizeDp = 160
        val px =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeDp.toFloat(),
                    context.resources.displayMetrics,
                )
                .toInt()

        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val strokeWidth = px * 0.06f
        val padding = strokeWidth / 2f + px * 0.02f
        val rect = RectF(padding, padding, px - padding, px - padding)

        val primaryColor =
            if (aod) Color.WHITE else ContextCompat.getColor(context, primaryColorRes)

        val trackPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
                color = primaryColor
                alpha = 40
            }
        canvas.drawArc(rect, -90f, 360f, false, trackPaint)

        val progress = (steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
        val sweepAngle = progress * 360f
        val progressPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
                color = primaryColor
            }
        if (sweepAngle > 0f) {
            canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
        }

        return bitmap
    }

    fun renderWalkingFigure(
        context: Context,
        steps: Int,
        primaryColorRes: Int,
        aod: Boolean = false,
    ): Bitmap {
        val sizeDp = 36
        val px =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeDp.toFloat(),
                    context.resources.displayMetrics,
                )
                .toInt()

        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = if (aod) Color.WHITE else ContextCompat.getColor(context, primaryColorRes)
        val frame = (steps / 4) % 4

        drawWalkingFrame(canvas, px, color, frame)
        return bitmap
    }

    private fun drawWalkingFrame(canvas: Canvas, size: Int, color: Int, frame: Int) {
        val dotR = size * 0.045f
        val cx = size / 2f
        val unit = size * 0.07f

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }

        canvas.drawCircle(cx, unit * 1.8f, dotR * 1.8f, paint)

        canvas.drawCircle(cx, unit * 3.5f, dotR, paint)
        canvas.drawCircle(cx, unit * 5f, dotR, paint)
        canvas.drawCircle(cx, unit * 6.5f, dotR, paint)

        when (frame) {
            0 -> {

                canvas.drawCircle(cx - unit * 1.5f, unit * 4f, dotR, paint)
                canvas.drawCircle(cx - unit * 2.5f, unit * 3f, dotR, paint)

                canvas.drawCircle(cx + unit * 1.5f, unit * 5f, dotR, paint)
                canvas.drawCircle(cx + unit * 2.5f, unit * 5.5f, dotR, paint)

                canvas.drawCircle(cx - unit * 1f, unit * 8f, dotR, paint)
                canvas.drawCircle(cx - unit * 2f, unit * 9.5f, dotR, paint)
                canvas.drawCircle(cx - unit * 2.5f, unit * 11f, dotR, paint)

                canvas.drawCircle(cx + unit * 1f, unit * 8f, dotR, paint)
                canvas.drawCircle(cx + unit * 1.5f, unit * 9.5f, dotR, paint)
                canvas.drawCircle(cx + unit * 2f, unit * 11f, dotR, paint)
            }
            1 -> {

                canvas.drawCircle(cx - unit * 1.5f, unit * 4.5f, dotR, paint)
                canvas.drawCircle(cx - unit * 2f, unit * 3.5f, dotR, paint)

                canvas.drawCircle(cx + unit * 1.5f, unit * 4.5f, dotR, paint)
                canvas.drawCircle(cx + unit * 2f, unit * 5.5f, dotR, paint)

                canvas.drawCircle(cx - unit * 0.3f, unit * 8f, dotR, paint)
                canvas.drawCircle(cx - unit * 0.5f, unit * 9.5f, dotR, paint)
                canvas.drawCircle(cx - unit * 0.5f, unit * 11f, dotR, paint)
                canvas.drawCircle(cx + unit * 0.3f, unit * 8f, dotR, paint)
                canvas.drawCircle(cx + unit * 0.5f, unit * 9.5f, dotR, paint)
                canvas.drawCircle(cx + unit * 0.5f, unit * 11f, dotR, paint)
            }
            2 -> {

                canvas.drawCircle(cx - unit * 1.5f, unit * 5f, dotR, paint)
                canvas.drawCircle(cx - unit * 2.5f, unit * 5.5f, dotR, paint)

                canvas.drawCircle(cx + unit * 1.5f, unit * 4f, dotR, paint)
                canvas.drawCircle(cx + unit * 2.5f, unit * 3f, dotR, paint)

                canvas.drawCircle(cx - unit * 1f, unit * 8f, dotR, paint)
                canvas.drawCircle(cx - unit * 1.5f, unit * 9.5f, dotR, paint)
                canvas.drawCircle(cx - unit * 2f, unit * 11f, dotR, paint)

                canvas.drawCircle(cx + unit * 1f, unit * 8f, dotR, paint)
                canvas.drawCircle(cx + unit * 2f, unit * 9.5f, dotR, paint)
                canvas.drawCircle(cx + unit * 2.5f, unit * 11f, dotR, paint)
            }
            3 -> {

                canvas.drawCircle(cx - unit * 1.5f, unit * 4.5f, dotR, paint)
                canvas.drawCircle(cx - unit * 2f, unit * 5.5f, dotR, paint)

                canvas.drawCircle(cx + unit * 1.5f, unit * 4.5f, dotR, paint)
                canvas.drawCircle(cx + unit * 2f, unit * 3.5f, dotR, paint)

                canvas.drawCircle(cx - unit * 0.3f, unit * 8f, dotR, paint)
                canvas.drawCircle(cx - unit * 0.5f, unit * 9.5f, dotR, paint)
                canvas.drawCircle(cx - unit * 0.5f, unit * 11f, dotR, paint)
                canvas.drawCircle(cx + unit * 0.3f, unit * 8f, dotR, paint)
                canvas.drawCircle(cx + unit * 0.5f, unit * 9.5f, dotR, paint)
                canvas.drawCircle(cx + unit * 0.5f, unit * 11f, dotR, paint)
            }
        }
    }
}
