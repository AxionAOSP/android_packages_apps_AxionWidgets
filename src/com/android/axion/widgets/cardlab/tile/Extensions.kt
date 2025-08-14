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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*
import android.view.View
import android.widget.RemoteViews
import com.android.axion.widgets.R

const val ACTION_TILE_CLICK = "com.android.axion.widgets.ACTION_TILE_CLICK"
const val EXTRA_WIDGET_ID = "extra_widget_id"

fun Context.updateWidget(widgetId: Int, data: TileData) {
    val appWidgetManager = AppWidgetManager.getInstance(this)
    val provider = appWidgetManager.getAppWidgetInfo(widgetId)?.provider

    val isPill = provider?.className == PillTileWidgetReceiver::class.java.name
    val layoutRes = if (isPill) R.layout.widget_pill_tile else R.layout.widget_tile
    val views = RemoteViews(packageName, layoutRes)

    val overlay = if (data.isActive) View.VISIBLE else View.GONE
    val tile = if (data.isActive) View.GONE else View.VISIBLE

    views.setViewVisibility(R.id.overlay_active_tile_view, overlay)
    views.setViewVisibility(R.id.tile_active_view, overlay)
    views.setViewVisibility(R.id.tile_view, tile)
    views.setImageViewResource(R.id.tile_view, data.iconRes)
    views.setImageViewResource(R.id.tile_active_view, data.iconRes)

    if (isPill) {
        views.setViewVisibility(R.id.tile_label_active, overlay)
        views.setViewVisibility(R.id.tile_label, tile)
        val label = if (!data.label.isNullOrEmpty()) data.label else data.type
        views.setTextViewText(R.id.tile_label_active, label)
        views.setTextViewText(R.id.tile_label, label)
    }

    val receiverClass = if (isPill) PillTileWidgetReceiver::class.java else TileWidgetReceiver::class.java
    val pendingIntent = broadcastPendingIntent(
        requestCode = data.widgetId,
        action = ACTION_TILE_CLICK,
        extras = mapOf(EXTRA_WIDGET_ID to data.widgetId),
        receiverClass = receiverClass
    )
    views.setOnClickPendingIntent(R.id.tile_view_root, pendingIntent)

    appWidgetManager.updateAppWidget(widgetId, views)
}

fun Context.broadcastPendingIntent(
    requestCode: Int,
    action: String,
    extras: Map<String, Any?> = emptyMap(),
    receiverClass: Class<*>
): PendingIntent {
    val intent = Intent(this, receiverClass).apply {
        this.action = action
        extras.forEach { (key, value) ->
            when (value) {
                is Boolean -> putExtra(key, value)
                is Int -> putExtra(key, value)
                is String -> putExtra(key, value)
            }
        }
    }
    return PendingIntent.getBroadcast(
        this,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
