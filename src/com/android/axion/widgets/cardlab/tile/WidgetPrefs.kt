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

import android.content.Context
import android.content.SharedPreferences

object WidgetPrefs {

    private const val PREFS_NAME = "tile_widget_prefs"

    const val DEFAULT_TILE_SIZE_DP = 72
    const val DEFAULT_PILL_HEIGHT_DP = 72
    const val DEFAULT_PILL_WIDTH_DP = 171
    const val DEFAULT_PILL_AUTOFIT = true

    const val MIN_TILE_SIZE_DP = 40
    const val MAX_TILE_SIZE_DP = 120
    const val MIN_PILL_HEIGHT_DP = 40
    const val MAX_PILL_HEIGHT_DP = 120
    const val MIN_PILL_WIDTH_DP = 80
    const val MAX_PILL_WIDTH_DP = 400

    private const val KEY_TILE_SIZE_PREFIX = "tile_size_dp_"
    private const val KEY_PILL_HEIGHT_PREFIX = "pill_h_dp_"
    private const val KEY_PILL_WIDTH_PREFIX = "pill_w_dp_"
    private const val KEY_PILL_AUTOFIT_PREFIX = "pill_autofit_"

    private fun getPrefs(context: Context): SharedPreferences =
        context
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setWidgetAction(context: Context, widgetId: Int, tileType: String) {
        getPrefs(context).edit().putString(widgetId.toString(), tileType).apply()
    }

    fun getWidgetAction(context: Context, widgetId: Int): String? {
        return getPrefs(context).getString(widgetId.toString(), null)
    }

    fun setTileSizeDp(context: Context, widgetId: Int, dp: Int) {
        getPrefs(context)
            .edit()
            .putInt(KEY_TILE_SIZE_PREFIX + widgetId, dp.coerceIn(MIN_TILE_SIZE_DP, MAX_TILE_SIZE_DP))
            .apply()
    }

    fun getTileSizeDp(context: Context, widgetId: Int): Int =
        getPrefs(context).getInt(KEY_TILE_SIZE_PREFIX + widgetId, DEFAULT_TILE_SIZE_DP)

    fun setPillHeightDp(context: Context, widgetId: Int, dp: Int) {
        getPrefs(context)
            .edit()
            .putInt(
                KEY_PILL_HEIGHT_PREFIX + widgetId,
                dp.coerceIn(MIN_PILL_HEIGHT_DP, MAX_PILL_HEIGHT_DP),
            )
            .apply()
    }

    fun getPillHeightDp(context: Context, widgetId: Int): Int =
        getPrefs(context).getInt(KEY_PILL_HEIGHT_PREFIX + widgetId, DEFAULT_PILL_HEIGHT_DP)

    fun setPillWidthDp(context: Context, widgetId: Int, dp: Int) {
        getPrefs(context)
            .edit()
            .putInt(
                KEY_PILL_WIDTH_PREFIX + widgetId,
                dp.coerceIn(MIN_PILL_WIDTH_DP, MAX_PILL_WIDTH_DP),
            )
            .apply()
    }

    fun getPillWidthDp(context: Context, widgetId: Int): Int =
        getPrefs(context).getInt(KEY_PILL_WIDTH_PREFIX + widgetId, DEFAULT_PILL_WIDTH_DP)

    fun setPillAutoFitWidth(context: Context, widgetId: Int, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PILL_AUTOFIT_PREFIX + widgetId, enabled).apply()
    }

    fun isPillAutoFitWidth(context: Context, widgetId: Int): Boolean =
        getPrefs(context).getBoolean(KEY_PILL_AUTOFIT_PREFIX + widgetId, DEFAULT_PILL_AUTOFIT)

    fun removeWidget(context: Context, widgetId: Int) {
        getPrefs(context)
            .edit()
            .remove(widgetId.toString())
            .remove(KEY_TILE_SIZE_PREFIX + widgetId)
            .remove(KEY_PILL_HEIGHT_PREFIX + widgetId)
            .remove(KEY_PILL_WIDTH_PREFIX + widgetId)
            .remove(KEY_PILL_AUTOFIT_PREFIX + widgetId)
            .apply()
    }

    fun getAllWidgetIds(context: Context): List<Int> {
        return getPrefs(context).all.keys.mapNotNull { it.toIntOrNull() }
    }
}
