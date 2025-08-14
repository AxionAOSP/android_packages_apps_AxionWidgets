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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class PillTileWidgetReceiver : AppWidgetProvider() {

    private var job: Job? = null
    private var listening = false

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TILE_CLICK) {
            val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
            if (widgetId != -1) {
                TileManager.updateState(widgetId)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        bind(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefs.removeWidget(context, it) }
    }

    override fun onDisabled(context: Context) {
        job?.cancel()
    }

    private fun startFlow(context: Context) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            TileManager.tilesFlow.collectLatest { tiles ->
                tiles.values.forEach { data ->
                    context.updateWidget(data.widgetId, data)
                }
            }
        }
    }

    fun bind(context: Context) {
        if (listening) return
        TileManager.bind(context)
        TileManager.addConsumer(this)
        startFlow(context)
        listening = true
    }

    fun dispose() {
        if (!listening) return
        job?.cancel()
        TileManager.removeConsumer(this)
        listening = false
    }
}
