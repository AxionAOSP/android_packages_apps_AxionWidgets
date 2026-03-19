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

package com.android.axion.widgets.cardlab.pedometer

import android.content.Context
import android.content.SharedPreferences

object PedometerPrefs {

    private const val PREFS_NAME = "pedometer_prefs"
    private const val KEY_GOAL_PREFIX = "goal_"
    private const val KEY_BASELINE = "baseline"
    private const val KEY_BASELINE_DATE = "baseline_date"
    private const val KEY_DAILY_STEPS = "daily_steps"
    private const val DEFAULT_GOAL = 10000

    private fun getPrefs(context: Context): SharedPreferences =
        context
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getGoal(context: Context, widgetId: Int): Int =
        getPrefs(context).getInt("$KEY_GOAL_PREFIX$widgetId", DEFAULT_GOAL)

    fun setGoal(context: Context, widgetId: Int, goal: Int) {
        getPrefs(context).edit().putInt("$KEY_GOAL_PREFIX$widgetId", goal).apply()
    }

    fun removeWidget(context: Context, widgetId: Int) {
        getPrefs(context).edit().remove("$KEY_GOAL_PREFIX$widgetId").apply()
    }

    fun getBaseline(context: Context): Float = getPrefs(context).getFloat(KEY_BASELINE, -1f)

    fun getBaselineDate(context: Context): String =
        getPrefs(context).getString(KEY_BASELINE_DATE, "") ?: ""

    fun getDailySteps(context: Context): Int = getPrefs(context).getInt(KEY_DAILY_STEPS, 0)

    fun setBaseline(context: Context, baseline: Float, date: String) {
        getPrefs(context)
            .edit()
            .putFloat(KEY_BASELINE, baseline)
            .putString(KEY_BASELINE_DATE, date)
            .apply()
    }

    fun setDailySteps(context: Context, steps: Int) {
        getPrefs(context).edit().putInt(KEY_DAILY_STEPS, steps).apply()
    }
}
