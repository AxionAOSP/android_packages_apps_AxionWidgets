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

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.android.axion.widgets.AxionWidgetProvider
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.manager.QuickLookDataManager
import com.android.axion.widgets.utils.logger
import com.android.axion.widgets.provider.BatteryStatusProvider
import com.android.axion.widgets.provider.CalendarProvider
import com.android.axion.widgets.provider.MediaPlaybackProvider
import com.android.axion.widgets.provider.WeatherProvider

class QuickLookWidgetReceiver : AxionWidgetProvider() {

    override fun requiredProviders() = listOf(
        BatteryStatusProvider::class,
        CalendarProvider::class,
        MediaPlaybackProvider::class,
        WeatherProvider::class
    )

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        logger("intent received. intent: ${intent}")
        val qldata = QuickLookDataManager.get(context).quickLookData
        update(context, qldata)
    }

    companion object {
        fun update(context: Context, data: QuickLookData) {
            val views = QuickLookWidgetInteractor.get(context).updateRemoteViews(data)
            AxionWidgetProvider.updateAllWidgets(
                context,
                QuickLookWidgetReceiver::class.java,
                views
            )
            this.logger("updated ${data}")
        }
    }
}
