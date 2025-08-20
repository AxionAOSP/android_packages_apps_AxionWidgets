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

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.android.axion.widgets.di.QuickLookWidgetEntryPoint
import com.android.axion.widgets.manager.QuickLookDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.EntryPointAccessors

class MediaNotificationListenerService : NotificationListenerService() {

    private val coroutineScope: CoroutineScope = MainScope()
    private val notificationsMap = mutableMapOf<String, StatusBarNotification>()
    private val _notificationsFlow = MutableStateFlow<List<StatusBarNotification>>(emptyList())
    val notificationsFlow = _notificationsFlow.asStateFlow()

    private val dataManager: QuickLookDataManager by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            QuickLookWidgetEntryPoint::class.java
        ).quickLookDataManager()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        notificationsMap[sbn.key] = sbn
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        notificationsMap.remove(sbn.key)
        updateNotifications()
    }

    private fun updateNotifications() {
        val currentNotifications = notificationsMap.values.toList()
        _notificationsFlow.value = currentNotifications
        try {
            dataManager.updateNotifications(currentNotifications)
        } catch (_: Exception) {}
    }

    private fun refreshNotificationsFromSystem() {
        coroutineScope.launch(Dispatchers.IO) {
            val activeMap = runCatching {
                activeNotifications?.associateBy { it.key }
            }.getOrNull() ?: emptyMap()
            withContext(Dispatchers.Main) {
                notificationsMap.clear()
                notificationsMap.putAll(activeMap)
                updateNotifications()
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshNotificationsFromSystem()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) {
            instance = null
        }
    }

    companion object {
        @Volatile
        private var instance: MediaNotificationListenerService? = null
        fun getInstance(): MediaNotificationListenerService? = instance
    }
}
