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
import com.android.axion.widgets.platform.AxPlatformBridge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DozeState(
    val isDozing: Boolean = false,
    val dozeAmount: Float = 0f,
    val aodEnabled: Boolean = false,
) {
    val isAod: Boolean
        get() = isDozing && aodEnabled
}

object AodState {
    const val DOZE_TRANSPARENCY_ENABLED = false

    @Volatile var isAod: Boolean = false
        get() = DOZE_TRANSPARENCY_ENABLED && field
}

class DozeStateProvider(private val bridge: AxPlatformBridge) {

    val dozeFlow: Flow<DozeState> =
        bridge.stateFlow(AxPlatformClient.KEY_DOZE).map { bundle ->
            if (bundle.isEmpty) return@map DozeState()
            DozeState(
                isDozing = bundle.getBoolean("isDozing", false),
                dozeAmount = bundle.getFloat("dozeAmount", 0f),
                aodEnabled = bundle.getBoolean("aodEnabled", false),
            )
        }

    fun isCurrentlyDozing(): Boolean {
        val state = bridge.getState(AxPlatformClient.KEY_DOZE)
        if (state.isEmpty) return false
        val isDozing = state.getBoolean("isDozing", false)
        val aodEnabled = state.getBoolean("aodEnabled", false)
        return isDozing && aodEnabled
    }
}
