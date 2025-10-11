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
import com.android.axion.widgets.manager.WidgetUsageManager
import com.android.axion.widgets.utils.logger
import kotlin.reflect.KClass

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
                logger("exception occurred!!! exception: $e")
            }
        }

        fun <W : AxionWidgetProvider, D> updateWidget(
            context: Context,
            widgetClass: Class<W>,
            data: D,
            remoteViewsBuilder: (Context, D) -> RemoteViews
        ) {
            val views = remoteViewsBuilder(context, data)
            forEachWidgetId(context, widgetClass) { id ->
                AppWidgetManager.getInstance(context).updateAppWidget(id, views)
            }
        }

        fun <W : AxionWidgetProvider> updateAllWidgets(
            context: Context,
            widgetClass: Class<W>,
            views: RemoteViews
        ) {
            forEachWidgetId(context, widgetClass) { id ->
                AppWidgetManager.getInstance(context).updateAppWidget(id, views)
            }
        }

        fun <W : AxionWidgetProvider> doForAllWidgets(
            context: Context,
            widgetClass: Class<W>,
            action: (Int) -> Unit
        ) {
            forEachWidgetId(context, widgetClass, action)
        }

        fun <D> buildRemoteViews(
            context: Context,
            layoutResId: Int,
            data: D,
            binder: RemoteViews.(D) -> Unit
        ): RemoteViews {
            return RemoteViews(context.packageName, layoutResId).apply { binder(data) }
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUsageManager.updateWidgetStatus(context, this::class.java)
        WidgetUpdateService.update(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUsageManager.updateWidgetStatus(context, this::class.java)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUsageManager.updateWidgetStatus(context, this::class.java)
        WidgetUpdateService.update(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetUsageManager.updateWidgetStatus(context, this::class.java)
    }
    
    open fun requiredProviders(): List<KClass<out AxionProvider<*>>> = emptyList()

}
