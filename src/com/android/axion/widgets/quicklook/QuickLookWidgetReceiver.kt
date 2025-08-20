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
import com.android.axion.widgets.callback.QuickLookDataCallback
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.di.QuickLookWidgetEntryPoint
import com.android.axion.widgets.manager.QuickLookDataManager
import com.android.axion.widgets.quicklook.QuickLookWidgetInteractor
import dagger.hilt.android.EntryPointAccessors

class QuickLookWidgetReceiver : AppWidgetProvider(), QuickLookDataCallback {

    private lateinit var interactor: QuickLookWidgetInteractor
    private lateinit var dataManager: QuickLookDataManager
    private var listening = false

    private fun initDependencies(context: Context) {
        if (::interactor.isInitialized && ::dataManager.isInitialized) return
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            QuickLookWidgetEntryPoint::class.java
        )
        interactor = entryPoint.quickLookWidgetInteractor()
        dataManager = entryPoint.quickLookDataManager()
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        initDependencies(context)
        startListening()
        refreshWidgets()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        stopListening()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        stopListening()
    }

    private fun startListening() {
        if (listening) return
        dataManager.addListener(this)
        listening = true
    }

    private fun stopListening() {
        if (!listening) return
        dataManager.removeListener(this)
        listening = false
    }

    private fun refreshWidgets() {
        val context = interactor.context
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, QuickLookWidgetReceiver::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        for (appWidgetId in appWidgetIds) {
            val views = interactor.updateRemoteViews()
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onDataUpdated() {
        refreshWidgets()
    }
}
