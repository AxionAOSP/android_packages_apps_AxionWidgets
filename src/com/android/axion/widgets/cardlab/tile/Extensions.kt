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
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioManager
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.android.axion.widgets.R
import com.android.axion.widgets.data.TileData
import com.android.axion.widgets.provider.AodState

const val ACTION_TILE_CLICK = "com.android.axion.widgets.ACTION_TILE_CLICK"
const val EXTRA_WIDGET_ID = "extra_widget_id"
const val EXTRA_RINGER_MODE = "extra_ringer_mode"

private const val PILL_ASPECT_RATIO = 1.4f
private const val LAUNCHER_WIDGET_PADDING = 12f
private const val TILE_ICON_IN_CIRCLE_RATIO = 0.47f
private const val PILL_ICON_RATIO = 0.40f
private const val PILL_TEXT_RATIO = 0.22f
private const val PILL_PAD_START_RATIO = 0.22f
private const val RINGER_ICON_RATIO = 0.36f
private const val RINGER_REQUEST_CODE_MULTIPLIER = 10
private const val SET_BACKGROUND_TINT_LIST = "setBackgroundTintList"

private val RINGER_THUMBS =
    intArrayOf(R.id.ringer_thumb_normal, R.id.ringer_thumb_vibrate, R.id.ringer_thumb_silent)
private val RINGER_THUMB_BACKGROUNDS =
    intArrayOf(
        R.id.ringer_thumb_bg_normal,
        R.id.ringer_thumb_bg_vibrate,
        R.id.ringer_thumb_bg_silent,
    )
private val RINGER_DOTS =
    intArrayOf(R.id.ringer_dot_normal, R.id.ringer_dot_vibrate, R.id.ringer_dot_silent)
private val RINGER_ICONS =
    intArrayOf(R.id.ringer_icon_normal, R.id.ringer_icon_vibrate, R.id.ringer_icon_silent)
private val RINGER_NORMAL_CLICK_IDS =
    intArrayOf(R.id.ringer_click_normal_three, R.id.ringer_click_normal_two)
private val RINGER_SILENT_CLICK_IDS =
    intArrayOf(R.id.ringer_click_silent_three, R.id.ringer_click_silent_two)

private fun getWidgetSize(options: Bundle): SizeF {
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
    val isRingerSlider = isPill && TileIcons.isRingerSpec(data.spec)
    val layoutId =
        when {
            isRingerSlider -> R.layout.widget_ringer_tile
            isPill -> R.layout.widget_pill_tile
            else -> R.layout.widget_tile
        }
    val views = RemoteViews(packageName, layoutId)
    val aod = AodState.isAod
    val options = appWidgetManager.getAppWidgetOptions(widgetId)
    val widgetSize = getWidgetSize(options)

    if (isRingerSlider) {
        applyRingerSliderWidget(views, data, aod)
    } else {
        applyTileAppearance(views, data, aod)
    }

    if (isRingerSlider) {
        applyRingerSliderSizing(views, widgetSize, widgetId)
    } else if (isPill) {
        applyPillSizing(views, widgetSize, data, aod, widgetId)
    } else {
        applyTileSizing(views, widgetSize, widgetId)
    }

    if (isRingerSlider) {
        applyRingerSliderClickActions(views, data.widgetId)
    } else {
        val pendingIntent =
            broadcastPendingIntent(
                requestCode = data.widgetId,
                action = ACTION_TILE_CLICK,
                extras = mapOf(EXTRA_WIDGET_ID to data.widgetId),
                receiverClass = AxTileReceiver::class.java,
            )
        views.setOnClickPendingIntent(R.id.tile_view_root, pendingIntent)
    }

    appWidgetManager.updateAppWidget(widgetId, views)
}

private fun Context.applyTileAppearance(views: RemoteViews, data: TileData, aod: Boolean) {
    if (aod) {
        views.setViewVisibility(R.id.overlay_active_tile_view, View.GONE)
        views.setViewVisibility(R.id.tile_active_view, View.GONE)
        views.setViewVisibility(R.id.tile_view, View.VISIBLE)
        views.setViewVisibility(R.id.tile_bg, View.VISIBLE)
        views.setInt(R.id.tile_bg, "setBackgroundResource", R.drawable.bg_widget_tile_aod)
        views.setBackgroundTint(R.id.tile_bg, null)
        views.setBackgroundTint(R.id.overlay_active_tile_view, null)
        views.setImageViewResource(R.id.tile_view, data.iconRes)
        views.setInt(R.id.tile_view, "setColorFilter", Color.WHITE)
        return
    }

    val inactiveIconColor = getColor(R.color.battery_device_primary_color)
    val activeIconColor = getColor(R.color.device_primary_color_inverse)
    val activeBackgroundColor = getColor(R.color.tile_active)
    val overlay = if (data.isActive) View.VISIBLE else View.GONE
    val tile = if (data.isActive) View.GONE else View.VISIBLE

    views.setViewVisibility(R.id.overlay_active_tile_view, overlay)
    views.setViewVisibility(R.id.tile_active_view, overlay)
    views.setViewVisibility(R.id.tile_view, tile)
    views.setViewVisibility(R.id.tile_bg, tile)
    views.setInt(R.id.tile_bg, "setBackgroundResource", R.drawable.bg_widget_tile)
    views.setInt(
        R.id.overlay_active_tile_view,
        "setBackgroundResource",
        R.drawable.bg_widget_tile,
    )
    views.setBackgroundTint(R.id.tile_bg, getColor(R.color.battery_bg_color))
    views.setBackgroundTint(R.id.overlay_active_tile_view, activeBackgroundColor)
    views.setImageViewResource(R.id.tile_view, data.iconRes)
    views.setImageViewResource(R.id.tile_active_view, data.iconRes)
    views.setInt(R.id.tile_view, "setColorFilter", inactiveIconColor)
    views.setInt(R.id.tile_active_view, "setColorFilter", activeIconColor)
}

private fun Context.applyRingerSliderWidget(
    views: RemoteViews,
    data: TileData,
    aod: Boolean,
) {
    val ringerMode = visibleRingerMode(data.ringerMode, data.hasVibrator)
    val selectedThumb = selectedRingerThumb(ringerMode)
    val selectedDot = selectedRingerDot(ringerMode)
    val thumbBg = if (aod) R.drawable.bg_widget_tile_aod else R.drawable.bg_widget_tile
    val bg = if (aod) R.drawable.bg_widget_tile_aod else R.drawable.bg_widget_tile
    val iconTint = if (aod) Color.WHITE else getColor(R.color.device_primary_color_inverse)
    val dotTint = if (aod) Color.WHITE else getColor(R.color.battery_device_primary_color)
    val activeBgTint = if (aod) null else getColor(R.color.tile_active)
    val trackTint = if (aod) null else getColor(R.color.battery_bg_color)

    views.setInt(R.id.ringer_slider_bg, "setBackgroundResource", bg)
    views.setBackgroundTint(R.id.ringer_slider_bg, trackTint)
    views.setViewVisibility(
        R.id.ringer_slot_vibrate,
        if (data.hasVibrator) View.VISIBLE else View.GONE,
    )
    views.setViewVisibility(
        R.id.ringer_click_zones_three,
        if (data.hasVibrator) View.VISIBLE else View.GONE,
    )
    views.setViewVisibility(
        R.id.ringer_click_zones_two,
        if (data.hasVibrator) View.GONE else View.VISIBLE,
    )

    RINGER_THUMBS.forEach { id ->
        val visible = id == selectedThumb && (id != R.id.ringer_thumb_vibrate || data.hasVibrator)
        views.setViewVisibility(id, if (visible) View.VISIBLE else View.GONE)
    }
    RINGER_DOTS.forEach { id ->
        val visible = id != selectedDot && (id != R.id.ringer_dot_vibrate || data.hasVibrator)
        views.setViewVisibility(id, if (visible) View.VISIBLE else View.GONE)
        views.setBackgroundTint(id, dotTint)
        views.setFloat(id, "setAlpha", 0.4f)
    }
    RINGER_THUMB_BACKGROUNDS.forEach { id ->
        views.setInt(id, "setBackgroundResource", thumbBg)
        views.setBackgroundTint(id, activeBgTint)
    }
    RINGER_ICONS.forEach { id -> views.setInt(id, "setColorFilter", iconTint) }
}

private fun Context.applyRingerSliderClickActions(views: RemoteViews, widgetId: Int) {
    RINGER_NORMAL_CLICK_IDS.forEach { id ->
        setRingerClickAction(
            views,
            id,
            widgetId,
            AudioManager.RINGER_MODE_NORMAL,
            getString(R.string.ringer_normal),
        )
    }
    setRingerClickAction(
        views,
        R.id.ringer_click_vibrate,
        widgetId,
        AudioManager.RINGER_MODE_VIBRATE,
        getString(R.string.ringer_vibrate),
    )
    RINGER_SILENT_CLICK_IDS.forEach { id ->
        setRingerClickAction(
            views,
            id,
            widgetId,
            AudioManager.RINGER_MODE_SILENT,
            getString(R.string.ringer_silent),
        )
    }
}

private fun Context.setRingerClickAction(
    views: RemoteViews,
    viewId: Int,
    widgetId: Int,
    ringerMode: Int,
    contentDescription: String,
) {
    val pendingIntent =
        broadcastPendingIntent(
            requestCode = ringerRequestCode(widgetId, ringerMode),
            action = ACTION_TILE_CLICK,
            extras =
                mapOf(
                    EXTRA_WIDGET_ID to widgetId,
                    EXTRA_RINGER_MODE to ringerMode,
                ),
            receiverClass = AxTileReceiver::class.java,
        )
    views.setOnClickPendingIntent(viewId, pendingIntent)
    views.setContentDescription(viewId, contentDescription)
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

private fun Context.applyRingerSliderSizing(
    views: RemoteViews,
    widgetSize: SizeF,
    widgetId: Int,
) {
    val h = widgetSize.height
    if (h <= 0f) return

    val pillHeight = WidgetPrefs.getPillHeightDp(this, widgetId).toFloat()
    val pillWidthPref = WidgetPrefs.getPillWidthDp(this, widgetId).toFloat()
    val cellMaxWidth = if (widgetSize.width > 0f) widgetSize.width else Float.MAX_VALUE
    val targetWidth = pillWidthPref.coerceAtMost(cellMaxWidth)
    val verticalPadding =
        resources.getDimension(R.dimen.pill_widget_padding_vertical) /
            resources.displayMetrics.density
    val thumbSize = (pillHeight - verticalPadding * 2f).coerceAtLeast(0f)
    val iconSize = thumbSize * RINGER_ICON_RATIO

    views.setViewLayoutWidth(R.id.tile_view_root, targetWidth, TypedValue.COMPLEX_UNIT_DIP)
    views.setViewLayoutHeight(R.id.tile_view_root, pillHeight, TypedValue.COMPLEX_UNIT_DIP)
    RINGER_THUMBS.forEach { id ->
        views.setViewLayoutWidth(id, thumbSize, TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutHeight(id, thumbSize, TypedValue.COMPLEX_UNIT_DIP)
    }
    RINGER_ICONS.forEach { id ->
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

private fun selectedRingerThumb(ringerMode: Int?): Int =
    when (ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> R.id.ringer_thumb_vibrate
        AudioManager.RINGER_MODE_SILENT -> R.id.ringer_thumb_silent
        else -> R.id.ringer_thumb_normal
    }

private fun selectedRingerDot(ringerMode: Int?): Int =
    when (ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> R.id.ringer_dot_vibrate
        AudioManager.RINGER_MODE_SILENT -> R.id.ringer_dot_silent
        else -> R.id.ringer_dot_normal
    }

private fun visibleRingerMode(ringerMode: Int?, hasVibrator: Boolean): Int? =
    if (ringerMode == AudioManager.RINGER_MODE_VIBRATE && !hasVibrator) {
        AudioManager.RINGER_MODE_SILENT
    } else {
        ringerMode
    }

private fun ringerRequestCode(widgetId: Int, ringerMode: Int): Int =
    -((widgetId * RINGER_REQUEST_CODE_MULTIPLIER) + ringerMode + 1)

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

private fun RemoteViews.setBackgroundTint(viewId: Int, color: Int?) {
    setColorStateList(viewId, SET_BACKGROUND_TINT_LIST, color?.let(ColorStateList::valueOf))
}

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
