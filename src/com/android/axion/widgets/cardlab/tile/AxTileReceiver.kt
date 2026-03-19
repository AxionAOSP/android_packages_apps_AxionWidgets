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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.data.TileData

class AxTileReceiver : AxionWidgetProvider() {

    override fun requiredProviders() = listOf(TileRepository::class)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TILE_CLICK) {
            val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
            if (widgetId != -1) {
                TileManager.get(context).updateState(widgetId)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val spec = WidgetPrefs.getWidgetAction(context, appWidgetId) ?: return
        val manager = TileManager.get(context)
        val tileData = manager.tilesFlow[appWidgetId]
        if (tileData != null) {
            context.updateWidget(appWidgetId, tileData)
        } else {
            val data =
                TileData(
                    spec = spec,
                    isActive = false,
                    iconRes = TileIcons.getIcon(spec, false),
                    widgetId = appWidgetId,
                    label = spec.replaceFirstChar { it.uppercase() },
                )
            context.updateWidget(appWidgetId, data)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetPrefs.removeWidget(context, it) }
    }
}
