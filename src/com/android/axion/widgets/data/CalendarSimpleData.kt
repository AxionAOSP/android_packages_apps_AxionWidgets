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
package com.android.axion.widgets.data

import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import android.provider.CalendarContract
import android.util.Log
import android.text.format.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CalendarSimpleData(
    val id: Long = 0L,
    val title: String? = null,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val location: String? = null
) {

    fun getToBeginTime(): Long {
        val currentTime = System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toMinutes(startTime) - TimeUnit.MILLISECONDS.toMinutes(currentTime)
    }

    fun getEventStatus(): Int {
        val toBeginTime = getToBeginTime()
        return when {
            toBeginTime > 20 -> EVENT_STATUS_TO_SCHEDULE
            toBeginTime > 0 -> EVENT_STATUS_TO_BEGIN
            toBeginTime > -10 -> EVENT_STATUS_NOW
            else -> EVENT_STATUS_END
        }
    }

    fun isEventVisible(): Boolean {
        val status = getEventStatus()
        return status == EVENT_STATUS_TO_BEGIN || status == EVENT_STATUS_NOW
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CalendarSimpleData) return false

        return id == other.id &&
            startTime == other.startTime &&
            endTime == other.endTime &&
            TextUtils.equals(title, other.title) &&
            TextUtils.equals(location, other.location)
    }

    override fun hashCode(): Int {
        return Objects.hash(id, title, startTime, endTime, location)
    }

    companion object {
        private const val TAG = "CalendarSimpleData"

        const val EVENT_STATUS_TO_SCHEDULE = 0
        const val EVENT_STATUS_TO_BEGIN = 1
        const val EVENT_STATUS_NOW = 2
        const val EVENT_STATUS_END = 3

        fun buildDataFromCursor(cursor: Cursor): CalendarSimpleData {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("event_id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val location = cursor.getString(cursor.getColumnIndexOrThrow("eventLocation"))
            val begin = cursor.getLong(cursor.getColumnIndexOrThrow("begin"))
            val end = cursor.getLong(cursor.getColumnIndexOrThrow("end"))

            Log.d(TAG, "Next calendar event is loaded. id = $id, title = $title, location = $location, dtStart = $begin, dtEnd = $end")

            return CalendarSimpleData(id, title, begin, end, location)
        }
        
        fun toQuickLookCalendarEvent(context: Context, data: CalendarSimpleData?): QuickLookData.CalendarEvent? {
            if (data == null) return null

            val formatter = DateFormat.getTimeFormat(context)
            val start = formatter.format(Date(data.startTime))
            val end = formatter.format(Date(data.endTime))

            return QuickLookData.CalendarEvent(
                id = data.id,
                title = data.title ?: "",
                startTime = data.startTime,
                endTime = data.endTime,
                location = data.location ?: "",
                desc = "$start - $end"
            )
        }

        fun isEventValid(context:Context, data: QuickLookData.CalendarEvent?): Boolean {
            if (data == null) return false
            return context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf("deleted"),
                "_id = ?",
                arrayOf(data.id.toString()),
                null
            )?.use { cursor ->
                cursor.moveToFirst() && (cursor.getColumnIndex("deleted").takeIf { it != -1 }?.let { cursor.getInt(it) } ?: 0) == 0
            } ?: false
        }

        val EMPTY = CalendarSimpleData()
    }
}
