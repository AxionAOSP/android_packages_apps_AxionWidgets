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

package com.android.axion.widgets.di

import android.content.Context
import com.android.axion.widgets.cardlab.photo.PhotoProvider
import com.android.axion.widgets.cardlab.tile.TileManager
import com.android.axion.widgets.cardlab.tile.TileRepository
import com.android.axion.widgets.manager.QuickLookDataManager
import com.android.axion.widgets.platform.AxPlatformBridge
import com.android.axion.widgets.provider.BatteryStatusProvider
import com.android.axion.widgets.provider.CompassProvider
import com.android.axion.widgets.provider.DateProvider
import com.android.axion.widgets.provider.DozeStateProvider
import com.android.axion.widgets.provider.MediaPlayerProvider
import com.android.axion.widgets.provider.PedometerProvider
import com.android.axion.widgets.provider.QuickLookServiceClient
import com.android.axion.widgets.provider.UsageStatsProvider
import com.android.axion.widgets.quicklook.MessageProvider
import com.android.axion.widgets.quicklook.QuickLookWidgetInteractor
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.BINARY
import kotlinx.coroutines.*

@Module
@InstallIn(SingletonComponent::class)
object AxionModule {

    @Provides
    @Singleton
    fun provideAxPlatformBridge(@ApplicationContext context: Context): AxPlatformBridge =
        AxPlatformBridge(context)

    @Provides
    @Singleton
    fun provideBatteryStatusProvider(bridge: AxPlatformBridge): BatteryStatusProvider =
        BatteryStatusProvider(bridge)

    @Provides
    @Singleton
    fun provideQuickLookServiceClient(
        @ApplicationContext context: Context
    ): QuickLookServiceClient = QuickLookServiceClient(context)

    @Provides
    @Singleton
    fun provideMessageProvider(@ApplicationContext context: Context): MessageProvider =
        MessageProvider(context)

    @Provides
    @Singleton
    fun provideQuickLookDataManager(
        @ApplicationContext context: Context,
        messageProvider: MessageProvider,
    ): QuickLookDataManager = QuickLookDataManager(context, messageProvider)

    @Provides
    @Singleton
    fun provideQuickLookWidgetInteractor(
        @ApplicationContext context: Context,
        dataManager: QuickLookDataManager,
    ): QuickLookWidgetInteractor = QuickLookWidgetInteractor(context, dataManager)

    @Provides
    @Singleton
    fun providePhotoProvider(@ApplicationContext context: Context): PhotoProvider =
        PhotoProvider(context)

    @Provides
    @Singleton
    fun provideUsageStatsProvider(@ApplicationContext context: Context): UsageStatsProvider =
        UsageStatsProvider(context)

    @Provides
    @Singleton
    fun provideMediaPlayerProvider(
        @ApplicationContext context: Context,
        @MainScope scope: CoroutineScope,
    ): MediaPlayerProvider = MediaPlayerProvider(context, scope)

    @Provides
    @Singleton
    fun providePedometerProvider(@ApplicationContext context: Context): PedometerProvider =
        PedometerProvider(context)

    @Provides
    @Singleton
    fun provideCompassProvider(
        @ApplicationContext context: Context,
        @IoScope scope: CoroutineScope,
    ): CompassProvider = CompassProvider(context, scope)

    @Provides
    @Singleton
    fun provideDateProvider(@ApplicationContext context: Context): DateProvider =
        DateProvider(context)

    @Provides
    @Singleton
    fun provideDozeStateProvider(bridge: AxPlatformBridge): DozeStateProvider =
        DozeStateProvider(bridge)
}

@Module
@InstallIn(SingletonComponent::class)
object TileModule {

    @Provides
    @Singleton
    fun provideTileManager(
        @ApplicationContext context: Context,
        repository: TileRepository,
        bridge: AxPlatformBridge,
        @IoScope scope: CoroutineScope,
    ): TileManager = TileManager(context, repository, bridge, scope)

    @Provides
    @Singleton
    fun provideTileRepository(
        @ApplicationContext context: Context,
        @IoScope scope: CoroutineScope,
        bridge: AxPlatformBridge,
    ): TileRepository = TileRepository(context, scope, bridge)
}

@Singleton
@Component(modules = [AxionModule::class, CoroutineScopeModule::class, TileModule::class])
interface AxionAppComponent {

    fun quickLookWidgetInteractor(): QuickLookWidgetInteractor

    fun quickLookDataManager(): QuickLookDataManager

    fun tileRepository(): TileRepository

    fun tileManager(): TileManager

    fun messageProvider(): MessageProvider

    fun mediaPlayerProvider(): MediaPlayerProvider

    fun pedometerProvider(): PedometerProvider

    fun compassProvider(): CompassProvider

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance @ApplicationContext context: Context): AxionAppComponent
    }
}

@Qualifier @Retention(BINARY) annotation class MainScope

@Qualifier @Retention(BINARY) annotation class IoScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @Provides
    @Singleton
    @MainScope
    fun provideMainScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Provides
    @Singleton
    @IoScope
    fun provideIoScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
