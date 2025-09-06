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
package com.android.axion.widgets

import android.app.*
import android.app.Service
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.android.axion.widgets.cardlab.BatteryWidgetReceiver
import com.android.axion.widgets.cardlab.tile.*
import com.android.axion.widgets.data.*
import com.android.axion.widgets.manager.*
import com.android.axion.widgets.provider.*
import com.android.axion.widgets.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

interface AxionProvider<T> { val dataFlow: kotlinx.coroutines.flow.Flow<T?> }

@AndroidEntryPoint(Service::class)
class WidgetUpdateService : Hilt_WidgetUpdateService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject lateinit var batteryProvider: BatteryStatusProvider
    @Inject lateinit var calendarProvider: CalendarProvider
    @Inject lateinit var mediaProvider: MediaPlaybackProvider
    @Inject lateinit var notificationProvider: NotificationProvider
    @Inject lateinit var weatherProvider: WeatherProvider
    @Inject lateinit var quickLookDataManager: QuickLookDataManager
    @Inject lateinit var tileRepository: TileRepository
    @Inject lateinit var tileManager: TileManager

    lateinit var notifService: MediaNotificationListenerService

    private val activeFlow = MutableStateFlow(true)
    private var isScreenOn = true
    private var onLauncher = true
    private var stackRegistered = false

    private val taskListener = object : TaskStackListener() {
        override fun onTaskStackChanged() = checkFocusedTask()
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    updateWidgetsState()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    checkFocusedTask()
                }
            }
        }
    }

    var fgServiceEnabled by Updatable<Boolean> { enabled ->
        if (enabled == true) {
            startForeground(1002, buildNotification())
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    var notifListenerEnabled by Updatable<Boolean> { enabled ->
        runCatching {
            if (enabled == true) {
                notifService = MediaNotificationListenerService()
                notifService.notifProvider = notificationProvider
                notifService.registerAsSystemService(applicationContext, MediaNotificationListenerService.componentName, UserHandle.USER_ALL)
                logger("enable notification listener")
            } else {
                notifService.unregisterAsSystemService()
                logger("disabled notification listener")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        logger("WidgetUpdateService created")
        fgServiceEnabled = true
        notifListenerEnabled = true
        isRunning = true
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        if (!stackRegistered) {
            runCatching {
                ActivityTaskManager.getService().registerTaskStackListener(taskListener)
                stackRegistered = true
            }
        }

        startProviders()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        logger("WidgetUpdateService destroyed")
        Tracker.destroy()
        notifListenerEnabled = false
        fgServiceEnabled = false
        
        runCatching {
            unregisterReceiver(screenReceiver)
            if (stackRegistered) {
                ActivityTaskManager.getService().unregisterTaskStackListener(taskListener)
            }
        }

        scope.cancel()
        isRunning = false
        super.onDestroy()
    }

    fun startProviders() {
        scope.collect(batteryProvider, activeFlow) { battery ->
            BatteryWidgetReceiver.update(applicationContext, battery)
            quickLookDataManager.batteryData = battery
            logger("battery update: $battery")
        }

        scope.collect(calendarProvider, activeFlow) { calendar ->
            quickLookDataManager.calendarData = calendar
            logger("calendarData update: $calendar")
        }

        scope.collect(mediaProvider, activeFlow) { media ->
            quickLookDataManager.mediaData = media
            logger("mediaData update $media")
        }

        scope.collect(notificationProvider, activeFlow) { notifications ->
            notifications?.let { 
                scope.launch(Dispatchers.Main) { mediaProvider.updateNotifications(it) }
            }
        }

        scope.collect(weatherProvider, activeFlow) { weather ->
            quickLookDataManager.weatherData = weather
            logger("weather update $weather")
        }

        scope.collect(tileRepository, activeFlow) { tiles ->
            tiles?.let { scope.launch(Dispatchers.Main) { tileManager.tilesFlow = it } }
            logger("tiles update $tiles")
        }
    }

    fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel("widgets_service", "Widget Service", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(channel)
        nm.setNotificationListenerAccessGrantedForUser(MediaNotificationListenerService.componentName, UserHandle.USER_CURRENT, true)
        val notification = NotificationCompat.Builder(this, "widgets_service")
            .setContentTitle("Widget Service Running")
            .setContentText("Updating widgets in real-time")
            .setSmallIcon(R.drawable.ic_unknown)
            .setOngoing(true)
            .build()
        return notification
    }

    private fun checkFocusedTask() {
        val topPackage = try {
            ActivityTaskManager.getService().getFocusedRootTaskInfo()?.topActivity?.packageName
        } catch (e: RemoteException) {
            null
        }
        onLauncher = topPackage == "com.android.launcher3"
        updateWidgetsState()
    }

    private fun updateWidgetsState() {
        val active = isScreenOn && onLauncher
        activeFlow.value = active
    }

    companion object {
        @Volatile
        var isRunning = false
    }
}
