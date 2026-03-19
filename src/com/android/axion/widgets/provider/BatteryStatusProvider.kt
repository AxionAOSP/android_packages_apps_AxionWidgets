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

import com.android.axion.platform.AxPlatformClient
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.platform.AxPlatformBridge
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BatteryStatusProvider @Inject constructor(private val bridge: AxPlatformBridge) :
    AxionProvider<QuickLookData.Battery> {

    override val dataFlow: Flow<QuickLookData.Battery?> =
        bridge.stateFlow(AxPlatformClient.KEY_BATTERY).map { bundle ->
            if (bundle.isEmpty) return@map null
            val level = bundle.getInt("level", -1)
            val isCharging = bundle.getBoolean("isCharging", false)
            val isPluggedIn = bundle.getBoolean("isPluggedIn", false)
            val estimateStr = bundle.getString("batteryTimeRemainingEstimate", "")
            val timeRemaining = estimateStr?.toLongOrNull()
            QuickLookData.Battery(
                isCharging = isCharging || isPluggedIn,
                level = level,
                chargingTimeRemaining = timeRemaining,
            )
        }
}
