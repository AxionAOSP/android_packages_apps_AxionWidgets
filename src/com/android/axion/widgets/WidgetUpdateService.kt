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
import com.android.axion.widgets.cardlab.BatteryWidgetReceiver
import com.android.axion.widgets.cardlab.screentime.*
import com.android.axion.widgets.cardlab.tile.*
import com.android.axion.widgets.cardlab.photo.*
import com.android.axion.widgets.data.*
import com.android.axion.widgets.di.*
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

    @Inject @IoScope
    lateinit var scope: CoroutineScope

    @Inject @MainScope
    lateinit var mainScope: CoroutineScope

    @Inject lateinit var batteryProvider: BatteryStatusProvider
    @Inject lateinit var calendarProvider: CalendarProvider
    @Inject lateinit var mediaProvider: MediaPlaybackProvider
    @Inject lateinit var weatherProvider: WeatherProvider
    @Inject lateinit var quickLookDataManager: QuickLookDataManager
    @Inject lateinit var tileRepository: TileRepository
    @Inject lateinit var tileManager: TileManager
    @Inject lateinit var photoProvider: PhotoProvider
    @Inject lateinit var usageStatsProvider: UsageStatsProvider
    
    private var usageData: UsageData? = null
    private var photodSmall: PhotoWidgetData? = null
    private var photodLarge: PhotoWidgetData? = null

    lateinit var notifService: MediaNotificationListenerService

    var notifListenerEnabled by Updatable<Boolean> { enabled ->
        runCatching {
            if (enabled == true) registerNotifService()
            else unregisterNotifService()
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (isRunning) {
            logger("WidgetUpdateService already running, skipping onCreate")
            return
        }
        
        logger("WidgetUpdateService created")
        isRunning = true
        Tracker.get().scope = mainScope
        notifListenerEnabled = true
        WidgetUsageManager.refreshAll(applicationContext)
        startProviders()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            logger("Service not initialized, initializing now")
            onCreate()
        }
        
        when (intent?.action) {
            ACTION_UPDATE -> {
                logger("Update requested from widget provider")
                update()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        logger("WidgetUpdateService destroyed")
        Tracker.destroy()
        notifListenerEnabled = false
        scope.cancel()
        mainScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    private fun startProviders() {
        scope.combinedCollect(
            combinedFlow = WidgetFlows(
                batteryProvider,
                calendarProvider,
                mediaProvider,
                weatherProvider,
                tileRepository,
                photoProvider,
                usageStatsProvider
            )
        ) { data ->
            data?.let { d ->
                BatteryWidgetReceiver.update(applicationContext, d.battery)
                ScreenTimeWidgetReceiver.update(applicationContext, d.usage)
                d.photos?.forEach { photo ->
                    when (photo.size) {
                        1 -> {
                            PhotoWidgetSmallReceiver.update(applicationContext, photo)
                            photodSmall = photo
                        }
                        2 -> {
                            PhotoWidgetLargeReceiver.update(applicationContext, photo)
                            photodLarge = photo
                        }
                    }
                }
                
                quickLookDataManager.apply {
                    batteryData = d.battery
                    calendarData = d.calendar
                    mediaData = d.media
                    weatherData = d.weather
                }

                d.tiles?.let { tileManager.tilesFlow = it }
            }
        }
    }

    fun registerNotifService() {
        notifService = MediaNotificationListenerService()
        notifService.scope = mainScope
        notifService.mediaProvider = mediaProvider
        notifService.registerAsSystemService(applicationContext, MediaNotificationListenerService.componentName, UserHandle.USER_ALL)
        logger("enable notification listener")
    }
    
    fun unregisterNotifService() {
        notifService.unregisterAsSystemService()
        logger("disabled notification listener")
    }

    private fun update() {
        scope.launch {
            BatteryWidgetReceiver.update(applicationContext, quickLookDataManager.batteryData)
            ScreenTimeWidgetReceiver.update(applicationContext, usageData, true)
            photodSmall?.let {
                PhotoWidgetSmallReceiver.update(applicationContext, it)
                logger("photo update: widgetId=${it.widgetId} ${it.size}")
            }
            photodLarge?.let {
                PhotoWidgetLargeReceiver.update(applicationContext, it)
                logger("photo update: widgetId=${it.widgetId} ${it.size}")
            }
        }
    }

    companion object {
        @Volatile
        var isRunning = false
        
        const val ACTION_UPDATE = "com.android.axion.widgets.ACTION_UPDATE"

        fun update(context: Context) {
            val intent = Intent(context, WidgetUpdateService::class.java).apply {
                action = ACTION_UPDATE
            }
            context.startService(intent)
        }
    }
}
