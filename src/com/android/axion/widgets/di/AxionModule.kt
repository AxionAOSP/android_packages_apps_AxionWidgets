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
import com.android.axion.widgets.cardlab.tile.TileConfigs
import com.android.axion.widgets.cardlab.tile.TileManager
import com.android.axion.widgets.cardlab.tile.TileRepository
import com.android.axion.widgets.manager.QuickLookDataManager
import com.android.axion.widgets.provider.BatteryStatusProvider
import com.android.axion.widgets.provider.CalendarProvider
import com.android.axion.widgets.provider.MediaPlaybackProvider
import com.android.axion.widgets.provider.NotificationProvider
import com.android.axion.widgets.provider.UsageStatsProvider
import com.android.axion.widgets.provider.WeatherProvider
import com.android.axion.widgets.quicklook.QuickLookWidgetInteractor
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AxionModule {

    @Provides
    @Singleton
    fun provideBatteryStatusProvider(
        @ApplicationContext context: Context
    ): BatteryStatusProvider =
        BatteryStatusProvider(context)

    @Provides
    @Singleton
    fun provideCalendarProvider(
        @ApplicationContext context: Context
    ): CalendarProvider =
        CalendarProvider(context)

    @Provides
    @Singleton
    fun provideMediaPlaybackProvider(
        @ApplicationContext context: Context
    ): MediaPlaybackProvider =
        MediaPlaybackProvider(context)

    @Provides
    @Singleton
    fun provideNotificationProvider(
        @ApplicationContext context: Context
    ): NotificationProvider =
        NotificationProvider(context)

    @Provides
    @Singleton
    fun provideQuickLookDataManager(
        @ApplicationContext context: Context
    ): QuickLookDataManager =
        QuickLookDataManager(context)

    @Provides
    @Singleton
    fun provideQuickLookWidgetInteractor(
        @ApplicationContext context: Context,
        dataManager: QuickLookDataManager
    ): QuickLookWidgetInteractor =
        QuickLookWidgetInteractor(context, dataManager)
        
    @Provides
    @Singleton
    fun provideWeatherProvider(
        @ApplicationContext context: Context
    ): WeatherProvider =
        WeatherProvider(context)
        
    @Provides
    @Singleton
    fun providePhotoProvider(
        @ApplicationContext context: Context
    ): PhotoProvider =
        PhotoProvider(context)

    @Provides
    @Singleton
    fun provideUsageStatsProvider(
        @ApplicationContext context: Context
    ): UsageStatsProvider =
        UsageStatsProvider(context)
}

@Module
@InstallIn(SingletonComponent::class)
object TileModule {

    @Provides
    @Singleton
    fun provideTileManager(
        @ApplicationContext context: Context,
        repository: TileRepository,
        tileConfigs: TileConfigs
    ): TileManager =
        TileManager(context, repository, tileConfigs)

    @Provides
    @Singleton
    fun provideTileConfigs(
        @ApplicationContext context: Context
    ): TileConfigs =
        TileConfigs(context)

    @Provides
    @Singleton
    fun provideTileRepository(
        @ApplicationContext context: Context,
        tileConfigs: TileConfigs
    ): TileRepository =
        TileRepository(context, tileConfigs)
}

@Singleton
@Component(
    modules = [
        AxionModule::class,
        TileModule::class
    ]
)
interface AxionAppComponent {

    fun quickLookWidgetInteractor(): QuickLookWidgetInteractor
    fun quickLookDataManager(): QuickLookDataManager
    fun tileRepository(): TileRepository
    fun tileManager(): TileManager

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance @ApplicationContext context: Context
        ): AxionAppComponent
    }
}
