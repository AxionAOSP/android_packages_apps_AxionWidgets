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
package com.android.axion.widgets.quicklook

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.android.axion.widgets.callback.QuickLookDataCallback
import com.android.axion.widgets.manager.QuickLookDataManager

class QuickLookWidgetReceiver : AppWidgetProvider(), QuickLookDataCallback {

    private val Context.dm: QuickLookDataManager
        get() = QuickLookDataManager.get(this)

    private lateinit var appContext: Context
    private var initialized = false

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (!initialized) init(context)
    }

    override fun onDisabled(context: Context) {
        context.dm.dispose()
        initialized = false
    }

    private fun init(context: Context) {
        appContext = context.applicationContext
        context.dm.init()
        context.dm.addListener(this)
        refreshWidgets()
        initialized = true
    }

    private fun refreshWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val thisWidget = ComponentName(appContext, QuickLookWidgetReceiver::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        appWidgetIds.forEach { appWidgetId ->
            val views: RemoteViews? = QuickLookWidgetInteractor.get(appContext).updateRemoteViews()
            views?.let {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onDataUpdated() {
        refreshWidgets()
    }
}
