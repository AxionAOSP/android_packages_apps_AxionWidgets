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
import com.android.axion.widgets.utils.logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

data class PhotoWidgetData(
    val widgetId: Int,
    val bitmap: Bitmap?,
    val uris: List<Uri>,
    val grayscale: Boolean
)

@Singleton
class PhotoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AxionProvider<PhotoWidgetData> {

    private val interactor = PhotoInteractor(context)

    override val dataFlow: Flow<PhotoWidgetData?> = flow {
        val activeWidgetIds = interactor.getAllActiveWidgetIds()
        logger("started. activeWidgetIds=$activeWidgetIds")

        if (activeWidgetIds.isEmpty()) {
            logger("no active widgets, emitting null")
            emit(null)
            return@flow
        }

        while (true) {
            activeWidgetIds.forEach { widgetId ->
                val uris = interactor.getImageUris(widgetId)
                logger("widgetId=$widgetId uris=${uris.size}")

                if (uris.isEmpty()) {
                    logger("widgetId=$widgetId has no URIs")
                    emit(PhotoWidgetData(widgetId, null, emptyList(), false))
                    return@forEach
                }

                val prefsKey = "carousel_position_$widgetId"
                val prefs = context.getSharedPreferences("photo_widget_prefs", Context.MODE_PRIVATE)
                var pos = prefs.getInt(prefsKey, -1)
                pos = (pos + 1) % uris.size
                prefs.edit().putInt(prefsKey, pos).apply()

                logger("widgetId=$widgetId pos=$pos/${uris.size}")

                val bitmap = interactor.loadBitmapFromUri(uris[pos])
                val grayscale = interactor.loadGrayscalePref(widgetId)
                val finalBitmap = if (grayscale) bitmap?.let { interactor.toGrayscale(it) } else bitmap

                emit(PhotoWidgetData(widgetId, finalBitmap, uris, grayscale))
                logger("emitted update for widgetId=$widgetId grayscale=$grayscale")
            }

            val interval = activeWidgetIds.minOf { interactor.loadShuffleInterval(it) }

            logger("delaying for $interval ms before next cycle")
            delay(interval)
        }
    }
}
