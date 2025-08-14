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

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setWidgetAction(context: Context, widgetId: Int, tileType: String) {
        getPrefs(context).edit()
            .putString(widgetId.toString(), tileType)
            .apply()
    }

    fun getWidgetAction(context: Context, widgetId: Int): String? {
        return getPrefs(context).getString(widgetId.toString(), null)
    }

    fun removeWidget(context: Context, widgetId: Int) {
        getPrefs(context).edit()
            .remove(widgetId.toString())
            .apply()
    }
    
    fun getAllWidgetIds(context: Context): List<Int> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .all.keys.mapNotNull { it.toIntOrNull() }
    }
}
