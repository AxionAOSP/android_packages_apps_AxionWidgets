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
package com.android.axion.widgets.provider

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.UsageData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AxionProvider<UsageData> {

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    override val dataFlow: Flow<UsageData?> = flow {
        val safeMillis = TimeUnit.HOURS.toMillis(8L) 

        while (true) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            if (stats.isNullOrEmpty()) {
                Log.w("UsageStatsProvider", "No usage stats available.")
                emit(null)
            } else {
                val totalTime = stats.sumOf { it.totalTimeInForeground }

                val hours = TimeUnit.MILLISECONDS.toHours(totalTime)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime) % 60
                val formatted = "${hours}H ${minutes}M"

                val percent = ((totalTime.toDouble() / safeMillis) * 100).toInt()
                    .coerceAtMost(100)

                emit(
                    UsageData(
                        totalTimeForeground = totalTime,
                        formatted = formatted,
                        level = percent
                    )
                )
            }

            delay(60_000)
        }
    }
}
