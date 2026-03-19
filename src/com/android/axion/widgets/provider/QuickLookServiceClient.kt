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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.text.format.DateFormat
import com.android.axion.quicklook.IAxQuickLookService
import com.android.axion.quicklook.IQuickLookCallback
import com.android.axion.quicklook.QuickLookTarget
import com.android.axion.quicklook.calendarData
import com.android.axion.quicklook.mediaData
import com.android.axion.quicklook.weatherData
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.utils.logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class QuickLookServiceClient @Inject constructor(@ApplicationContext private val context: Context) {
    private val SERVICE_ACTION = "com.android.axion.quicklook.SERVICE"
    private val SERVICE_PACKAGE = "com.android.axion.quicklook"

    private var service: IAxQuickLookService? = null
    private var bound = false

    private val _targetsFlow = MutableStateFlow<List<QuickLookTarget>>(emptyList())

    val weatherProvider =
        object : AxionProvider<QuickLookData.Weather> {
            override val dataFlow: Flow<QuickLookData.Weather?> =
                _targetsFlow.map { targets ->
                    targets
                        .firstOrNull { it.targetType == QuickLookTarget.TYPE_WEATHER }
                        ?.weatherData
                        ?.let { w ->
                            QuickLookData.Weather(
                                temp = w.temp,
                                condition = w.condition,
                                conditionCode = w.conditionCode,
                                iconBytes = w.iconBytes,
                            )
                        }
                }
        }

    val calendarProvider =
        object : AxionProvider<QuickLookData.CalendarEvent> {
            override val dataFlow: Flow<QuickLookData.CalendarEvent?> =
                _targetsFlow.map { targets ->
                    targets
                        .firstOrNull { it.targetType == QuickLookTarget.TYPE_CALENDAR }
                        ?.calendarData
                        ?.let { c ->
                            val formatter = DateFormat.getTimeFormat(context)
                            val start = formatter.format(Date(c.startTime))
                            val end = formatter.format(Date(c.endTime))
                            QuickLookData.CalendarEvent(
                                id = c.id,
                                title = c.title ?: "",
                                startTime = c.startTime,
                                endTime = c.endTime,
                                location = c.location ?: "",
                                desc = c.description.ifEmpty { "$start - $end" },
                            )
                        }
                }
        }

    val mediaProvider =
        object : AxionProvider<QuickLookData.Media> {
            override val dataFlow: Flow<QuickLookData.Media?> =
                _targetsFlow.map { targets ->
                    targets
                        .firstOrNull { it.targetType == QuickLookTarget.TYPE_MEDIA }
                        ?.mediaData
                        ?.let { m ->
                            QuickLookData.Media(
                                title = m.track,
                                artist = m.artist,
                                packageName = m.packageName,
                                active = m.isPlaying,
                            )
                        }
                }
        }

    private val callback =
        object : IQuickLookCallback.Stub() {
            override fun onTargetsUpdated(targets: MutableList<QuickLookTarget>) {
                _targetsFlow.value = targets.toList()
                this.logger("onTargetsUpdated: ${targets.size} targets")
            }
        }

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = IAxQuickLookService.Stub.asInterface(binder)
                service?.registerCallback(callback)
                val initial = service?.currentTargets
                if (initial != null) {
                    _targetsFlow.value = initial.toList()
                }
                this.logger("AxQuickLook service connected")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
                this.logger("AxQuickLook service disconnected, rebinding...")
                bind()
            }

            override fun onBindingDied(name: ComponentName?) {
                service = null
                bound = false
                this.logger("AxQuickLook binding died, rebinding...")
                bind()
            }
        }

    fun bind() {
        if (bound) return
        val intent = Intent(SERVICE_ACTION).apply { setPackage(SERVICE_PACKAGE) }
        bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        this.logger("AxQuickLook bind attempt: $bound")
    }

    fun unbind() {
        if (!bound) return
        service?.unregisterCallback(callback)
        context.unbindService(connection)
        service = null
        bound = false
    }
}
