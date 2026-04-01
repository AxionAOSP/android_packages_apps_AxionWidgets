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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.android.axion.widgets.AxionApp
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.cardlab.pedometer.PedometerPrefs
import com.android.axion.widgets.data.PedometerData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class PedometerProvider @Inject constructor(@ApplicationContext private val context: Context) :
    AxionProvider<PedometerData>, SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val _data = MutableStateFlow<PedometerData?>(null)
    override val dataFlow: Flow<PedometerData?> = _data

    private var registered = false
    private var lastPersistTime = 0L

    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> registerSensor()
                    Intent.ACTION_SCREEN_OFF -> unregisterSensor()
                }
            }
        }

    fun start() {
        if (stepSensor == null) return

        val persistedSteps = PedometerPrefs.getDailySteps(context)
        val today = LocalDate.now().toString()
        if (PedometerPrefs.getBaselineDate(context) == today && persistedSteps > 0) {
            _data.value = PedometerData(persistedSteps)
        }

        registerSensor()
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        context.registerReceiver(screenReceiver, filter)
    }

    fun stop() {
        unregisterSensor()
        runCatching { context.unregisterReceiver(screenReceiver) }
    }

    private fun registerSensor() {
        if (registered || stepSensor == null) return
        sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        registered = true
    }

    private fun unregisterSensor() {
        if (!registered) return
        sensorManager?.unregisterListener(this)
        registered = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val rawSteps = event.values[0]
        val today = LocalDate.now().toString()

        val baselineDate = PedometerPrefs.getBaselineDate(context)
        val storedBaseline = PedometerPrefs.getBaseline(context)

        val dailySteps: Int
        if (baselineDate != today) {
            PedometerPrefs.setBaseline(context, rawSteps, today)
            PedometerPrefs.setOffset(context, 0)
            PedometerPrefs.setDailySteps(context, 0)
            dailySteps = 0
        } else if (storedBaseline < 0 || rawSteps < storedBaseline) {
            val carryOver = PedometerPrefs.getDailySteps(context)
            PedometerPrefs.setBaseline(context, rawSteps, today)
            PedometerPrefs.setOffset(context, carryOver)
            dailySteps = carryOver
        } else {
            val offset = PedometerPrefs.getOffset(context)
            dailySteps = offset + (rawSteps - storedBaseline).toInt().coerceAtLeast(0)
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastPersistTime > 10_000) {
            PedometerPrefs.setDailySteps(context, dailySteps)
            lastPersistTime = now
        }
        _data.value = PedometerData(dailySteps)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        fun get(context: Context): PedometerProvider {
            val app = context.applicationContext as AxionApp
            return app.appComponent.pedometerProvider()
        }
    }
}
