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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Tracker private constructor() : SafeCloseable {

    private val closeables = mutableListOf<SafeCloseable>()
    private var closed = false

    var scope :CoroutineScope? = null

    companion object {
        @Volatile
        private var INSTANCE: Tracker? = null

        fun get(): Tracker {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Tracker().also { INSTANCE = it }
            }
        }

        fun destroy() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }

    fun addCloseable(closeable: SafeCloseable) {
        scope?.launch {
            if (closed) {
                closeable.close()
                logger("Closed immediately -> ${closeable::class.simpleName}@${closeable.hashCode()}")
            } else {
                closeables.add(closeable)
                logger("Adding closeable -> ${closeable::class.simpleName}@${closeable.hashCode()}")
                logger("Total closeables now -> ${closeables.size}: ${closeables.joinToString { it::class.simpleName + "@" + it.hashCode() }}")
            }
        }
    }

    fun removeCloseable(closeable: SafeCloseable) {
        scope?.launch {
            if (closeables.remove(closeable)) {
                closeable.close()
                logger("Removed closeable -> ${closeable::class.simpleName}@${closeable.hashCode()}")
                logger("All closeables cleared. Total now -> ${closeables.size}")
            }
        }
    }

    override fun close() {
        closed = true
        for (i in closeables.size - 1 downTo 0) {
            val item = closeables[i]
            runCatching { item.close() }
                .onSuccess { logger("Closed -> ${item::class.simpleName}@${item.hashCode()}") }
                .onFailure { logger("Failed to close -> ${item::class.simpleName}@${item.hashCode()} : ${it.message}") }
        }
        closeables.clear()
        logger("All closeables cleared. Total now -> ${closeables.size}")
        INSTANCE = null
        logger("Tracker instance reset! goodbye world!")
    }
}
