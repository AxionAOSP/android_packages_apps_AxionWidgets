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
package com.android.axion.widgets.cardlab.tile

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class TileWidgetReceiver : AppWidgetProvider() {

    private lateinit var tileManager: TileManager
    private var listening = false

    private fun initDependencies(context: Context) {
        if (::tileManager.isInitialized) return
        tileManager = TileManager.get(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        initDependencies(context)

        if (intent.action == ACTION_TILE_CLICK) {
            val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
            if (widgetId != -1) {
                tileManager.updateState(widgetId)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        initDependencies(context)
        bind()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetPrefs.removeWidget(context, it) }
    }

    override fun onDisabled(context: Context) {
        dispose()
    }

    private fun bind() {
        if (listening) return
        tileManager.addConsumer(this)
        listening = true
    }

    private fun dispose() {
        if (!listening) return
        tileManager.removeConsumer(this)
        listening = false
    }
}
