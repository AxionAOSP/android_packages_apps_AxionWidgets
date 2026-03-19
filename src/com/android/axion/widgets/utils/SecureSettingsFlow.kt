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

package com.android.axion.widgets.utils

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun Context.secureSettingsFlow(key: String): Flow<String?> =
    callbackFlow {
            val uri = Settings.Secure.getUriFor(key)

            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean, changedUri: Uri?) {
                        trySend(Settings.Secure.getString(contentResolver, key))
                    }
                }

            contentResolver.registerContentObserver(uri, false, observer)
            trySend(Settings.Secure.getString(contentResolver, key))
            awaitClose { contentResolver.unregisterContentObserver(observer) }
        }
        .distinctUntilChanged()

fun Context.secureIntFlow(key: String, default: Int = 0): Flow<Int> =
    callbackFlow {
            val uri = Settings.Secure.getUriFor(key)

            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean, changedUri: Uri?) {
                        trySend(Settings.Secure.getInt(contentResolver, key, default))
                    }
                }

            contentResolver.registerContentObserver(uri, false, observer)
            trySend(Settings.Secure.getInt(contentResolver, key, default))
            awaitClose { contentResolver.unregisterContentObserver(observer) }
        }
        .distinctUntilChanged()
