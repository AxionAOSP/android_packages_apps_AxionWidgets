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
package com.android.axion.widgets.quicklook

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.android.axion.widgets.SettingsActivity
import com.android.axion.widgets.data.QuickLookData

object QuickLookActions {

    private fun getPendingIntent(
        context: Context,
        intent: Intent
    ): PendingIntent {
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun getClickPendingIntent(
        context: Context,
        qlData: QuickLookData
    ): PendingIntent {
        val defaultIntent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val clickIntent = when (qlData) {
            is QuickLookData.CalendarEvent -> Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            is QuickLookData.Media -> {
                val packageName = qlData.packageName
                if (!packageName.isNullOrEmpty()) {
                    context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    } ?: defaultIntent
                } else {
                    defaultIntent
                }
            }
            is QuickLookData.Weather -> Intent().apply {
                setClassName("org.omnirom.omnijaws", "org.omnirom.omnijaws.SettingsActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            else -> defaultIntent
        }

        return getPendingIntent(context, clickIntent)
    }
    
    fun getDateClickPendingIntent(context: Context): PendingIntent? {
        val clickIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        return getPendingIntent(context, clickIntent)
    }
}
