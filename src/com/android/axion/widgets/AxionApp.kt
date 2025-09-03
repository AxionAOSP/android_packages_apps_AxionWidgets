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
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.axion.widgets.di.AxionAppComponent
import com.android.axion.widgets.di.DaggerAxionAppComponent
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp(Application::class)
class AxionApp : Hilt_AxionApp() {

    lateinit var appComponent: AxionAppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAxionAppComponent.factory().create(this)
    }
}
