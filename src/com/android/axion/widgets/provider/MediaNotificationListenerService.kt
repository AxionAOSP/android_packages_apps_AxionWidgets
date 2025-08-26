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
import com.android.axion.widgets.manager.QuickLookDataManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

class MediaNotificationListenerService : NotificationListenerService() {

    private val coroutineScope = MainScope()
    private val backgroundExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val notificationsMap = mutableMapOf<String, StatusBarNotification>()
    private val _notificationsFlow = MutableStateFlow<List<StatusBarNotification>>(emptyList())
    val notificationsFlow = _notificationsFlow.asStateFlow()

    private val dataManager: QuickLookDataManager by lazy {
        QuickLookDataManager.get(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        coroutineScope.launch(backgroundExecutor) {
            notificationsMap[sbn.key] = sbn
            updateNotifications()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        coroutineScope.launch(backgroundExecutor) {
            notificationsMap.remove(sbn.key)
            updateNotifications()
        }
    }

    private suspend fun updateNotifications() {
        val currentNotifications = notificationsMap.values.toList()
        try {
            dataManager.updateNotifications(currentNotifications)
        } catch (_: Exception) {}
        withContext(Dispatchers.Main) {
            _notificationsFlow.value = currentNotifications
        }
    }

    private fun refreshNotificationsFromSystem() {
        coroutineScope.launch(backgroundExecutor) {
            val activeMap = runCatching {
                activeNotifications?.associateBy { it.key }
            }.getOrNull() ?: emptyMap()

            notificationsMap.clear()
            notificationsMap.putAll(activeMap)
            updateNotifications()
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
