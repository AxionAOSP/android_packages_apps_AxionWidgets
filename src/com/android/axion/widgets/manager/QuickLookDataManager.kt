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
package com.android.axion.widgets.manager

import android.content.*
import com.android.axion.widgets.AxionApp
import com.android.axion.widgets.data.*
import com.android.axion.widgets.quicklook.QuickLookWidgetReceiver
import com.android.axion.widgets.utils.logger
import com.android.axion.widgets.utils.Updatable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickLookDataManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    var batteryData by Updatable<QuickLookData.Battery>(::onDataUpdated)
    var calendarData by Updatable<QuickLookData.CalendarEvent>(::onDataUpdated)
    var mediaData by Updatable<QuickLookData.Media>(::onDataUpdated)
    var weatherData by Updatable<QuickLookData.Weather>(::onDataUpdated)

    val quickLookData: QuickLookData get() {
        return calendarData?.takeIf { CalendarSimpleData.isEventValid(context, it) }
            ?: mediaData
            ?: batteryData?.takeIf { it.isCharging }
            ?: weatherData
            ?: QuickLookData.Empty
    }

    fun onDataUpdated() {
        QuickLookWidgetReceiver.update(context, quickLookData)
        logger("onDataUpdated: ${quickLookData} batteryData: ${batteryData} calendarData: ${calendarData} mediaData: ${mediaData} weatherData: ${weatherData}")
    }
    
    companion object {
        fun get(context: Context): QuickLookDataManager {
            val app = context.applicationContext as AxionApp
            return app.appComponent.quickLookDataManager()
        }
    }
}
