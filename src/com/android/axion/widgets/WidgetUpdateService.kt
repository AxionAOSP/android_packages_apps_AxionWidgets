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
import android.content.res.Configuration
import android.os.*
import android.os.UserManager
import com.android.axion.widgets.cardlab.AxBatteryReceiver
import com.android.axion.widgets.cardlab.AxYearProgressReceiver
import com.android.axion.widgets.cardlab.clock.AxAnalogClockReceiver
import com.android.axion.widgets.cardlab.clock.AxDigitalClockReceiver
import com.android.axion.widgets.cardlab.clock.AxWorldClockReceiver
import com.android.axion.widgets.cardlab.compass.AxCompassReceiver
import com.android.axion.widgets.cardlab.countdown.AxCountdownReceiver
import com.android.axion.widgets.cardlab.date.AxDateReceiver
import com.android.axion.widgets.cardlab.fidget.AxBottleSpinnerReceiver
import com.android.axion.widgets.cardlab.fidget.AxRpsReceiver
import com.android.axion.widgets.cardlab.media.AxMediaPlayerReceiver
import com.android.axion.widgets.cardlab.pedometer.AxPedometerReceiver
import com.android.axion.widgets.cardlab.photo.*
import com.android.axion.widgets.cardlab.screentime.*
import com.android.axion.widgets.cardlab.tile.*
import com.android.axion.widgets.data.*
import com.android.axion.widgets.di.*
import com.android.axion.widgets.manager.*
import com.android.axion.widgets.provider.*
import com.android.axion.widgets.quicklook.MessageProvider
import com.android.axion.widgets.quicklook.QuickLookPrefs
import com.android.axion.widgets.utils.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.Flow

interface AxionProvider<T> {
    val dataFlow: Flow<T?>
}

@AndroidEntryPoint(Service::class)
class WidgetUpdateService : Hilt_WidgetUpdateService() {

    @Inject @IoScope lateinit var scope: CoroutineScope

    @Inject @MainScope lateinit var mainScope: CoroutineScope

    @Inject lateinit var batteryProvider: BatteryStatusProvider
    @Inject lateinit var quickLookClient: QuickLookServiceClient
    @Inject lateinit var quickLookDataManager: QuickLookDataManager
    @Inject lateinit var tileRepository: TileRepository
    @Inject lateinit var tileManager: TileManager
    @Inject lateinit var photoProvider: PhotoProvider
    @Inject lateinit var usageStatsProvider: UsageStatsProvider
    @Inject lateinit var mediaPlayerProvider: MediaPlayerProvider
    @Inject lateinit var pedometerProvider: PedometerProvider
    @Inject lateinit var compassProvider: CompassProvider
    @Inject lateinit var dozeStateProvider: DozeStateProvider
    @Inject lateinit var messageProvider: MessageProvider
    @Inject lateinit var dateProvider: DateProvider

    private val photoCache = mutableMapOf<Int, PhotoWidgetData>()
    private var providersStarted = false

    override fun onCreate() {
        super.onCreate()

        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        Process.setThreadGroupAndCpuset(Process.myPid(), 9)
        Process.setProcessGroup(Process.myPid(), 9)

        if (isRunning) {
            logger("WidgetUpdateService already running, skipping onCreate")
            return
        }

        logger("WidgetUpdateService created")
        isRunning = true
        Tracker.get().scope = mainScope

        if (getSystemService(UserManager::class.java).isUserUnlocked) {
            initProviders()
        } else {
            logger("User not yet unlocked, deferring provider setup")
        }
    }

    private fun initProviders() {
        if (providersStarted) return
        providersStarted = true
        quickLookClient.bind()
        WidgetUsageManager.refreshAll(applicationContext, ALL_RECEIVERS)
        startProviders()
        if (QuickLookPrefs.isEnabled(applicationContext, QuickLookPrefs.SOURCE_MESSAGES)) {
            messageProvider.scheduleRotation()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch(Dispatchers.Default + CoroutineName("WidgetUpdateBackground")) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            Process.setThreadGroupAndCpuset(Process.myPid(), 9)
        }

        when (intent?.action) {
            ACTION_UPDATE -> {
                if (!getSystemService(UserManager::class.java).isUserUnlocked) return START_STICKY
                initProviders()
                logger("Update requested from widget provider")
                update()
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (providersStarted) {
            scope.launch { refreshAllWidgets() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        logger("WidgetUpdateService destroyed")
        scope.cancel()
        mainScope.cancel()
        quickLookClient.unbind()
        mediaPlayerProvider.stop()
        pedometerProvider.stop()
        compassProvider.stop()
        messageProvider.shutdown()

        Tracker.destroy()
        isRunning = false
        super.onDestroy()
    }

    private fun startProviders() {
        scope.combinedCollect(
            combinedFlow =
                WidgetFlows(
                    batteryProvider,
                    quickLookClient.calendarProvider,
                    quickLookClient.mediaProvider,
                    quickLookClient.weatherProvider,
                    tileRepository,
                    photoProvider,
                    usageStatsProvider,
                )
        ) { data ->
            data?.let { d ->
                AxBatteryReceiver.update(applicationContext, d.battery)
                AxScreenTimeReceiver.update(applicationContext, d.usage)
                AxYearProgressReceiver.update(applicationContext)
                d.photos?.let { photos ->
                    val activeIds = photos.map { it.widgetId }.toSet()
                    photoCache.keys.retainAll(activeIds)
                    photos.forEach { photo ->
                        AxPhotoReceiver.update(applicationContext, photo)
                        photoCache[photo.widgetId] = photo
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

        scope.launch {
            tileRepository.dataFlow.distinctUntilChanged().collect { tiles ->
                tiles?.let { tileManager.tilesFlow = it }
            }
        }

        scope.launch {
            mediaPlayerProvider.dataFlow.distinctUntilChanged().collect { data ->
                AxMediaPlayerReceiver.update(applicationContext, data)
            }
        }

        pedometerProvider.start()
        scope.launch {
            pedometerProvider.dataFlow.distinctUntilChanged().collect { data ->
                AxPedometerReceiver.update(applicationContext, data)
            }
        }

        compassProvider.start()
        scope.launch {
            compassProvider.dataFlow.distinctUntilChanged().collect { data ->
                AxCompassReceiver.update(applicationContext, data)
            }
        }

        scope.launch {
            dateProvider.dateFlow.collect {
                quickLookDataManager.onDataUpdated()
            }
        }

        if (AodState.DOZE_TRANSPARENCY_ENABLED) {
            scope.launch {
                dozeStateProvider.dozeFlow.distinctUntilChanged().collect { state ->
                    val wasAod = AodState.isAod
                    AodState.isAod = state.isAod
                    if (wasAod != state.isAod) {
                        logger("AOD state changed: ${state.isAod}")
                        refreshAllWidgets()
                    }
                }
            }
        }
    }

    private fun refreshAllWidgets() {
        val ctx = applicationContext
        fun active(cls: Class<out AxionWidgetProvider>) = WidgetUsageManager.isActive(cls)

        if (active(AxBatteryReceiver::class.java))
            scope.launch { AxBatteryReceiver.update(ctx, quickLookDataManager.batteryData) }
        if (active(AxScreenTimeReceiver::class.java))
            scope.launch { AxScreenTimeReceiver.update(ctx, null, true) }
        if (active(AxYearProgressReceiver::class.java))
            scope.launch { AxYearProgressReceiver.update(ctx) }
        if (active(AxDigitalClockReceiver::class.java))
            scope.launch { AxDigitalClockReceiver.update(ctx) }
        if (active(AxAnalogClockReceiver::class.java))
            scope.launch { AxAnalogClockReceiver.update(ctx) }
        if (active(AxWorldClockReceiver::class.java))
            scope.launch { AxWorldClockReceiver.update(ctx) }
        if (active(AxCountdownReceiver::class.java))
            scope.launch { AxCountdownReceiver.update(ctx) }
        if (active(AxDateReceiver::class.java)) scope.launch { AxDateReceiver.update(ctx) }
        if (active(AxMediaPlayerReceiver::class.java))
            scope.launch { AxMediaPlayerReceiver.update(ctx, mediaPlayerProvider.currentData) }
        if (active(AxPedometerReceiver::class.java))
            scope.launch { AxPedometerReceiver.update(ctx, null) }
        if (active(AxCompassReceiver::class.java))
            scope.launch { AxCompassReceiver.update(ctx, null) }
        if (active(AxTileReceiver::class.java))
            scope.launch {
                val tiles = tileManager.tilesFlow
                if (tiles.isNotEmpty()) {
                    tiles.values.forEach { data -> ctx.updateWidget(data.widgetId, data) }
                } else {
                    WidgetPrefs.getAllWidgetIds(ctx).forEach { widgetId ->
                        val spec = WidgetPrefs.getWidgetAction(ctx, widgetId) ?: return@forEach
                        tileManager.setTileForWidget(widgetId, spec)
                    }
                }
            }
        if (active(AxBottleSpinnerReceiver::class.java))
            scope.launch { AxBottleSpinnerReceiver.update(ctx) }
        if (active(AxRpsReceiver::class.java)) scope.launch { AxRpsReceiver.update(ctx) }
        if (active(AxPhotoReceiver::class.java))
            scope.launch {
                photoCache.values.forEach { photo -> AxPhotoReceiver.update(ctx, photo) }
            }
    }

    private fun update() {
        scope.launch(Dispatchers.IO + CoroutineName("WidgetUpdateIO")) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            Process.setThreadGroupAndCpuset(Process.myPid(), 9)
            refreshAllWidgets()
        }
    }

    companion object {
        @Volatile var isRunning = false

        const val ACTION_UPDATE = "com.android.axion.widgets.ACTION_UPDATE"

        val ALL_RECEIVERS: List<Class<out AxionWidgetProvider>> =
            listOf(
                AxBatteryReceiver::class.java,
                AxScreenTimeReceiver::class.java,
                AxYearProgressReceiver::class.java,
                AxDigitalClockReceiver::class.java,
                AxAnalogClockReceiver::class.java,
                AxWorldClockReceiver::class.java,
                AxCountdownReceiver::class.java,
                AxDateReceiver::class.java,
                AxMediaPlayerReceiver::class.java,
                AxPedometerReceiver::class.java,
                AxCompassReceiver::class.java,
                AxTileReceiver::class.java,
                AxPhotoReceiver::class.java,
                AxBottleSpinnerReceiver::class.java,
                AxRpsReceiver::class.java,
            )

        fun update(context: Context) {
            val intent =
                Intent(context, WidgetUpdateService::class.java).apply { action = ACTION_UPDATE }
            context.startService(intent)
        }
    }
}
