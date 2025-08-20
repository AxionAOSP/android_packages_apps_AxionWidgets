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

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.Settings
import com.android.axion.widgets.data.CalendarSimpleData
import com.android.axion.widgets.utils.WeakListenerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    interface Callback {
        fun onCalendarDataChanged(data: CalendarSimpleData?)
    }

    private val handler = Handler(Looper.getMainLooper())

    private val callbacks = WeakListenerManager<Callback>().apply {
        setLifecycleCallbacks(
            onActive = {
                if (isQuicklookEnabled()) {
                    startCalendarListening()
                } else {
                    stopCalendarListening()
                    notifyCallbacks(null)
                }
            },
            onInactive = {
                stopCalendarListening()
            }
        )
    }

    private var calendarObserver: ContentObserver? = null
    private var isCalendarListening = false
    private var lastNotifiedData: CalendarSimpleData? = null

    fun addCallback(callback: Callback) {
        callbacks.addListener(callback)
        if (isQuicklookEnabled()) {
            handler.post {
                try {
                    queryCalendar()
                } catch (e: SecurityException) {
                    notifyCallbacks(null)
                }
            }
        } else {
            callback.onCalendarDataChanged(null)
        }
    }

    fun removeCallback(callback: Callback) {
        callbacks.removeListener(callback)
    }

    private fun startCalendarListening() {
        if (isCalendarListening) return

        calendarObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                if (isQuicklookEnabled()) {
                    try {
                        queryCalendar()
                    } catch (e: SecurityException) {
                        stopCalendarListening()
                        notifyCallbacks(null)
                    }
                } else {
                    stopCalendarListening()
                    notifyCallbacks(null)
                }
            }
        }.also { observer ->
            try {
                val resolver = context.contentResolver
                val uris = listOf(
                    CalendarContract.Instances.CONTENT_URI,
                    CalendarContract.Events.CONTENT_URI,
                    CalendarContract.Calendars.CONTENT_URI,
                    CalendarContract.Reminders.CONTENT_URI,
                    CalendarContract.Attendees.CONTENT_URI
                )
                uris.forEach { resolver.registerContentObserver(it, true, observer) }
                queryCalendar()
                isCalendarListening = true
            } catch (e: SecurityException) {
                calendarObserver = null
                isCalendarListening = false
                notifyCallbacks(null)
            }
        }
    }

    private fun stopCalendarListening() {
        calendarObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            calendarObserver = null
        }
        isCalendarListening = false
    }

    private fun queryCalendar() {
        if (!isQuicklookEnabled()) return
        try {
            val now = System.currentTimeMillis()
            val end = now + 24 * 60 * 60 * 1000L

            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
                ContentUris.appendId(this, now)
                ContentUris.appendId(this, end)
            }.build()

            val visibleEvent = context.contentResolver.query(
                uri,
                arrayOf("event_id", "title", "begin", "end", "eventLocation"),
                "visible = 1 AND allDay = 0 AND end > ?",
                arrayOf(now.toString()),
                "begin ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val event = CalendarSimpleData.buildDataFromCursor(cursor)
                    if (event.isEventVisible() && isEventValid(event)) {
                        return@use event
                    }
                }
                null
            } ?: CalendarSimpleData.EMPTY

            notifyCallbacks(if (visibleEvent == CalendarSimpleData.EMPTY) null else visibleEvent)
        } catch (e: SecurityException) {
            notifyCallbacks(null)
        }
    }

    private fun isEventValid(event: CalendarSimpleData): Boolean {
        return context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf("_id", "deleted"),
            "_id = ?",
            arrayOf(event.id.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex("deleted")
                (if (idx != -1) cursor.getInt(idx) else 0) == 0
            } else false
        } ?: false
    }

    private fun notifyCallbacks(data: CalendarSimpleData?) {
        if (data == lastNotifiedData) return
        lastNotifiedData = data
        callbacks.notify { it.onCalendarDataChanged(data) }
    }

    private fun isQuicklookEnabled(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            "nt_quicklook_events",
            1
        ) == 1
    }
}
