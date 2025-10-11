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
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.data.CalendarSimpleData
import com.android.axion.widgets.utils.callbackFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CoroutineScope
) : AxionProvider<QuickLookData.CalendarEvent> {

    private val handler = Handler(Looper.getMainLooper())
    private var calendarObserver: ContentObserver? = null

    override val dataFlow: Flow<QuickLookData.CalendarEvent?> = callbackFlow(
        initial = null,
        scope = scope,
        register = { callback -> start(callback) },
        unregister = { stop() },
        createCallback = { emit ->
            { event: QuickLookData.CalendarEvent? ->
                emit(event)
            }
        }
    )

    private fun start(emit: (QuickLookData.CalendarEvent?) -> Unit) {
        if (calendarObserver != null) return

        calendarObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                emit(queryCalendarEvent())
            }
        }.also { observer ->
            val resolver = context.contentResolver
            val uris = listOf(
                CalendarContract.Instances.CONTENT_URI,
                CalendarContract.Events.CONTENT_URI,
                CalendarContract.Calendars.CONTENT_URI,
                CalendarContract.Reminders.CONTENT_URI,
                CalendarContract.Attendees.CONTENT_URI
            )
            uris.forEach { resolver.registerContentObserver(it, true, observer) }
            emit(queryCalendarEvent())
        }
    }

    private fun stop() {
        calendarObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        calendarObserver = null
    }

    private fun queryCalendarEvent(): QuickLookData.CalendarEvent? {
        if (!isQuicklookEnabled()) return null

        val now = System.currentTimeMillis()
        val end = now + 24 * 60 * 60 * 1000L
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
            ContentUris.appendId(this, now)
            ContentUris.appendId(this, end)
        }.build()

        val event: CalendarSimpleData? = context.contentResolver.query(
            uri,
            arrayOf("event_id", "title", "begin", "end", "eventLocation"),
            "visible = 1 AND allDay = 0 AND end > ?",
            arrayOf(now.toString()),
            "begin ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val e = CalendarSimpleData.buildDataFromCursor(cursor)
                if (e.isEventVisible()) {
                    return@use e
                }
            }
            null
        }
        return CalendarSimpleData.toQuickLookCalendarEvent(context, event)
    }

    private fun isEventValid(event: CalendarSimpleData): Boolean {
        return context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf("deleted"),
            "_id = ?",
            arrayOf(event.id.toString()),
            null
        )?.use { cursor ->
            cursor.moveToFirst() && (cursor.getColumnIndex("deleted").takeIf { it != -1 }?.let { cursor.getInt(it) } ?: 0) == 0
        } ?: false
    }

    private fun isQuicklookEnabled(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, "nt_quicklook_events", 1) == 1
    }
}
