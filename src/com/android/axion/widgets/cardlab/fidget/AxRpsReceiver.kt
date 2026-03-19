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
import com.android.axion.widgets.provider.AodState
import kotlin.math.sqrt

class AxRpsReceiver : AxionWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_PLAY) {
            startGame(context.applicationContext)
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
        private const val ACTION_PLAY = "com.android.axion.widgets.ACTION_RPS_PLAY"
        private const val PREFS_NAME = "rps_prefs"
        private const val KEY_LAST = "last_choice"

        private const val ROCK = 0
        private const val PAPER = 1
        private const val SCISSORS = 2
        private const val NONE = -1

        private const val BITMAP_SIZE = 420
        private const val GRID_SIZE = 19
        private const val CENTER = GRID_SIZE / 2

        private val GAME_TOKEN = Any()
        private val handler = Handler(Looper.getMainLooper())
        private var cachedBitmaps = arrayOfNulls<Bitmap>(3)
        private var cachedColor = 0

        private val PATTERNS =
            arrayOf(
                arrayOf(
                    "...................",
                    "...................",
                    "...................",
                    ".......#####.......",
                    "....###+++++###....",
                    "...##++++++++++#...",
                    "..##++++++++++++#..",
                    "..#+++++++++++++#..",
                    ".##++++++++++++++#.",
                    ".#+++++++++++++++#.",
                    ".#+++++++++++++++#.",
                    "..#+++++++++++++#..",
                    "..##++++++++++++#..",
                    "...##++++++++++#...",
                    "....###+++++###....",
                    "......#######......",
                    "...................",
                    "...................",
                    "...................",
                ),
                arrayOf(
                    "...................",
                    ".......#.#.#.#.....",
                    "......##.#.#.##....",
                    "......##.#.#.##....",
                    "......##.#.#.##....",
                    "......##.#.#.##....",
                    "..#...##.#.#.##....",
                    "..##..##.#.#.##....",
                    "..##.###########...",
                    "...##.##########...",
                    "....############...",
                    ".....##########....",
                    "......########.....",
                    "......########.....",
                    ".......######......",
                    "........####.......",
                    "...................",
                    "...................",
                    "...................",
                ),
                arrayOf(
                    "...................",
                    "..##...........##..",
                    "...##.........##...",
                    "....##.......##....",
                    ".....##.....##.....",
                    "......##...##......",
                    ".......##.##.......",
                    "........###........",
                    ".......##.##.......",
                    "......##...##......",
                    ".....##.....##.....",
                    "....###.....###....",
                    "...#..#.....#..#...",
                    "...#..#.....#..#...",
                    "....##.......##....",
                    "...................",
                    "...................",
                    "...................",
                    "...................",
                ),
            )

        private val IDLE_PATTERN =
            arrayOf(
                "...................",
                "...................",
                ".......#####.......",
                "......#.....#......",
                ".....#.......#.....",
                ".....#......##.....",
                "...........##......",
                "..........##.......",
                ".........##........",
                "........##.........",
                "........##.........",
                "...................",
                "........##.........",
                "........##.........",
                "...................",
                "...................",
                "...................",
                "...................",
                "...................",
            )

        fun update(context: Context) {
            cachedBitmaps = arrayOfNulls(3)
            val views = buildViews(context)
            updateAllWidgets(context, AxRpsReceiver::class.java, views)
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = getPrefs(context)
            val last = prefs.getInt(KEY_LAST, NONE)
            val aod = AodState.isAod

            val dotColor =
                if (aod) Color.WHITE else context.getColor(R.color.battery_device_primary_color)
            val bgDotAlpha = if (aod) 20 else 35
            val bitmap = getOrCreateBitmap(last, dotColor, bgDotAlpha)

            val views = RemoteViews(context.packageName, R.layout.widget_rps)
            views.setImageViewBitmap(R.id.rps_symbol, bitmap)

            if (aod) {
                views.setInt(R.id.rps_root, "setBackgroundResource", R.drawable.bg_widget_card_aod)
            } else {
                views.setInt(R.id.rps_root, "setBackgroundResource", R.drawable.bg_widget_card)
            }

            views.setOnClickPendingIntent(R.id.rps_root, createPlayIntent(context))
            return views
        }

        private fun startGame(context: Context) {
            handler.removeCallbacksAndMessages(GAME_TOKEN)

            val choice = (0..2).random()
            val aod = AodState.isAod
            val dotColor =
                if (aod) Color.WHITE else context.getColor(R.color.battery_device_primary_color)
            val bgDotAlpha = if (aod) 20 else 35

            val baseTime = SystemClock.uptimeMillis()
            val shuffleFrames = 12
            val frameMs = 80L

            for (i in 0 until shuffleFrames) {
                handler.postAtTime(
                    {
                        val r = (0..2).random()
                        val bitmap = getOrCreateBitmap(r, dotColor, bgDotAlpha)
                        val views = RemoteViews(context.packageName, R.layout.widget_rps)
                        views.setImageViewBitmap(R.id.rps_symbol, bitmap)
                        partialUpdate(context, views)
                    },
                    GAME_TOKEN,
                    baseTime + i * frameMs,
                )
            }

            handler.postAtTime(
                {
                    getPrefs(context).edit().putInt(KEY_LAST, choice).apply()
                    val bitmap = getOrCreateBitmap(choice, dotColor, bgDotAlpha)
                    val views = RemoteViews(context.packageName, R.layout.widget_rps)
                    views.setImageViewBitmap(R.id.rps_symbol, bitmap)
                    partialUpdate(context, views)
                },
                GAME_TOKEN,
                baseTime + shuffleFrames * frameMs,
            )
        }

        private fun partialUpdate(context: Context, views: RemoteViews) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, AxRpsReceiver::class.java))
            for (id in ids) {
                mgr.partiallyUpdateAppWidget(id, views)
            }
        }

        private fun createPlayIntent(context: Context): PendingIntent {
            val intent = Intent(ACTION_PLAY).apply { setClass(context, AxRpsReceiver::class.java) }
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

        private fun getOrCreateBitmap(choice: Int, dotColor: Int, bgDotAlpha: Int): Bitmap {
            if (choice in 0..2 && cachedColor == dotColor) {
                cachedBitmaps[choice]?.let {
                    return it
                }
            }
            val bitmap = createBitmap(choice, dotColor, bgDotAlpha)
            if (choice in 0..2) {
                cachedBitmaps[choice] = bitmap
                cachedColor = dotColor
            }
            return bitmap
        }

        private fun createBitmap(choice: Int, dotColor: Int, bgDotAlpha: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val step = BITMAP_SIZE.toFloat() / GRID_SIZE
            val dotRadius = step * 0.34f
            val circleR = GRID_SIZE / 2f
            val pattern = if (choice in 0..2) PATTERNS[choice] else IDLE_PATTERN

            for (gy in 0 until GRID_SIZE) {
                val row = pattern[gy]
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
