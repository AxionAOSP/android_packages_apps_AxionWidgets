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
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaSession
import com.android.axion.widgets.data.MediaNotification
import com.android.axion.widgets.data.MediaNotifications
import com.android.axion.widgets.provider.MediaPlaybackProvider
import com.android.axion.widgets.utils.logger
import kotlinx.coroutines.*
import java.util.concurrent.Executors

class MediaNotificationListenerService : NotificationListenerService() {

    var mediaProvider: MediaPlaybackProvider? = null
    var scope: CoroutineScope? = null

    private val mediaNotifications = mutableMapOf<String, MediaNotification>()
    private var lastMediaNotifs: MediaNotifications = emptyList()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        scope?.launch {
            val mediaNotif = MediaNotification(
                key = sbn.key,
                token = sbn.notification.extras.getParcelable("android.mediaSession")
            )
            mediaNotifications[sbn.key] = mediaNotif
            update()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        scope?.launch {
            mediaNotifications.remove(sbn.key)
            update()
        }
    }

    private suspend fun update() {
        val list = mediaNotifications.values.toList()
        if (list != lastMediaNotifs) {
            lastMediaNotifs = list
            mediaProvider?.onMediaUpdate(list)
            logger("media notifications updated!")
        }
    }

    private fun refresh() {
        scope?.launch {
            val activeMap = runCatching {
                activeNotifications?.associateBy { it.key }?.mapValues { (_, sbn) ->
                    MediaNotification(
                        key = sbn.key,
                        token = sbn.notification.extras.getParcelable("android.mediaSession")
                    )
                } ?: emptyMap()
            }.getOrNull() ?: emptyMap()

            mediaNotifications.clear()
            mediaNotifications.putAll(activeMap)
            update()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        logger("Listener connected")
        refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        logger("Listener disconnected")
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
