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

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemProperties
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.PhotoWidgetData
import com.android.axion.widgets.data.PhotoWidgetDataList
import com.android.axion.widgets.utils.logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AxionProvider<PhotoWidgetDataList> {

    private val interactor = PhotoInteractor(context)

    private val knownWidgets = mutableSetOf<Pair<Int, Int>>()

    private val nextShuffleTimes = mutableMapOf<Int, Long>()

    override val dataFlow: Flow<PhotoWidgetDataList?> = flow {
        logger("PhotoProvider started")

        while (true) {
            val current = interactor.getAllActiveWidgetIds()
            if (current.isNotEmpty()) {
                knownWidgets += current
                knownWidgets.retainAll(current)
            }

            if (knownWidgets.isEmpty()) {
                logger("no active widgets, emitting null")
                emit(null)
                delay(10_000)
                continue
            }

            val now = System.currentTimeMillis()
            val updates = mutableListOf<PhotoWidgetData>()

            knownWidgets.forEach { (size, widgetId) ->
                val interval = interactor.loadShuffleInterval(widgetId)
                val nextTime = nextShuffleTimes[widgetId] ?: 0L

                if (now >= nextTime) {
                    val uris = interactor.getImageUris(widgetId)
                    logger("widgetId=$widgetId (size=$size) uris=${uris.size}")

                    if (uris.isEmpty()) {
                        updates.add(PhotoWidgetData(widgetId, null, emptyList(), false, size))
                    } else {
                        val prefsKey = "carousel_position_$widgetId"
                        val prefs = context.getSharedPreferences("photo_widget_prefs", Context.MODE_PRIVATE)
                        var pos = prefs.getInt(prefsKey, -1)
                        pos = (pos + 1) % uris.size
                        prefs.edit().putInt(prefsKey, pos).apply()

                        logger("widgetId=$widgetId pos=$pos/${uris.size}")

                        val bitmap = interactor.loadBitmapFromUri(uris[pos])
                        val grayscale = interactor.loadGrayscalePref(widgetId)
                        val finalBitmap = if (grayscale) bitmap?.let { interactor.toGrayscale(it) } else bitmap

                        updates.add(PhotoWidgetData(widgetId, finalBitmap, uris, grayscale, size))
                    }

                    nextShuffleTimes[widgetId] = now + interval
                }
            }

            if (updates.isNotEmpty()) {
                emit(updates)
                logger("emitted ${updates.size} photo widget updates")
            }

            delay(60000)
        }
    }
}
