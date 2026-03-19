/*
 * Copyright (C) 2025-2026 AxionOS Project
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

package com.android.axion.widgets.quicklook

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.android.axion.widgets.R
import com.android.axion.widgets.manager.QuickLookDataManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageProvider @Inject constructor(private val context: Context) {

    private var index = (0..100).random()

    fun getMessage(): String {
        val messages = context.resources.getStringArray(R.array.quicklook_messages)
        if (messages.isEmpty()) return ""
        index = (index + 1) % messages.size
        return messages[index]
    }

    fun scheduleRotation() {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intervalMin = QuickLookPrefs.getMessageInterval(context)
        val intervalMs = intervalMin * 60_000L
        val pi = getAlarmIntent()

        am.cancel(pi)
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + intervalMs,
            intervalMs,
            pi,
        )
    }

    fun cancelRotation() {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(getAlarmIntent())
    }

    private fun getAlarmIntent(): PendingIntent {
        val intent = Intent(ACTION_ROTATE_MESSAGE).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun shutdown() {
        cancelRotation()
    }

    class RotateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_ROTATE_MESSAGE) {
                QuickLookDataManager.get(context).onDataUpdated()
            }
        }
    }

    companion object {
        const val ACTION_ROTATE_MESSAGE = "com.android.axion.widgets.ACTION_ROTATE_MESSAGE"
    }
}
