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
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

class PhotoWidgetLargeReceiver : AppWidgetProvider() {

    private var interactor: PhotoInteractor? = null

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (interactor == null) {
            interactor = PhotoInteractor(context.applicationContext)
        }
        interactor?.bind(appWidgetIds.toList())
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val remainingWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, PhotoWidgetLargeReceiver::class.java)
        )
        if (remainingWidgetIds.isEmpty()) {
            interactor?.dispose()
            interactor = null
        } else {
            interactor?.bind(remainingWidgetIds.toList())
        }
    }

    override fun onDisabled(context: Context) {
        interactor?.dispose()
        interactor = null
    }
}
