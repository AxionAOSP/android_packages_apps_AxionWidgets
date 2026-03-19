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

package com.android.axion.widgets.cardlab.countdown

import android.content.Context
import android.content.SharedPreferences

data class CountdownConfig(val eventName: String, val targetDateMillis: Long)

object CountdownPrefs {

    private const val PREFS_NAME = "countdown_prefs"
    private const val SUFFIX_NAME = "_name"
    private const val SUFFIX_DATE = "_date"

    private fun getPrefs(context: Context): SharedPreferences =
        context
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(context: Context, widgetId: Int): CountdownConfig? {
        val prefs = getPrefs(context)
        val name = prefs.getString("$widgetId$SUFFIX_NAME", null) ?: return null
        val date = prefs.getLong("$widgetId$SUFFIX_DATE", -1L)
        if (date == -1L) return null
        return CountdownConfig(name, date)
    }

    fun set(context: Context, widgetId: Int, config: CountdownConfig) {
        getPrefs(context)
            .edit()
            .putString("$widgetId$SUFFIX_NAME", config.eventName)
            .putLong("$widgetId$SUFFIX_DATE", config.targetDateMillis)
            .apply()
    }

    fun remove(context: Context, widgetId: Int) {
        getPrefs(context)
            .edit()
            .remove("$widgetId$SUFFIX_NAME")
            .remove("$widgetId$SUFFIX_DATE")
            .apply()
    }
}
