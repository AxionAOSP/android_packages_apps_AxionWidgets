/*
 * Copyright (C) 2025-2026 AxionOS Project
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

package com.android.axion.widgets.quicklook

import android.content.Context

object QuickLookPrefs {

    private const val PREFS_NAME = "quicklook_prefs"

    const val SOURCE_CALENDAR = "calendar"
    const val SOURCE_MEDIA = "media"
    const val SOURCE_BATTERY = "battery"
    const val SOURCE_WEATHER = "weather"
    const val SOURCE_MESSAGES = "messages"

    private const val KEY_SHOW_DATE = "show_date"
    private const val KEY_DATE_FORMAT = "date_format"
    private const val KEY_CALENDAR_LOOKAHEAD = "cal_lookahead"
    private const val KEY_TEMP_UNIT = "temp_unit"
    private const val KEY_MSG_INTERVAL = "msg_interval"

    val ALL_SOURCES =
        listOf(SOURCE_CALENDAR, SOURCE_MEDIA, SOURCE_BATTERY, SOURCE_WEATHER, SOURCE_MESSAGES)

    private val DEFAULTS =
        mapOf(
            SOURCE_CALENDAR to true,
            SOURCE_MEDIA to true,
            SOURCE_BATTERY to true,
            SOURCE_WEATHER to true,
            SOURCE_MESSAGES to false,
        )

    const val DATE_FORMAT_DEFAULT = 0
    const val DATE_FORMAT_SHORT = 1
    const val DATE_FORMAT_FULL = 2

    const val LOOKAHEAD_15 = 15
    const val LOOKAHEAD_30 = 30
    const val LOOKAHEAD_60 = 60
    const val LOOKAHEAD_120 = 120

    private fun prefs(context: Context) =
        context
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context, source: String): Boolean =
        prefs(context).getBoolean("src_$source", DEFAULTS[source] ?: true)

    fun setEnabled(context: Context, source: String, enabled: Boolean) {
        prefs(context).edit().putBoolean("src_$source", enabled).apply()
    }

    fun getEnabledSources(context: Context): Set<String> =
        ALL_SOURCES.filter { isEnabled(context, it) }.toSet()

    fun showDate(context: Context): Boolean = prefs(context).getBoolean(KEY_SHOW_DATE, true)

    fun setShowDate(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_DATE, show).apply()
    }

    fun getDateFormat(context: Context): Int =
        prefs(context).getInt(KEY_DATE_FORMAT, DATE_FORMAT_DEFAULT)

    fun setDateFormat(context: Context, format: Int) {
        prefs(context).edit().putInt(KEY_DATE_FORMAT, format).apply()
    }

    fun getCalendarLookahead(context: Context): Int =
        prefs(context).getInt(KEY_CALENDAR_LOOKAHEAD, LOOKAHEAD_30)

    fun setCalendarLookahead(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_CALENDAR_LOOKAHEAD, minutes).apply()
    }

    fun getMessageInterval(context: Context): Int =
        prefs(context).getInt(KEY_MSG_INTERVAL, MSG_INTERVAL_1H)

    fun setMessageInterval(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_MSG_INTERVAL, minutes).apply()
    }

    const val MSG_INTERVAL_30M = 30
    const val MSG_INTERVAL_1H = 60
    const val MSG_INTERVAL_3H = 180
    const val MSG_INTERVAL_6H = 360
    const val MSG_INTERVAL_DAILY = 1440
}
