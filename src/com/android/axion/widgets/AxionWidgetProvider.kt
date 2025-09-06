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
package com.android.axion.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.android.axion.widgets.data.QuickLookData
import com.android.axion.widgets.utils.logger

abstract class AxionWidgetProvider : AppWidgetProvider() {

    companion object {
        private fun <T : AxionWidgetProvider> forEachWidgetId(
            context: Context,
            widgetClass: Class<T>,
            action: (Int) -> Unit
        ) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, widgetClass))
                ids.forEach(action)
            }.onFailure { e ->
                logger("exception occured!!! exception: ${e}")
            }
        }

        fun <T : AxionWidgetProvider> updateWidget(
            context: Context,
            widgetClass: Class<T>,
            data: QuickLookData,
            remoteViewsBuilder: (Context, QuickLookData) -> RemoteViews
        ) {
            val views = remoteViewsBuilder(context, data)
            forEachWidgetId(context, widgetClass) { id ->
                AppWidgetManager.getInstance(context).updateAppWidget(id, views)
            }
        }

        fun <T : AxionWidgetProvider> updateAllWidgets(
            context: Context,
            widgetClass: Class<T>,
            views: RemoteViews
        ) {
            forEachWidgetId(context, widgetClass) { id ->
                AppWidgetManager.getInstance(context).updateAppWidget(id, views)
            }
        }

        fun <T : AxionWidgetProvider> doForAllWidgets(
            context: Context,
            widgetClass: Class<T>,
            action: (Int) -> Unit
        ) {
            forEachWidgetId(context, widgetClass, action)
        }

        fun buildRemoteViews(
            context: Context,
            layoutResId: Int,
            data: QuickLookData,
            binder: RemoteViews.(QuickLookData) -> Unit
        ): RemoteViews {
            return RemoteViews(context.packageName, layoutResId).apply { binder(data) }
        }
    }
}
