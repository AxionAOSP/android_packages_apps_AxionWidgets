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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.SystemProperties
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun RemoteViews.setTextOrHide(viewId: Int, text: String?) {
    if (text.isNullOrEmpty()) {
        setViewVisibility(viewId, View.GONE)
    } else {
        setViewVisibility(viewId, View.VISIBLE)
        setTextViewText(viewId, text)
    }
}

fun RemoteViews.setIconOrHide(viewId: Int, bitmap: Bitmap?) {
    if (bitmap != null) {
        setViewVisibility(viewId, View.VISIBLE)
        setImageViewBitmap(viewId, bitmap)
    } else {
        setViewVisibility(viewId, View.GONE)
    }
}

inline fun <reified T> T.logger(msg: String) {
    val DEBUG = SystemProperties.getBoolean("persist.sys.axion_widgets_debug", false)
    if (DEBUG) Log.d(T::class.java.simpleName + ": AxLogger", msg)
}

inline fun <reified T, Callback> callbackFlow(
    initial: T? = null,
    crossinline register: (Callback) -> Unit,
    crossinline unregister: (Callback) -> Unit,
    crossinline createCallback: (emit: (T?) -> Unit) -> Callback,
    crossinline onCallbackCreated: (Callback) -> Unit = {}
): Flow<T?> = flow {
    var last: T? = null
    emit(initial)

    val flow = kotlinx.coroutines.flow.callbackFlow<T?> {
        val emitFn: (T?) -> Unit = { value ->
            trySend(value).isSuccess
        }

        val callback: Callback = createCallback(emitFn)
        register(callback)
        onCallbackCreated(callback)

        val safeCloseable = object : SafeCloseable {
            override fun close() {
                try { unregister(callback) } catch (_: Exception) {}
            }
        }

        Tracker.get().addCloseable(safeCloseable)

        awaitClose { unregister(callback) }
    }

    flow.collect { value ->
        if (value != last) {
            last = value
            emit(value)
        }
    }
}

fun <T> Context.broadcastFlow(
    filter: IntentFilter,
    parseIntent: (Intent) -> T
): Flow<T> = flow {
    var last: T? = null

    val flow = kotlinx.coroutines.flow.callbackFlow<T> {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                trySend(parseIntent(intent)).isSuccess
            }
        }

        val stickyIntent = registerReceiver(receiver, filter)
        stickyIntent?.let { trySend(parseIntent(it)).isSuccess }

        val safeCloseable = object : SafeCloseable {
            override fun close() {
                try { unregisterReceiver(receiver) } catch (_: Exception) {}
            }
        }

        Tracker.get().addCloseable(safeCloseable)

        awaitClose { try { unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    flow.collect { value ->
        if (value != last) {
            last = value
            emit(value)
        }
    }
}

class Updatable<T>(
    private val onChanged: (T?) -> Unit
) : ReadWriteProperty<Any?, T?> {

    constructor(onChanged: () -> Unit) : this({ _ -> onChanged() })

    private var backing: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? = backing

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        if (value != backing) {
            backing = value
            onChanged(value)
        }
    }
}

fun <T> CoroutineScope.collect(
    provider: AxionProvider<T>,
    activeFlow: StateFlow<Boolean>,
    action: (T?) -> Unit
) {
    var collectJob: Job? = null

    val collector = object : SafeCloseable {
        override fun close() {
            collectJob?.cancel()
            collectJob = null
        }
    }

    val mainJob = launch {
        activeFlow.collect { active ->
            if (active) {
                if (collectJob == null || collectJob?.isCancelled == true) {
                    collectJob = launch {
                        provider.dataFlow.collect { action(it) }
                    }
                }
                logger("activeflow is active!!! collecting!")
            } else {
                Tracker.get().removeCloseable(collector)
                logger("activeflow is not active! time to sleep!!")
            }
        }
    }

    Tracker.get().addCloseable(collector)
    Tracker.get().addCloseable(object : SafeCloseable {
        override fun close() {
            mainJob.cancel()
            Tracker.get().removeCloseable(collector)
        }
    })
}
