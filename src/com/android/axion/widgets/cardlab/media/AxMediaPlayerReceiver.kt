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

package com.android.axion.widgets.cardlab.media

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.WidgetUpdateService
import com.android.axion.widgets.data.MediaPlayerData
import com.android.axion.widgets.provider.AodState
import com.android.axion.widgets.provider.MediaPlayerProvider

class AxMediaPlayerReceiver : AxionWidgetProvider() {

    override fun requiredProviders() = listOf(MediaPlayerProvider::class)

    override fun refresh(context: Context, service: WidgetUpdateService) =
        update(context, service.mediaPlayerProvider.currentData)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MEDIA_PLAY_PAUSE -> MediaPlayerProvider.get(context).playPause()
            ACTION_MEDIA_NEXT -> MediaPlayerProvider.get(context).next()
            ACTION_MEDIA_PREV -> MediaPlayerProvider.get(context).previous()
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val data = MediaPlayerProvider.get(context).currentData
        val views = buildViews(context, data)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        private const val ACTION_MEDIA_PLAY_PAUSE =
            "com.android.axion.widgets.ACTION_MEDIA_PLAY_PAUSE"
        private const val ACTION_MEDIA_NEXT = "com.android.axion.widgets.ACTION_MEDIA_NEXT"
        private const val ACTION_MEDIA_PREV = "com.android.axion.widgets.ACTION_MEDIA_PREV"

        fun update(context: Context, data: MediaPlayerData?) {
            val views = buildViews(context, data)
            updateAllWidgets(context, AxMediaPlayerReceiver::class.java, views)
        }

        private fun buildViews(context: Context, data: MediaPlayerData?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_media_player)
            val aod = AodState.isAod

            if (aod) {
                views.setInt(
                    R.id.media_root,
                    "setBackgroundResource",
                    R.drawable.bg_media_widget_aod,
                )
            } else {
                views.setInt(R.id.media_root, "setBackgroundResource", R.drawable.bg_media_widget)
            }

            if (data != null && (data.title != null || data.artist != null)) {
                views.setViewVisibility(R.id.media_content, View.VISIBLE)
                views.setViewVisibility(R.id.media_idle, View.GONE)

                if (aod) {
                    if (data.albumArt != null) {
                        views.setImageViewBitmap(R.id.album_art, toGrayscale(data.albumArt))
                    } else {
                        views.setImageViewBitmap(R.id.album_art, null)
                    }
                    views.setInt(R.id.album_art, "setBackgroundColor", Color.TRANSPARENT)
                } else if (data.albumArt != null) {
                    views.setImageViewBitmap(R.id.album_art, data.albumArt)
                } else {
                    views.setImageViewBitmap(R.id.album_art, null)
                    views.setImageViewResource(R.id.album_art, R.drawable.ic_media_default)
                }

                views.setTextViewText(R.id.track_title, data.title ?: "")
                views.setTextViewText(R.id.track_artist, data.artist ?: "")

                views.setImageViewResource(
                    R.id.btn_play_pause,
                    if (data.isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play,
                )

                if (data.duration > 0) {
                    val progress =
                        ((data.position.toFloat() / data.duration) * 1000).toInt().coerceIn(0, 1000)
                    views.setProgressBar(R.id.progress_bar, 1000, progress, false)
                    views.setViewVisibility(R.id.progress_bar, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.progress_bar, View.GONE)
                }

                views.setOnClickPendingIntent(
                    R.id.btn_prev,
                    createActionIntent(context, ACTION_MEDIA_PREV, 1),
                )
                views.setOnClickPendingIntent(
                    R.id.btn_play_pause,
                    createActionIntent(context, ACTION_MEDIA_PLAY_PAUSE, 2),
                )
                views.setOnClickPendingIntent(
                    R.id.btn_next,
                    createActionIntent(context, ACTION_MEDIA_NEXT, 3),
                )

                if (data.packageName != null) {
                    val launchIntent =
                        context.packageManager.getLaunchIntentForPackage(data.packageName)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        val pi =
                            PendingIntent.getActivity(
                                context,
                                4,
                                launchIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            )
                        views.setOnClickPendingIntent(R.id.album_art, pi)
                    }
                }
            } else {
                views.setViewVisibility(R.id.media_content, View.GONE)
                views.setViewVisibility(R.id.media_idle, if (aod) View.GONE else View.VISIBLE)
                views.setImageViewBitmap(R.id.album_art, null)

                val lastPkg = MediaPlayerProvider.get(context).lastPlayedPackage
                if (lastPkg != null) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(lastPkg)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        val pi =
                            PendingIntent.getActivity(
                                context,
                                5,
                                launchIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            )
                        views.setOnClickPendingIntent(R.id.media_idle, pi)
                    }
                }
            }

            return views
        }

        private fun toGrayscale(src: Bitmap): Bitmap {
            val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint =
                Paint().apply {
                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                    alpha = 120
                }
            canvas.drawBitmap(src, 0f, 0f, paint)
            return bmp
        }

        private fun createActionIntent(
            context: Context,
            action: String,
            requestCode: Int,
        ): PendingIntent {
            val intent =
                Intent(action).apply { setClass(context, AxMediaPlayerReceiver::class.java) }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
