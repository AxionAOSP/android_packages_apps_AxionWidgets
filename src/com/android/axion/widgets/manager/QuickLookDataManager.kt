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
import android.os.Process
import android.provider.CalendarContract
import android.service.notification.StatusBarNotification
import com.android.axion.widgets.callback.*
import com.android.axion.widgets.data.*
import com.android.axion.widgets.provider.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickLookDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaProvider: MediaPlaybackProvider,
    private val weatherProvider: WeatherProvider,
    private val calendarProvider: CalendarProvider,
    private val batteryDataManager: BatteryDataManager
) {

    private val listeners = mutableSetOf<QuickLookDataCallback>()
    private val coroutineScope: CoroutineScope = MainScope()

    private var latestWeather: QuickLookData.Weather? = null
    private var latestMedia: QuickLookData.Media? = null
    private var latestCalendar: QuickLookData.CalendarEvent? = null
    private var latestBattery: QuickLookData.Battery? = null

    private var mediaFlowJob: Job? = null

    private var notificationListenerStarted = false

    private val weatherCallback = object : WeatherProvider.Callback {
        override fun onWeatherUpdated(data: NTWeatherData) {
            val weatherData = QuickLookData.Weather(data.temp, data.condition, data.conditionCode)
            val newWeather = weatherData.takeIf { data != NTWeatherData.EMPTY }
            if (latestWeather != newWeather) {
                latestWeather = newWeather
                notifyListeners()
            }
        }
    }

    private val calendarCallback = object : CalendarProvider.Callback {
        override fun onCalendarDataChanged(data: CalendarSimpleData?) {
            val calendar = data?.toCalendarEvent()
            if (latestCalendar != calendar) {
                latestCalendar = calendar
                notifyListeners()
            }
        }
    }

    init {
        weatherProvider.addCallback(weatherCallback)
        calendarProvider.addCallback(calendarCallback)
        batteryDataManager.batteryFlow
            .onEach { battery ->
                val batteryInfo = battery?.takeIf { it.isCharging }
                if (latestBattery != batteryInfo) {
                    latestBattery = batteryInfo
                    notifyListeners()
                }
            }
            .launchIn(coroutineScope)
        mediaFlowJob?.cancel()
        mediaFlowJob = mediaProvider.mediaFlow
            .onEach { media ->
                latestMedia = media
                notifyListeners()
            }
            .launchIn(coroutineScope)
        startNotificationListener()
        observeNotificationFlow()
    }

    private fun startNotificationListener() {
        if (notificationListenerStarted) return
        try {
            val componentName = ComponentName(context, MediaNotificationListenerService::class.java)
            MediaNotificationListenerService().registerAsSystemService(
                context, componentName, Process.myUid()
            )
            notificationListenerStarted = true
        } catch (e: Exception) {
        }
    }

    private fun observeNotificationFlow() {
        val service = MediaNotificationListenerService.getInstance() ?: return
        service.notificationsFlow
            .onEach { notifications ->
                updateNotifications(notifications)
            }
            .launchIn(coroutineScope)
    }

    fun updateNotifications(notifications: List<StatusBarNotification>) {
        mediaProvider.updateNotifications(notifications)
    }

    fun addListener(listener: QuickLookDataCallback) {
        listeners.add(listener)
        listener.onDataUpdated()
    }

    fun removeListener(listener: QuickLookDataCallback) {
        listeners.remove(listener)
    }

    fun notifyListeners() {
        listeners.forEach { it.onDataUpdated() }
    }

    fun getQuickLookData(): QuickLookData {
        latestCalendar = latestCalendar?.takeIf { isEventValid(it) }
        return latestCalendar
            ?: latestMedia
            ?: latestBattery
            ?: latestWeather
            ?: QuickLookData.Empty
    }

    private fun isEventValid(event: QuickLookData.CalendarEvent): Boolean {
        val now = System.currentTimeMillis()
        if (event.endTime <= now) return false
        return context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf("deleted"),
            "_id = ?",
            arrayOf(event.id.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val deletedIndex = cursor.getColumnIndex("deleted")
                val deleted = if (deletedIndex != -1) cursor.getInt(deletedIndex) else 0
                deleted == 0
            } else false
        } ?: false
    }

    fun cleanup() {
        weatherProvider.removeCallback(weatherCallback)
        calendarProvider.removeCallback(calendarCallback)
        mediaFlowJob?.cancel()
        mediaProvider.cleanup()
        listeners.clear()
    }

    fun getcontext(): Context = context
}
