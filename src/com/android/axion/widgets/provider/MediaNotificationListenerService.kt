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
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import com.android.axion.widgets.utils.SafeCloseable
import com.android.axion.widgets.utils.Tracker
import com.android.axion.widgets.utils.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

class MediaNotificationListenerService : NotificationListenerService(), SafeCloseable {

    var notifProvider: NotificationProvider? = null
    
    private var lastNotifiedNotifications: List<StatusBarNotification> = emptyList()

    private val scope = MainScope()
    private val backgroundExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val notificationsMap = mutableMapOf<String, StatusBarNotification>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        scope.launch(backgroundExecutor) {
            notificationsMap[sbn.key] = sbn
            updateNotifications()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        scope.launch(backgroundExecutor) {
            notificationsMap.remove(sbn.key)
            updateNotifications()
        }
    }

    private suspend fun updateNotifications() {
        val currentNotifications = notificationsMap.values.toList()
        if (currentNotifications != lastNotifiedNotifications) {
            lastNotifiedNotifications = currentNotifications
            notifProvider?.onNotificationsChanged(currentNotifications)
            logger("notifications update! notifprovider available!!")
        }
    }

    private fun refreshNotificationsFromSystem() {
        scope.launch(backgroundExecutor) {
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
        logger("listener connected")
        Tracker.get().addCloseable(this)
        refreshNotificationsFromSystem()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        logger("listener disconnected")
    }

    override fun close() {
        scope.cancel()
    }
    
    companion object {
        var instance: MediaNotificationListenerService? = null
            private set
        val componentName: ComponentName by lazy {
            val javaClass = MediaNotificationListenerService::class.java
            ComponentName(javaClass.getPackage().name, javaClass.name)
        }
    }
}
