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
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.android.axion.widgets.AxionApp
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.MediaPlayerData
import com.android.axion.widgets.di.MainScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class MediaPlayerProvider
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @MainScope private val scope: CoroutineScope,
) : AxionProvider<MediaPlayerData> {

    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val _data = MutableStateFlow<MediaPlayerData?>(null)
    private var activeController: MediaController? = null
    private val prefs =
        context
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override val dataFlow: Flow<MediaPlayerData?> = _data

    val currentData: MediaPlayerData?
        get() = _data.value

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateActiveSession(controllers ?: emptyList())
        }

    private val controllerCallback =
        object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateData()
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updateData()
            }

            override fun onSessionDestroyed() {
                detachController()
                val sessions = sessionManager?.getActiveSessions(null) ?: emptyList()
                if (sessions.isNotEmpty()) {
                    updateActiveSession(sessions)
                } else {
                    _data.value = null
                }
            }
        }

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        sessionManager?.addOnActiveSessionsChangedListener(sessionsListener, null, mainHandler)
        updateActiveSession(sessionManager?.getActiveSessions(null) ?: emptyList())
    }

    private fun updateActiveSession(controllers: List<MediaController>) {
        val playing =
            controllers.firstOrNull { c -> c.playbackState?.state == PlaybackState.STATE_PLAYING }
        val target = playing ?: controllers.firstOrNull()

        if (target?.sessionToken == activeController?.sessionToken) {
            if (target != null) updateData()
            return
        }

        detachController()
        if (target != null) {
            activeController = target
            target.registerCallback(controllerCallback)
            updateData()
        } else {
            _data.value = null
        }
    }

    private fun detachController() {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    private fun updateData() {
        val controller = activeController ?: return
        val metadata = controller.metadata
        val state = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val current = _data.value

        if (title == null && artist == null && current?.title != null) {
            _data.value =
                current.copy(
                    isPlaying = state?.state == PlaybackState.STATE_PLAYING,
                    position = state?.position ?: current.position,
                )
            return
        }

        val pkg = controller.packageName
        if (title != null && pkg != null) {
            prefs.edit().putString(KEY_LAST_PKG, pkg).apply()
        }

        _data.value =
            MediaPlayerData(
                title = title,
                artist = artist,
                albumArt = extractAlbumArt(metadata),
                isPlaying = state?.state == PlaybackState.STATE_PLAYING,
                duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                position = state?.position ?: 0L,
                packageName = pkg,
            )
    }

    private fun extractAlbumArt(metadata: MediaMetadata?): Bitmap? {
        if (metadata == null) return null
        val art =
            metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: return null
        return scaleBitmap(art, MAX_ART_SIZE)
    }

    private fun scaleBitmap(src: Bitmap, maxSize: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxSize && h <= maxSize) return src
        val scale = maxSize.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    fun playPause() {
        val controller = activeController ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun next() {
        activeController?.transportControls?.skipToNext()
    }

    fun previous() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun stop() {
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        detachController()
        _data.value = null
    }

    val lastPlayedPackage: String?
        get() = prefs.getString(KEY_LAST_PKG, null)

    companion object {
        private const val MAX_ART_SIZE = 512
        private const val PREFS_NAME = "media_prefs"
        private const val KEY_LAST_PKG = "last_pkg"

        fun get(context: Context): MediaPlayerProvider {
            val app = context.applicationContext as AxionApp
            return app.appComponent.mediaPlayerProvider()
        }
    }
}
