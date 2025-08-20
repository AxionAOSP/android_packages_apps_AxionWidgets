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
import com.android.axion.widgets.cardlab.tile.*
import com.android.axion.widgets.manager.*
import com.android.axion.widgets.quicklook.QuickLookWidgetInteractor
import com.android.axion.widgets.provider.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AxionModule {

    @Provides
    @Singleton
    fun provideBatteryDataManager(@ApplicationContext context: Context, batteryStatusProvider: BatteryStatusProvider): BatteryDataManager {
        return BatteryDataManager(context, batteryStatusProvider)
    }

    @Provides
    @Singleton
    fun provideBatteryStatusProvider(@ApplicationContext context: Context): BatteryStatusProvider {
        return BatteryStatusProvider(context)
    }

    @Provides
    @Singleton
    fun provideWeatherProvider(@ApplicationContext context: Context): WeatherProvider {
        return WeatherProvider(context)
    }

    @Provides
    @Singleton
    fun provideCalendarProvider(@ApplicationContext context: Context): CalendarProvider {
        return CalendarProvider(context)
    }

    @Provides
    @Singleton
    fun provideMediaPlaybackProvider(@ApplicationContext context: Context): MediaPlaybackProvider {
        return MediaPlaybackProvider(context)
    }

    @Provides
    @Singleton
    fun provideQuickLookDataManager(
        @ApplicationContext context: Context,
        mediaProvider: MediaPlaybackProvider,
        weatherProvider: WeatherProvider,
        calendarProvider: CalendarProvider,
        batteryDataManager: BatteryDataManager
    ): QuickLookDataManager {
        return QuickLookDataManager(context, mediaProvider, weatherProvider, calendarProvider, batteryDataManager)
    }

    @Provides
    @Singleton
    fun provideQuickLookWidgetInteractor(
        @ApplicationContext context: Context,
        dataManager: QuickLookDataManager
    ): QuickLookWidgetInteractor {
        return QuickLookWidgetInteractor(context, dataManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object TileModule {

    @Provides
    @Singleton
    fun provideTileRepository(@ApplicationContext context: Context): TileRepository {
        return TileRepository(context)
    }

    @Provides
    @Singleton
    fun provideTileManager(
        @ApplicationContext context: Context,
        repository: TileRepository
    ): TileManager {
        return TileManager(context, repository)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QuickLookWidgetEntryPoint {
    fun quickLookWidgetInteractor(): QuickLookWidgetInteractor
    fun quickLookDataManager(): QuickLookDataManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BatteryWidgetEntryPoint {
    fun batteryDataManager(): BatteryDataManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TileWidgetEntryPoint {
    fun tileRepository(): TileRepository
    fun tileManager(): TileManager
}
