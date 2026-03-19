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

package com.android.axion.widgets.cardlab.photo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.os.SystemClock
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.PhotoWidgetData
import com.android.axion.widgets.data.PhotoWidgetDataList
import com.android.axion.widgets.utils.logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class PhotoProvider @Inject constructor(@ApplicationContext private val context: Context) :
    AxionProvider<PhotoWidgetDataList> {

    private val interactor = PhotoInteractor(context)

    private val nextShuffleTimes = mutableMapOf<Int, Long>()

    override val dataFlow: Flow<PhotoWidgetDataList?> = callbackFlow {
        logger("PhotoProvider started")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_PHOTO_SHUFFLE).setPackage(context.packageName)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val result = processWidgets()
                    if (result != null) trySend(result)
                    scheduleNext(alarmManager, pendingIntent)
                }
            }

        context.registerReceiver(
            receiver,
            IntentFilter(ACTION_PHOTO_SHUFFLE),
            Context.RECEIVER_NOT_EXPORTED,
        )

        val initial = processWidgets()
        trySend(initial)
        scheduleNext(alarmManager, pendingIntent)

        awaitClose {
            alarmManager.cancel(pendingIntent)
            context.unregisterReceiver(receiver)
        }
    }

    private fun processWidgets(): PhotoWidgetDataList? {
        val current = interactor.getAllActiveWidgetIds()
        if (current.isEmpty()) return null

        val now = System.currentTimeMillis()
        val updates = mutableListOf<PhotoWidgetData>()

        current.forEach { widgetId ->
            val interval = interactor.loadShuffleInterval(widgetId)
            val nextTime = nextShuffleTimes[widgetId] ?: 0L

            if (now >= nextTime) {
                val uris = interactor.getImageUris(widgetId)
                logger("widgetId=$widgetId uris=${uris.size}")

                if (uris.isEmpty()) {
                    return@forEach
                } else {
                    val prefsKey = "carousel_position_$widgetId"
                    val prefs =
                        context.getSharedPreferences("photo_widget_prefs", Context.MODE_PRIVATE)
                    var pos = prefs.getInt(prefsKey, -1)
                    pos = (pos + 1) % uris.size
                    prefs.edit().putInt(prefsKey, pos).apply()

                    logger("widgetId=$widgetId pos=$pos/${uris.size}")

                    val bitmap = interactor.loadBitmapFromUri(uris[pos])
                    val grayscale = interactor.loadGrayscalePref(widgetId)
                    val finalBitmap =
                        if (grayscale) bitmap?.let { interactor.toGrayscale(it) } else bitmap

                    updates.add(PhotoWidgetData(widgetId, finalBitmap, uris, grayscale))
                }

                nextShuffleTimes[widgetId] = now + interval
            }
        }

        return if (updates.isNotEmpty()) updates else null
    }

    private fun scheduleNext(alarmManager: AlarmManager, pendingIntent: PendingIntent) {
        val nextTime = nextShuffleTimes.values.minOrNull() ?: return
        val delay = (nextTime - System.currentTimeMillis()).coerceAtLeast(10_000)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delay,
            pendingIntent,
        )
        logger("PhotoProvider: next alarm in ${delay / 1000}s")
    }

    companion object {
        private const val ACTION_PHOTO_SHUFFLE = "com.android.axion.widgets.ACTION_PHOTO_SHUFFLE"
    }
}
