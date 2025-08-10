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

import android.content.Context
import android.media.MediaMetadata
import android.media.session.*
import android.service.notification.StatusBarNotification
import com.android.axion.widgets.data.*
import kotlinx.coroutines.flow.*

class MediaPlaybackProvider(
    private val context: Context
) : MediaController.Callback() {

    private val mediaControllers = mutableListOf<MediaControllerSession>()
    private var activeController: MediaControllerSession? = null

    private var lastPlaybackState: PlaybackState? = null

    private var lastMedia: QuickLookData.Media? = null

    private val _mediaFlow = MutableStateFlow<QuickLookData.Media?>(null)
    val mediaFlow = _mediaFlow.asStateFlow()

    private val isPlaying: Boolean
        get() = activeController?.isPlaying() == true

    private val currentMedia: QuickLookData.Media?
        get() = lastMedia

    private fun createMedia(): QuickLookData.Media? {
        val metadata = activeController?.controller?.metadata
        val title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
        val artist = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()
        val pkg = activeController?.controller?.packageName
        if (title.isNullOrEmpty() && artist.isNullOrEmpty()) return null
        return QuickLookData.Media(title, artist, pkg, isPlaying)
    }

    private fun updateCachedMedia() {
        val newMedia = createMedia()
        if (newMedia != lastMedia) {
            lastMedia = newMedia
            _mediaFlow.value = lastMedia
        }
    }

    fun cleanup() {
        activeController?.unregister()
        activeController = null
        mediaControllers.forEach { it.unregister() }
        mediaControllers.clear()
        lastPlaybackState = null
        lastMedia = null
        _mediaFlow.value = null
    }

    fun updateNotifications(notifs: List<StatusBarNotification>) {
        mediaControllers.forEach { it.unregister() }
        mediaControllers.clear()
        notifs.forEach { sbn ->
            val token = sbn.notification.extras.getParcelable<MediaSession.Token>("android.mediaSession")
            token?.let {
                val controller = MediaController(context, it)
                val wrapper = MediaControllerSession(controller, sbn)
                mediaControllers.add(wrapper)
                wrapper.register()
            }
        }
        updateTrackedController()
    }

    private fun updateTrackedController() {
        val newTracked = mediaControllers.firstOrNull { it.isPlaying() }
        if (newTracked == activeController) return

        activeController?.unregister()
        activeController = newTracked
        activeController?.register()

        if (activeController == null) {
            lastPlaybackState = null
            lastMedia = null
            _mediaFlow.value = null
        } else {
            updateCachedMedia()
        }
    }

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        super.onPlaybackStateChanged(state)
        if (state == lastPlaybackState) return
        lastPlaybackState = state
        updateTrackedController()
        if (isPlaying) {
            updateCachedMedia()
        } else {
            activeController = null
            lastMedia = null
            _mediaFlow.value = null
        }
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
        super.onMetadataChanged(metadata)
        updateCachedMedia()
    }
    
    private inner class MediaControllerSession(
        val controller: MediaController,
        val sbn: StatusBarNotification?
    ) {
        fun isPlaying(): Boolean {
            val state = controller.playbackState ?: return false
            return state.state == PlaybackState.STATE_PLAYING
        }

        fun register() {
            controller.registerCallback(this@MediaPlaybackProvider)
        }

        fun unregister() {
            controller.unregisterCallback(this@MediaPlaybackProvider)
        }
    }
}
