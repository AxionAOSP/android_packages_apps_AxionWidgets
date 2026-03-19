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
import com.android.axion.widgets.AxionApp
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.CompassData
import com.android.axion.widgets.di.IoScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

@Singleton
class CompassProvider
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @IoScope private val scope: CoroutineScope,
) : AxionProvider<CompassData>, SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val _rawAzimuth = MutableStateFlow<CompassData?>(null)
    private val _data = MutableStateFlow<CompassData?>(null)
    override val dataFlow: Flow<CompassData?> = _data

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var lastAzimuth = -999f
    private var registered = false

    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> registerSensor()
                    Intent.ACTION_SCREEN_OFF -> unregisterSensor()
                }
            }
        }

    @OptIn(FlowPreview::class)
    fun start() {
        if (rotationSensor == null) return
        registerSensor()
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        context.registerReceiver(screenReceiver, filter)

        scope.launch { _rawAzimuth.sample(333).collect { data -> _data.value = data } }
    }

    fun stop() {
        unregisterSensor()
        runCatching { context.unregisterReceiver(screenReceiver) }
    }

    private fun registerSensor() {
        if (registered || rotationSensor == null) return
        sensorManager?.registerListener(this, rotationSensor, 200_000)
        registered = true
    }

    private fun unregisterSensor() {
        if (!registered) return
        sensorManager?.unregisterListener(this)
        registered = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val normalizedAzimuth = ((azimuth % 360) + 360) % 360

        if (abs(normalizedAzimuth - lastAzimuth) < 2f) return
        lastAzimuth = normalizedAzimuth

        _rawAzimuth.value = CompassData(normalizedAzimuth)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        fun get(context: Context): CompassProvider {
            val app = context.applicationContext as AxionApp
            return app.appComponent.compassProvider()
        }
    }
}
