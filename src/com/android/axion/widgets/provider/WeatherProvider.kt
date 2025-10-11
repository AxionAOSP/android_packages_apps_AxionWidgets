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
package com.android.axion.widgets.provider

import android.content.Context
import android.provider.Settings
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.QuickLookData
import com.android.internal.util.android.OmniJawsClient
import com.android.axion.widgets.utils.callbackFlow
import com.android.axion.widgets.utils.logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CoroutineScope
) : AxionProvider<QuickLookData.Weather> {

    override val dataFlow: Flow<QuickLookData.Weather?> = callbackFlow(
        initial = null,
        scope = scope,
        register = { OmniJawsClient.get().addObserver(context, it) },
        unregister = { OmniJawsClient.get().removeObserver(context, it) },
        createCallback = { emit ->
            object : OmniJawsClient.OmniJawsObserver {
                override fun weatherUpdated() {
                    val qlEnabled = isQuicklookEnabled()
                    val omniEnabled = OmniJawsClient.get().isOmniJawsEnabled(context)
                    if (!qlEnabled || !omniEnabled) {
                        this.logger("weather not enabled! omniEnabled: $omniEnabled qlEnabled: $qlEnabled")
                        emit(null)
                        return
                    }
                    OmniJawsClient.get().queryWeather(context)
                    val info = OmniJawsClient.get().weatherInfo
                    val weather = info?.run { QuickLookData.Weather(temp, condition, conditionCode) }
                    this.logger("weather updated! weather: $weather info: $info")
                    emit(weather)
                }

                override fun weatherError(errorReason: Int) {
                    emit(null)
                }
            }
        },
        onCallbackCreated = { (it as OmniJawsClient.OmniJawsObserver).weatherUpdated() }
    )

    private fun isQuicklookEnabled(): Boolean =
        Settings.Secure.getInt(context.contentResolver, "nt_quicklook_weather", 1) == 1
}
