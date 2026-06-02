/*
 * Copyright (C) 2026 AxionOS Project
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

enum class ClockBackground {
    TRANSLUCENT,
    TRANSPARENT
}

object ClockPrefs {

    private const val PREFS_NAME = "clock_prefs"
    private const val SUFFIX_BG = "_bg"

    private fun getPrefs(context: Context): SharedPreferences =
        context
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBackground(context: Context, widgetId: Int): ClockBackground {
        val prefs = getPrefs(context)
        val bgStr = prefs.getString("$widgetId$SUFFIX_BG", null) ?: return ClockBackground.TRANSLUCENT
        return try {
            ClockBackground.valueOf(bgStr)
        } catch (e: Exception) {
            ClockBackground.TRANSLUCENT
        }
    }

    fun setBackground(context: Context, widgetId: Int, bg: ClockBackground) {
        getPrefs(context)
            .edit()
            .putString("$widgetId$SUFFIX_BG", bg.name)
            .apply()
    }

    fun remove(context: Context, widgetId: Int) {
        getPrefs(context)
            .edit()
            .remove("$widgetId$SUFFIX_BG")
            .apply()
    }
}
