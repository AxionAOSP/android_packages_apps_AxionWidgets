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
import android.graphics.Color
import android.graphics.Paint
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.android.axion.widgets.R
import com.android.axion.widgets.data.TileData
import com.android.axion.widgets.provider.AodState

const val ACTION_TILE_CLICK = "com.android.axion.widgets.ACTION_TILE_CLICK"
const val EXTRA_WIDGET_ID = "extra_widget_id"

private const val PILL_ASPECT_RATIO = 1.4f
private const val LAUNCHER_WIDGET_PADDING = 12f
private const val TILE_ICON_IN_CIRCLE_RATIO = 0.47f
private const val PILL_ICON_RATIO = 0.40f
private const val PILL_TEXT_RATIO = 0.22f
private const val PILL_PAD_START_RATIO = 0.22f

private fun getWidgetSize(options: android.os.Bundle): SizeF {
    val sizes = options.getParcelableArrayList(
        AppWidgetManager.OPTION_APPWIDGET_SIZES,
        SizeF::class.java,
    )
    if (!sizes.isNullOrEmpty()) return sizes.first()
    val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0).toFloat()
    val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
    return SizeF(maxW, maxH)
}

fun Context.isPillSize(widgetId: Int): Boolean {
    val options = AppWidgetManager.getInstance(this).getAppWidgetOptions(widgetId)
    val size = getWidgetSize(options)
    return size.width > 0 && size.height > 0 && size.width / size.height > PILL_ASPECT_RATIO
}

fun Context.updateWidget(widgetId: Int, data: TileData) {
    val appWidgetManager = AppWidgetManager.getInstance(this)
    val isPill = isPillSize(widgetId)
    val layoutId = if (isPill) R.layout.widget_pill_tile else R.layout.widget_tile
    val views = RemoteViews(packageName, layoutId)
    val aod = AodState.isAod

    if (aod) {
        views.setViewVisibility(R.id.overlay_active_tile_view, View.GONE)
        views.setViewVisibility(R.id.tile_active_view, View.GONE)
        views.setViewVisibility(R.id.tile_view, View.VISIBLE)
        views.setViewVisibility(R.id.tile_bg, View.VISIBLE)
        views.setInt(R.id.tile_bg, "setBackgroundResource", R.drawable.bg_widget_tile_aod)
        views.setImageViewResource(R.id.tile_view, data.iconRes)
        views.setInt(R.id.tile_view, "setColorFilter", Color.WHITE)
    } else {
        val overlay = if (data.isActive) View.VISIBLE else View.GONE
        val tile = if (data.isActive) View.GONE else View.VISIBLE

        views.setViewVisibility(R.id.overlay_active_tile_view, overlay)
        views.setViewVisibility(R.id.tile_active_view, overlay)
        views.setViewVisibility(R.id.tile_view, tile)
        views.setViewVisibility(R.id.tile_bg, tile)
        views.setInt(R.id.tile_bg, "setBackgroundResource", R.drawable.bg_widget_tile)
        views.setImageViewResource(R.id.tile_view, data.iconRes)
        views.setImageViewResource(R.id.tile_active_view, data.iconRes)
        views.setInt(
            R.id.tile_view,
            "setColorFilter",
            getColor(R.color.battery_device_primary_color),
        )
        views.setInt(
            R.id.tile_active_view,
            "setColorFilter",
            getColor(R.color.device_primary_color_inverse),
        )
    }

    val options = appWidgetManager.getAppWidgetOptions(widgetId)
    val widgetSize = getWidgetSize(options)

    if (isPill) {
        applyPillSizing(views, widgetSize, data, aod, widgetId)
    } else {
        applyTileSizing(views, widgetSize, widgetId)
    }

    val pendingIntent =
        broadcastPendingIntent(
            requestCode = data.widgetId,
            action = ACTION_TILE_CLICK,
            extras = mapOf(EXTRA_WIDGET_ID to data.widgetId),
            receiverClass = AxTileReceiver::class.java,
        )
    views.setOnClickPendingIntent(R.id.tile_view_root, pendingIntent)

    appWidgetManager.updateAppWidget(widgetId, views)
}

private fun Context.applyTileSizing(views: RemoteViews, widgetSize: SizeF, widgetId: Int) {
    val w = widgetSize.width
    val h = widgetSize.height
    if (w <= 0f || h <= 0f) return

    val circleSize = WidgetPrefs.getTileSizeDp(this, widgetId).toFloat()
    views.setViewLayoutWidth(R.id.tile_circle, circleSize, TypedValue.COMPLEX_UNIT_DIP)
    views.setViewLayoutHeight(R.id.tile_circle, circleSize, TypedValue.COMPLEX_UNIT_DIP)

    val iconSize = circleSize * TILE_ICON_IN_CIRCLE_RATIO
    for (id in intArrayOf(R.id.tile_view, R.id.tile_active_view)) {
        views.setViewLayoutWidth(id, iconSize, TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutHeight(id, iconSize, TypedValue.COMPLEX_UNIT_DIP)
    }
}

private fun Context.applyPillSizing(
    views: RemoteViews,
    widgetSize: SizeF,
    data: TileData,
    aod: Boolean,
    widgetId: Int,
) {
    val w = widgetSize.width
    val h = widgetSize.height
    if (h <= 0f) return

    val pillHeight = WidgetPrefs.getPillHeightDp(this, widgetId).toFloat()
    val pillWidthPref = WidgetPrefs.getPillWidthDp(this, widgetId).toFloat()
    val cellH = pillHeight + LAUNCHER_WIDGET_PADDING

    val label = data.label ?: data.spec
    val activeLabel = data.secondaryLabel?.takeIf { it.isNotEmpty() } ?: label
    views.setTextViewText(R.id.tile_label, label)
    views.setTextViewText(R.id.tile_label_active, activeLabel)

    if (aod) {
        views.setViewVisibility(R.id.tile_label, View.VISIBLE)
        views.setViewVisibility(R.id.tile_label_active, View.GONE)
        views.setTextColor(R.id.tile_label, Color.WHITE)
    } else {
        val ov = if (data.isActive) View.VISIBLE else View.GONE
        val tl = if (data.isActive) View.GONE else View.VISIBLE
        views.setViewVisibility(R.id.tile_label, tl)
        views.setViewVisibility(R.id.tile_label_active, ov)
        views.setTextColor(R.id.tile_label, getColor(R.color.battery_device_primary_color))
        views.setTextColor(
            R.id.tile_label_active,
            getColor(R.color.device_primary_color_inverse),
        )
    }

    val iconSize = cellH * PILL_ICON_RATIO
    for (id in intArrayOf(R.id.tile_view, R.id.tile_active_view)) {
        views.setViewLayoutWidth(id, iconSize, TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutHeight(id, iconSize, TypedValue.COMPLEX_UNIT_DIP)
    }

    val textSize = cellH * PILL_TEXT_RATIO
    views.setTextViewTextSize(R.id.tile_label, TypedValue.COMPLEX_UNIT_DIP, textSize)
    views.setTextViewTextSize(R.id.tile_label_active, TypedValue.COMPLEX_UNIT_DIP, textSize)
    val padStart = dpToPx(cellH * PILL_PAD_START_RATIO)
    views.setViewPadding(R.id.tile_content, padStart, 0, 0, 0)

    val autoFit = WidgetPrefs.isPillAutoFitWidth(this, widgetId)
    val cellMaxWidth = if (w > 0f) w else Float.MAX_VALUE
    val targetWidth =
        if (autoFit) {
            val longer = if (activeLabel.length >= label.length) activeLabel else label
            autoFitPillWidth(longer, textSize, iconSize, cellH * PILL_PAD_START_RATIO)
                .coerceAtMost(cellMaxWidth)
        } else {
            pillWidthPref.coerceAtMost(cellMaxWidth)
        }
    views.setViewLayoutWidth(R.id.tile_view_root, targetWidth, TypedValue.COMPLEX_UNIT_DIP)
    views.setViewLayoutHeight(R.id.tile_view_root, pillHeight, TypedValue.COMPLEX_UNIT_DIP)
}

private fun Context.autoFitPillWidth(
    label: String,
    textSizeDp: Float,
    iconSizeDp: Float,
    padStartDp: Float,
): Float {
    val textSizePx = dpToPx(textSizeDp).toFloat()
    val paint = Paint().apply { textSize = textSizePx }
    val textWidthPx = paint.measureText(label)
    val textWidthDp = textWidthPx / resources.displayMetrics.density

    val labelMarginStartDp =
        resources.getDimension(R.dimen.widget_label_frame_margin_start) /
            resources.displayMetrics.density
    val labelPadEndDp =
        resources.getDimension(R.dimen.widget_pill_label_padding_end) /
            resources.displayMetrics.density
    val outerPadHorizDp =
        resources.getDimension(R.dimen.pill_widget_padding_horizontal) /
            resources.displayMetrics.density

    return padStartDp +
        iconSizeDp +
        labelMarginStartDp +
        textWidthDp +
        labelPadEndDp +
        outerPadHorizDp * 2f
}

private fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

fun Context.broadcastPendingIntent(
    requestCode: Int,
    action: String,
    extras: Map<String, Any?> = emptyMap(),
    receiverClass: Class<*>,
): PendingIntent {
    val intent =
        Intent(this, receiverClass).apply {
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
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
