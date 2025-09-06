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
import android.os.Process
import android.service.notification.StatusBarNotification
import com.android.axion.widgets.AxionProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AxionProvider<List<StatusBarNotification>> {

    private val _notificationFlow = MutableStateFlow<List<StatusBarNotification>>(emptyList())
    override val dataFlow: Flow<List<StatusBarNotification>?> = _notificationFlow.asStateFlow()

    fun onNotificationsChanged(notifications: List<StatusBarNotification>) {
        _notificationFlow.value = notifications
    }
}
