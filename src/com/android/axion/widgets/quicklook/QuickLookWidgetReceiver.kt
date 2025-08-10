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
import android.view.View
import android.widget.RemoteViews
import com.android.axion.widgets.callback.QuickLookDataCallback
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.manager.QuickLookDataManager
import java.text.SimpleDateFormat
import java.util.*

class QuickLookWidgetReceiver : AppWidgetProvider(), QuickLookDataCallback {

    private var interactor: QuickLookWidgetInteractor? = null
    private var listening = false

    private fun getInteractor(context: Context): QuickLookWidgetInteractor {
        if (interactor == null) {
            interactor = QuickLookWidgetInteractor(context)
        }
        return interactor!!
    }
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        init(context)
        QuickLookDataManager.notifyListeners()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        cleanup()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cleanup()
    }
    
    fun init(context: Context) {
        if (listening) return
        QuickLookDataManager.init(context)
        QuickLookDataManager.addListener(this)
        listening = true
    }
    
    fun cleanup() {
        if (!listening) return
        QuickLookDataManager.removeListener(this)
        QuickLookDataManager.cleanup()
        listening = false
    }

    override fun onDataUpdated(data: QuickLookData) {
        val context = QuickLookDataManager.getAppContext()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, QuickLookWidgetReceiver::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        val interactor = getInteractor(context)
        for (appWidgetId in appWidgetIds) {
            val views = interactor.updateRemoteViews(data)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
