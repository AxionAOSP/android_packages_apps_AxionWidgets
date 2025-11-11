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

import android.app.Application
import android.content.Intent
import android.os.Process
import android.os.UserHandle
import android.util.Log
import com.android.axion.widgets.di.AxionAppComponent
import com.android.axion.widgets.di.DaggerAxionAppComponent
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp(Application::class)
class AxionApp : Hilt_AxionApp() {

    private val TAG = "AxionApp"

    lateinit var appComponent: AxionAppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")
        appComponent = DaggerAxionAppComponent.factory().create(this)
        startWidgetUpdateService()
        Process.setThreadAffinity(Process.myPid(), 1)
    }

    private fun startWidgetUpdateService() {
        if (WidgetUpdateService.isRunning) {
            Log.d(TAG, "WidgetUpdateService already running")
            return
        }
        
        try {
            val intent = Intent(this, WidgetUpdateService::class.java)
            startServiceAsUser(intent, UserHandle.CURRENT)
            Log.i(TAG, "WidgetUpdateService started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WidgetUpdateService", e)
        }
    }
}
