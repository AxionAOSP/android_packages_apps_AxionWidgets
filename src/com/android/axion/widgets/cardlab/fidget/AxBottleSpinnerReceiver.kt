/*
 * Copyright (C) 2025-2026 AxionOS Project
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

package com.android.axion.widgets.cardlab.fidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.WidgetUpdateService
import com.android.axion.widgets.provider.AodState
import kotlin.math.sqrt

class AxBottleSpinnerReceiver : AxionWidgetProvider() {

    override fun refresh(context: Context, service: WidgetUpdateService) = update(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SPIN) {
            startSpin(context.applicationContext)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        update(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context))
    }

    companion object {
        private const val ACTION_SPIN = "com.android.axion.widgets.ACTION_BOTTLE_SPIN"
        private const val PREFS_NAME = "bottle_spinner_prefs"
        private const val KEY_ANGLE = "current_angle"
        private const val BITMAP_SIZE = 420
        private const val GRID_SIZE = 19
        private const val CENTER = GRID_SIZE / 2

        private val BOTTLE =
            arrayOf(
                "...................",
                "...................",
                "........###........",
                "........#.#........",
                "........#.#........",
                "........#.#........",
                ".......##.##.......",
                "......##...##......",
                "......#.....#......",
                "......#.....#......",
                "......#.+#+.#......",
                "......#.+.+.#......",
                "......#.+#+.#......",
                "......#.....#......",
                "......#.....#......",
                "......#######......",
                "......#######......",
                "...................",
                "...................",
            )

        private val SPIN_TOKEN = Any()
        private val handler = Handler(Looper.getMainLooper())
        @Volatile private var currentAngle = 0f
        private var cachedBitmap: Bitmap? = null
        private var cachedColor = 0

        fun update(context: Context) {
            cachedBitmap = null
            val prefs = getPrefs(context)
            currentAngle = prefs.getFloat(KEY_ANGLE, 0f)
            val views = buildViews(context)
            updateAllWidgets(context, AxBottleSpinnerReceiver::class.java, views)
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = getPrefs(context)
            val angle = prefs.getFloat(KEY_ANGLE, 0f)
            val aod = AodState.isAod

            val dotColor =
                if (aod) Color.WHITE else context.getColor(R.color.battery_device_primary_color)
            val bgDotAlpha = if (aod) 20 else 35
            val bitmap = getOrCreateBitmap(dotColor, bgDotAlpha)

            val views = RemoteViews(context.packageName, R.layout.widget_bottle_spinner)
            views.setImageViewBitmap(R.id.spinner_image, bitmap)
            views.setFloat(R.id.spinner_image, "setRotation", angle)

            if (aod) {
                views.setInt(
                    R.id.spinner_root,
                    "setBackgroundResource",
                    R.drawable.bg_widget_card_aod,
                )
            } else {
                views.setInt(R.id.spinner_root, "setBackgroundResource", R.drawable.bg_widget_card)
            }

            views.setOnClickPendingIntent(R.id.spinner_root, createSpinIntent(context))
            return views
        }

        private fun startSpin(context: Context) {
            handler.removeCallbacksAndMessages(SPIN_TOKEN)

            var angle = currentAngle

            var velocity = 700f + (Math.random() * 600f).toFloat()
            val baseTime = SystemClock.uptimeMillis()
            var delay = 0L

            while (velocity > 8f) {
                val frameMs = if (velocity > 100f) 40L else 80L
                angle += velocity * frameMs / 1000f
                velocity *= if (velocity > 100f) 0.94f else 0.88f
                delay += frameMs
                val frameAngle = angle % 360f
                handler.postAtTime(
                    {
                        currentAngle = frameAngle
                        val views = RemoteViews(context.packageName, R.layout.widget_bottle_spinner)
                        views.setFloat(R.id.spinner_image, "setRotation", frameAngle)
                        partialUpdate(context, views)
                    },
                    SPIN_TOKEN,
                    baseTime + delay,
                )
            }

            val finalAngle = angle % 360f
            handler.postAtTime(
                {
                    currentAngle = finalAngle
                    getPrefs(context).edit().putFloat(KEY_ANGLE, finalAngle).apply()
                },
                SPIN_TOKEN,
                baseTime + delay + 100L,
            )
        }

        private fun partialUpdate(context: Context, views: RemoteViews) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids =
                mgr.getAppWidgetIds(ComponentName(context, AxBottleSpinnerReceiver::class.java))
            for (id in ids) {
                mgr.partiallyUpdateAppWidget(id, views)
            }
        }

        private fun createSpinIntent(context: Context): PendingIntent {
            val intent =
                Intent(ACTION_SPIN).apply { setClass(context, AxBottleSpinnerReceiver::class.java) }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun getPrefs(context: Context) =
            context
                .createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private fun getOrCreateBitmap(color: Int, bgDotAlpha: Int): Bitmap {
            val cached = cachedBitmap
            if (cached != null && cachedColor == color) return cached
            val bitmap = createDotMatrixBottle(BITMAP_SIZE, color, bgDotAlpha)
            cachedBitmap = bitmap
            cachedColor = color
            return bitmap
        }

        private fun createDotMatrixBottle(size: Int, dotColor: Int, bgDotAlpha: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val step = size.toFloat() / GRID_SIZE
            val dotRadius = step * 0.34f
            val circleR = GRID_SIZE / 2f

            for (gy in 0 until GRID_SIZE) {
                val row = BOTTLE[gy]
                for (gx in 0 until GRID_SIZE) {
                    val dx = gx - CENTER
                    val dy = gy - CENTER
                    val dist = sqrt((dx * dx + dy * dy).toFloat())
                    val inCircle = dist <= circleR
                    val ch = if (gx < row.length) row[gx] else '.'

                    if (ch == '.' && !inCircle) continue

                    paint.color = dotColor
                    paint.alpha =
                        when (ch) {
                            '#' -> 255
                            '+' -> (255 * 0.45f).toInt()
                            else -> bgDotAlpha
                        }
                    canvas.drawCircle((gx + 0.5f) * step, (gy + 0.5f) * step, dotRadius, paint)
                }
            }
            return bitmap
        }
    }
}
