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
package com.android.axion.widgets.cardlab.photo

import android.content.SharedPreferences
import android.appwidget.AppWidgetManager
import android.widget.RemoteViews
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PhotoTracker(
    private val interactor: PhotoInteractor,
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(interactor.context)
) {
    private val mutex = Mutex()
    private var shuffleJob: Job? = null
    var boundWidgetIds: List<Int> = emptyList()

    private val shufflePositions = mutableMapOf<Int, Int>()

    private val prefs = interactor.context.getSharedPreferences("photo_widget_prefs", android.content.Context.MODE_PRIVATE)
    private var listening = false

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith("shuffle_interval_")) {
            if (boundWidgetIds.isNotEmpty()) {
                val carouselWidgetIds = boundWidgetIds.filter { interactor.getImageUris(it).size > 1 }
                updateCarousels(carouselWidgetIds)
            }
        }
    }

    fun bind(widgetIds: List<Int>) {
        boundWidgetIds = widgetIds.distinct()
        val photoCardWidgetIds = boundWidgetIds.filter { interactor.getImageUris(it).size == 1 }
        val carouselWidgetIds = boundWidgetIds.filter { interactor.getImageUris(it).size > 1 }
        if (photoCardWidgetIds.isNotEmpty()) {
            updatePhotoCards(photoCardWidgetIds)
        }
        updateCarousels(carouselWidgetIds)
    }

    private fun updateCarousels(carouselWidgetIds: List<Int>) {
        stopListening()
        shuffleJob?.cancel()
        shuffleJob = null
        if (carouselWidgetIds.isNotEmpty()) {
            startListening()
            startPolling(carouselWidgetIds)
        }
    }

    private fun startPolling(carouselWidgetIds: List<Int>) {
        val intervals = carouselWidgetIds.map { interactor.loadShuffleInterval(it) }
        val interval = intervals.minOrNull() ?: 3_600_000L
        shuffleJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                mutex.withLock {
                    updateCarouselWidgets(carouselWidgetIds)
                }
                delay(interval)
            }
        }
    }

    private fun updatePhotoCards(widgetIds: List<Int>) {
        widgetIds.forEach { appWidgetId ->
            val uris = interactor.getImageUris(appWidgetId)
            if (uris.size == 1) {
                val bitmap = interactor.loadBitmapFromUri(uris[0])
                val finalBitmap = if (interactor.loadGrayscalePref(appWidgetId)) {
                    bitmap?.let { interactor.toGrayscale(it) }
                } else {
                    bitmap
                }
                val views = interactor.updateWidget(appWidgetId, finalBitmap)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private suspend fun updateCarouselWidgets(widgetIds: List<Int>) {
        widgetIds.forEach { appWidgetId ->
            val views = getNextRemoteViews(appWidgetId)
            withContext(Dispatchers.Main) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun startListening() {
        if (!listening) {
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)
            listening = true
        }
    }

    private fun stopListening() {
        if (listening) {
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            listening = false
        }
    }

    fun dispose() {
        shuffleJob?.cancel()
        shuffleJob = null
        boundWidgetIds = emptyList()
        shufflePositions.clear()
        stopListening()
    }

    private fun getNextRemoteViews(appWidgetId: Int): RemoteViews {
        val uris = interactor.getImageUris(appWidgetId)
        if (uris.isEmpty()) return interactor.updateWidget(appWidgetId, null)
        val size = uris.size
        var position = shufflePositions.getOrDefault(appWidgetId, -1)
        position = (position + 1) % size
        shufflePositions[appWidgetId] = position
        val nextUri = uris[position]
        val bitmap = interactor.loadBitmapFromUri(nextUri)
        val finalBitmap = if (interactor.loadGrayscalePref(appWidgetId)) {
            bitmap?.let { interactor.toGrayscale(it) }
        } else {
            bitmap
        }
        return interactor.updateWidget(appWidgetId, finalBitmap)
    }
}
