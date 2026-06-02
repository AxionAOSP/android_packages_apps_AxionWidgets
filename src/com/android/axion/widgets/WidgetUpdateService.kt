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

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.os.Process
import android.os.UserManager
import com.android.axion.widgets.cardlab.AxBatteryReceiver
import com.android.axion.widgets.cardlab.AxYearProgressReceiver
import com.android.axion.widgets.cardlab.compass.AxCompassReceiver
import com.android.axion.widgets.cardlab.media.AxMediaPlayerReceiver
import com.android.axion.widgets.cardlab.pedometer.AxPedometerReceiver
import com.android.axion.widgets.cardlab.photo.AxPhotoReceiver
import com.android.axion.widgets.cardlab.photo.PhotoProvider
import com.android.axion.widgets.cardlab.screentime.AxScreenTimeReceiver
import com.android.axion.widgets.cardlab.tile.TileManager
import com.android.axion.widgets.cardlab.tile.TileRepository
import com.android.axion.widgets.data.PhotoWidgetData
import com.android.axion.widgets.di.IoScope
import com.android.axion.widgets.di.MainScope
import com.android.axion.widgets.manager.QuickLookDataManager
import com.android.axion.widgets.manager.WidgetUsageManager
import com.android.axion.widgets.provider.AodState
import com.android.axion.widgets.provider.BatteryStatusProvider
import com.android.axion.widgets.provider.CompassProvider
import com.android.axion.widgets.provider.DateProvider
import com.android.axion.widgets.provider.DozeStateProvider
import com.android.axion.widgets.provider.MediaPlayerProvider
import com.android.axion.widgets.provider.PedometerProvider
import com.android.axion.widgets.provider.QuickLookServiceClient
import com.android.axion.widgets.provider.UsageStatsProvider
import com.android.axion.widgets.quicklook.MessageProvider
import com.android.axion.widgets.quicklook.QuickLookPrefs
import com.android.axion.widgets.utils.Tracker
import com.android.axion.widgets.utils.WidgetFlows
import com.android.axion.widgets.utils.combinedCollect
import com.android.axion.widgets.utils.logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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

    private val serviceJob by lazy { SupervisorJob(scope.coroutineContext[Job]) }
    private val serviceScope by lazy { CoroutineScope(scope.coroutineContext + serviceJob) }
    private val managedReceivers by lazy { loadManagedReceivers() }
    private val managedReceiverClasses by lazy { managedReceivers.map { it::class.java } }

    private val photoCache = mutableMapOf<Int, PhotoWidgetData>()
    private var providersStarted = false

    internal val cachedPhotos: Collection<PhotoWidgetData>
        get() = photoCache.values

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

    private fun initProviders(): Boolean {
        if (providersStarted) return false
        providersStarted = true
        quickLookClient.bind()
        refreshWidgetUsage()
        startProviders()
        scheduleRefreshAllWidgets()
        if (QuickLookPrefs.isEnabled(applicationContext, QuickLookPrefs.SOURCE_MESSAGES)) {
            messageProvider.scheduleRotation()
        }
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE -> {
                if (!getSystemService(UserManager::class.java).isUserUnlocked) return START_STICKY
                if (!initProviders()) {
                    refreshWidgetUsage()
                    scheduleRefreshAllWidgets()
                }
                logger("Update requested from widget provider")
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (providersStarted) {
            scheduleRefreshAllWidgets()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        logger("WidgetUpdateService destroyed")
        serviceJob.cancel()
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
        serviceScope.combinedCollect(
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
            val d = data ?: return@combinedCollect
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

        serviceScope.launch {
            tileRepository.dataFlow.distinctUntilChanged().collect { tiles ->
                tiles?.let { tileManager.tilesFlow = it }
            }
        }

        serviceScope.launch {
            mediaPlayerProvider.dataFlow.distinctUntilChanged().collect { data ->
                AxMediaPlayerReceiver.update(applicationContext, data)
            }
        }

        pedometerProvider.start()
        serviceScope.launch {
            pedometerProvider.dataFlow.distinctUntilChanged().collect { data ->
                AxPedometerReceiver.update(applicationContext, data)
            }
        }

        compassProvider.start()
        serviceScope.launch {
            compassProvider.dataFlow.distinctUntilChanged().collect { data ->
                AxCompassReceiver.update(applicationContext, data)
            }
        }

        serviceScope.launch {
            dateProvider.dateFlow.collect {
                quickLookDataManager.onDataUpdated()
            }
        }

        if (AodState.DOZE_TRANSPARENCY_ENABLED) {
            serviceScope.launch {
                dozeStateProvider.dozeFlow.distinctUntilChanged().collect { state ->
                    val wasAod = AodState.isAod
                    AodState.isAod = state.isAod
                    if (wasAod != state.isAod) {
                        logger("AOD state changed: ${state.isAod}")
                        scheduleRefreshAllWidgets()
                    }
                }
            }
        }
    }

    private fun scheduleRefreshAllWidgets() {
        val ctx = applicationContext
        managedReceivers
            .filter { WidgetUsageManager.isActive(it::class.java) }
            .forEach { receiver ->
                serviceScope.launch(
                    CoroutineName("WidgetRefresh:${receiver::class.java.simpleName}")
                ) {
                    receiver.refresh(ctx, this@WidgetUpdateService)
                }
            }
    }

    private fun refreshWidgetUsage() =
        WidgetUsageManager.refreshAll(applicationContext, managedReceiverClasses)

    private fun loadManagedReceivers(): List<AxionWidgetProvider> =
        loadReceiverClassNames().mapNotNull(::instantiateReceiver)

    private fun loadReceiverClassNames(): List<String> =
        AppWidgetManager.getInstance(applicationContext)
            .installedProviders
            .asSequence()
            .map { it.provider }
            .filter { it.packageName == packageName }
            .map { it.className }
            .toList()

    private fun instantiateReceiver(className: String): AxionWidgetProvider? =
        try {
            Class.forName(className)
                .asSubclass(AxionWidgetProvider::class.java)
                .getDeclaredConstructor()
                .newInstance()
        } catch (e: ReflectiveOperationException) {
            logger("Failed to load widget receiver $className: ${e.message}")
            null
        } catch (e: ClassCastException) {
            logger("Failed to load widget receiver $className: ${e.message}")
            null
        }

    companion object {
        @Volatile var isRunning = false

        const val ACTION_UPDATE = "com.android.axion.widgets.ACTION_UPDATE"

        fun update(context: Context) {
            val intent =
                Intent(context, WidgetUpdateService::class.java).apply { action = ACTION_UPDATE }
            context.startService(intent)
        }
    }
}
