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
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.MediaData
import com.android.axion.widgets.data.MediaNotifications
import com.android.axion.widgets.utils.SafeCloseable
import com.android.axion.widgets.utils.Tracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPlaybackProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaController.Callback(), AxionProvider<MediaData>, SafeCloseable {

    private val session = mutableListOf<MediaSessionController>()
    private var activeController: MediaSessionController? = null
    private var lastPlaybackState: PlaybackState? = null

    private val _mediaFlow = MutableStateFlow<MediaData?>(null)
    override val dataFlow: Flow<MediaData?> = _mediaFlow.asStateFlow()
    
    init {
        Tracker.get().addCloseable(this)
    }

    private fun createMedia(): MediaData? {
        val metadata = activeController?.controller?.metadata
        val title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
        val artist = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()
        val pkg = activeController?.controller?.packageName
        if (title.isNullOrEmpty() && artist.isNullOrEmpty()) return null
        val isPlaying = activeController?.isPlaying() == true
        return MediaData(title, artist, pkg, isPlaying)
    }

    private fun updateMedia() {
        _mediaFlow.value = createMedia()
    }

    override fun close() {
        session.toList().forEach { it.unregister() }
        session.clear()
        activeController?.unregister()
        activeController = null
        lastPlaybackState = null
        _mediaFlow.value = null
    }

    fun onMediaUpdate(mediaNotifs: MediaNotifications) {
        session.toList().forEach { it.unregister() }
        session.clear()
        mediaNotifs.forEach { n ->
            val token = n.token
            token?.let {
                val controller = MediaController(context, it)
                val wrapper = MediaSessionController(controller)
                session.add(wrapper)
                wrapper.register()
            }
        }
        updateController()
    }

    private fun updateController() {
        val new = session.firstOrNull { it.isPlaying() }
        if (new == activeController) return

        activeController?.unregister()
        activeController = new
        activeController?.register()

        if (activeController == null) {
            lastPlaybackState = null
            _mediaFlow.value = null
        } else {
            updateMedia()
        }
    }

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        super.onPlaybackStateChanged(state)
        if (state == lastPlaybackState) return
        lastPlaybackState = state
        updateController()
        if (activeController?.isPlaying() == true) {
            updateMedia()
        } else {
            activeController = null
            _mediaFlow.value = null
        }
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
        super.onMetadataChanged(metadata)
        updateMedia()
    }

    private inner class MediaSessionController(
        val controller: MediaController
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
