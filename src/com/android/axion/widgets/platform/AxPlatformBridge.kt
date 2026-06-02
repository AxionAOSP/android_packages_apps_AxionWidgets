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

package com.android.axion.widgets.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.axion.platform.AxFeatureState
import com.android.axion.platform.AxPlatformClient
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart

@Singleton
class AxPlatformBridge @Inject constructor(context: Context) {

    companion object {
        private const val TAG = "AxPlatformBridge"
    }

    private val client = AxPlatformClient.getInstance().also { it.init(context) }
    private val handler = Handler(Looper.getMainLooper())
    private val callbackExecutor = Executor { command -> handler.post(command) }
    private val stateFlows = mutableMapOf<String, MutableSharedFlow<AxFeatureState>>()
    private var registered = false

    private val callback =
        AxPlatformClient.StateCallback { key, state ->
            getOrCreateFlow(key).tryEmit(state)
        }

    fun connect() {
        if (registered) return
        client.registerCallback(callbackExecutor, callback)
        registered = true
        Log.d(TAG, "Connected to AxPlatform service")
    }

    fun stateFlow(key: String): Flow<AxFeatureState> {
        connect()
        return getOrCreateFlow(key).onStart {
            val cached = client.getState(key)
            if (!cached.isEmpty) emit(cached)
        }
    }

    fun getState(key: String): AxFeatureState = client.getState(key)

    fun toggle(feature: String) = client.toggle(feature)

    fun setValue(feature: String, value: Int) = client.setValue(feature, value)

    fun getSupportedFeatures(): Array<String> = client.getSupportedFeatures()

    private fun getOrCreateFlow(key: String): MutableSharedFlow<AxFeatureState> {
        return stateFlows.getOrPut(key) {
            MutableSharedFlow(
                replay = 1,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
    }
}
