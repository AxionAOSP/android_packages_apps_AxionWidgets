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

package com.android.axion.widgets.cardlab.clock

import android.content.Context
import android.content.SharedPreferences

data class WorldClockConfig(val timezoneId: String, val cityName: String)

object WorldClockPrefs {

    private const val PREFS_NAME = "world_clock_prefs"
    private const val SUFFIX_TZ = "_tz"
    private const val SUFFIX_CITY = "_city"

    private fun getPrefs(context: Context): SharedPreferences =
        context
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(context: Context, widgetId: Int): WorldClockConfig? {
        val prefs = getPrefs(context)
        val tz = prefs.getString("$widgetId$SUFFIX_TZ", null) ?: return null
        val city = prefs.getString("$widgetId$SUFFIX_CITY", tz) ?: tz
        return WorldClockConfig(tz, city)
    }

    fun set(context: Context, widgetId: Int, config: WorldClockConfig) {
        getPrefs(context)
            .edit()
            .putString("$widgetId$SUFFIX_TZ", config.timezoneId)
            .putString("$widgetId$SUFFIX_CITY", config.cityName)
            .apply()
    }

    fun remove(context: Context, widgetId: Int) {
        getPrefs(context)
            .edit()
            .remove("$widgetId$SUFFIX_TZ")
            .remove("$widgetId$SUFFIX_CITY")
            .apply()
    }
}
