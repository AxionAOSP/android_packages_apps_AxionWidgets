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

import android.appwidget.AppWidgetManager
import android.content.*
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.R
import com.android.axion.widgets.utils.logger
import com.android.axion.widgets.data.PhotoWidgetData

class PhotoWidgetSmallReceiver : AxionWidgetProvider() {

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val interactor = PhotoInteractor(context)

        appWidgetIds.forEach { widgetId ->
            interactor.removeImageUris(widgetId)
            logger("deleted widgetId=$widgetId, clean up!")
        }

        super.onDeleted(context, appWidgetIds)
    }

    override fun requiredProviders() = listOf(
        PhotoProvider::class
    )

    companion object {
        fun update(context: Context, data: PhotoWidgetData) {
            updateWidget(context, PhotoWidgetSmallReceiver::class.java, data) { ctx, d ->
                PhotoInteractor(context).updateWidget(d.widgetId, d.bitmap)
            }
        }
    }
}
