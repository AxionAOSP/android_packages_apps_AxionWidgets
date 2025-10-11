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

import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.*
import com.android.axion.widgets.manager.WidgetUsageManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*

data class WidgetDatas(
    val battery: BatteryData?,
    val calendar: CalendarData?,
    val media: MediaData?,
    val weather: WeatherData?,
    val tiles: TilesData?,
    val photos: PhotoWidgetDataList?,
    val usage: UsageData?
)

inline fun <T1, T2, T3, T4, T5, T6, T7, R> combineFlows(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> = combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { args ->
    @Suppress("UNCHECKED_CAST")
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7
    )
}

fun <T> AxionProvider<T>.requiredFlow(): Flow<T?> =
    WidgetUsageManager.isProviderRequired(this::class).flatMapLatest { required ->
        if (required) this.dataFlow else flowOf(null)
    }

fun WidgetFlows(
    batteryProvider: AxionProvider<BatteryData>,
    calendarProvider: AxionProvider<CalendarData>,
    mediaProvider: AxionProvider<MediaData>,
    weatherProvider: AxionProvider<WeatherData>,
    tileRepository: AxionProvider<TilesData>,
    photoProvider: AxionProvider<PhotoWidgetDataList>,
    usageStatsProvider: AxionProvider<UsageData>
): Flow<WidgetDatas> {

    return combineFlows(
        batteryProvider.requiredFlow(),
        calendarProvider.requiredFlow(),
        mediaProvider.requiredFlow(),
        weatherProvider.requiredFlow(),
        tileRepository.requiredFlow(),
        photoProvider.requiredFlow(),
        usageStatsProvider.requiredFlow()
    ) { battery, calendar, media, weather, tiles, photos, usage ->
        WidgetDatas(
            battery = battery,
            calendar = calendar,
            media = media,
            weather = weather,
            tiles = tiles,
            photos = photos,
            usage = usage
        )
    }
}

fun CoroutineScope.combinedCollect(
    combinedFlow: Flow<WidgetDatas>,
    action: (WidgetDatas?) -> Unit
) {
    val collector = object : SafeCloseable {
        private var job: Job? = null

        override fun close() {
            job?.cancel()
            job = null
        }

        fun start(scope: CoroutineScope) {
            job = scope.launch {
                combinedFlow
                    .distinctUntilChanged()
                    .catch { e -> logger("combinedCollect error: $e") }
                    .collect { data -> action(data) }
            }
        }
    }

    collector.start(this)
    Tracker.get().addCloseable(collector)
}
