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
import com.android.axion.widgets.quicklook.AxQuickLookReceiver
import com.android.axion.widgets.quicklook.MessageProvider
import com.android.axion.widgets.quicklook.QuickLookPrefs
import com.android.axion.widgets.utils.Updatable
import com.android.axion.widgets.utils.logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickLookDataManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val messageProvider: MessageProvider,
) {
    var batteryData by Updatable<QuickLookData.Battery>(::onDataUpdated)
    var calendarData by Updatable<QuickLookData.CalendarEvent>(::onDataUpdated)
    var mediaData by Updatable<QuickLookData.Media>(::onDataUpdated)
    var weatherData by Updatable<QuickLookData.Weather>(::onDataUpdated)

    val quickLookData: QuickLookData
        get() {
            val enabled = QuickLookPrefs.getEnabledSources(context)

            val cal =
                if (QuickLookPrefs.SOURCE_CALENDAR in enabled)
                    calendarData?.takeIf { isCalendarValid(it) }
                else null
            val media =
                if (QuickLookPrefs.SOURCE_MEDIA in enabled) mediaData?.takeIf { it.active }
                else null
            val battery =
                if (QuickLookPrefs.SOURCE_BATTERY in enabled) batteryData?.takeIf { it.isCharging }
                else null
            val weather = if (QuickLookPrefs.SOURCE_WEATHER in enabled) weatherData else null
            val message =
                if (QuickLookPrefs.SOURCE_MESSAGES in enabled)
                    QuickLookData.Message(messageProvider.getMessage())
                else null

            return cal ?: media ?: battery ?: weather ?: message ?: QuickLookData.Empty
        }

    private fun isCalendarValid(event: QuickLookData.CalendarEvent): Boolean {
        val now = System.currentTimeMillis()
        val lookaheadMs = QuickLookPrefs.getCalendarLookahead(context) * 60 * 1000L
        return event.endTime > now && event.startTime - now < lookaheadMs
    }

    fun onDataUpdated() {
        AxQuickLookReceiver.update(context, quickLookData)
        logger("onDataUpdated: $quickLookData")
    }

    companion object {
        fun get(context: Context): QuickLookDataManager {
            val app = context.applicationContext as AxionApp
            return app.appComponent.quickLookDataManager()
        }
    }
}
