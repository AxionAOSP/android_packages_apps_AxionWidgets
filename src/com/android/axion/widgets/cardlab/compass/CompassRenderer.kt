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

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import androidx.core.content.ContextCompat

object CompassRenderer {

    fun render(
        context: Context,
        azimuthDeg: Float,
        primaryColorRes: Int,
        compact: Boolean = false,
        aod: Boolean = false,
    ): Bitmap {
        val sizeDp = if (compact) 120 else 200
        val px =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeDp.toFloat(),
                    context.resources.displayMetrics,
                )
                .toInt()

        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = px / 2f
        val cy = px / 2f
        val radius = px * 0.42f

        val primaryColor =
            if (aod) Color.WHITE else ContextCompat.getColor(context, primaryColorRes)

        canvas.save()
        canvas.rotate(-azimuthDeg, cx, cy)

        val tickPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                strokeCap = Paint.Cap.ROUND
            }

        val step = if (compact) 30 else 10
        for (i in 0 until 360 step step) {
            val isCardinal = i % 90 == 0
            val isMajor = i % 30 == 0
            tickPaint.strokeWidth =
                when {
                    isCardinal -> px * 0.02f
                    isMajor -> px * 0.012f
                    else -> px * 0.006f
                }
            tickPaint.alpha =
                when {
                    isCardinal -> 255
                    isMajor -> 180
                    else -> 100
                }
            val tickLen =
                when {
                    isCardinal -> px * 0.07f
                    isMajor -> px * 0.045f
                    else -> px * 0.025f
                }
            val angle = Math.toRadians(i.toDouble())
            val outerX = cx + radius * Math.sin(angle).toFloat()
            val outerY = cy - radius * Math.cos(angle).toFloat()
            val innerX = cx + (radius - tickLen) * Math.sin(angle).toFloat()
            val innerY = cy - (radius - tickLen) * Math.cos(angle).toFloat()
            canvas.drawLine(innerX, innerY, outerX, outerY, tickPaint)
        }

        if (!compact) {

            val labelPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    textAlign = Paint.Align.CENTER
                    textSize = px * 0.08f
                    typeface = Typeface.create("nothingdot57", Typeface.NORMAL)
                }

            val labels = arrayOf("N", "E", "S", "W")
            val angles = arrayOf(0, 90, 180, 270)
            val labelRadius = radius - px * 0.1f

            for (idx in labels.indices) {
                val angle = Math.toRadians(angles[idx].toDouble())
                val lx = cx + labelRadius * Math.sin(angle).toFloat()
                val ly = cy - labelRadius * Math.cos(angle).toFloat()

                if (labels[idx] == "N") {
                    labelPaint.color = Color.rgb(215, 25, 33)
                } else {
                    labelPaint.color = primaryColor
                }

                val fm = labelPaint.fontMetrics
                canvas.drawText(labels[idx], lx, ly - (fm.ascent + fm.descent) / 2, labelPaint)
            }
        } else {

            val northDotPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(215, 25, 33)
                    style = Paint.Style.FILL
                }
            val dotRadius = px * 0.03f
            val dotY = cy - radius + px * 0.06f
            canvas.drawCircle(cx, dotY, dotRadius, northDotPaint)
        }

        canvas.restore()

        val needlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                style = Paint.Style.FILL
            }
        val needleLen = radius * if (compact) 0.45f else 0.35f
        val needleWidth = px * if (compact) 0.03f else 0.025f
        val path =
            Path().apply {
                moveTo(cx, cy - needleLen)
                lineTo(cx - needleWidth, cy)
                lineTo(cx + needleWidth, cy)
                close()
            }
        canvas.drawPath(path, needlePaint)

        val dotPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                style = Paint.Style.FILL
            }
        canvas.drawCircle(cx, cy, px * 0.025f, dotPaint)

        if (!compact) {

            val degreePaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    textAlign = Paint.Align.CENTER
                    textSize = px * 0.065f
                    typeface = Typeface.create("nothingdot57", Typeface.NORMAL)
                }
            val direction = getCardinalDirection(azimuthDeg)
            val degreeText = "${azimuthDeg.toInt()}° $direction"
            canvas.drawText(degreeText, cx, cy + radius + px * 0.08f, degreePaint)
        }

        return bitmap
    }

    private fun getCardinalDirection(azimuth: Float): String {
        val normalized = ((azimuth % 360) + 360) % 360
        return when {
            normalized < 22.5f || normalized >= 337.5f -> "N"
            normalized < 67.5f -> "NE"
            normalized < 112.5f -> "E"
            normalized < 157.5f -> "SE"
            normalized < 202.5f -> "S"
            normalized < 247.5f -> "SW"
            normalized < 292.5f -> "W"
            else -> "NW"
        }
    }
}
