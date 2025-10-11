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
package com.android.axion.widgets.manager

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.android.axion.widgets.AxionProvider
import com.android.axion.widgets.AxionWidgetProvider
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

object WidgetUsageManager {

    private val widgetActiveMap =
        mutableMapOf<KClass<out AxionWidgetProvider>, MutableStateFlow<Boolean>>()

    private val providerRequiredFlows =
        mutableMapOf<KClass<out AxionProvider<*>>, MutableStateFlow<Boolean>>()

    fun <T : AxionWidgetProvider> isActiveFlow(widgetClass: Class<T>): StateFlow<Boolean> {
        return widgetActiveMap.getOrPut(widgetClass.kotlin) { MutableStateFlow(false) }
    }

    fun <T : AxionWidgetProvider> updateWidgetStatus(context: Context, widgetClass: Class<T>) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, widgetClass))
        val hasWidgets = ids.isNotEmpty()
        widgetActiveMap
            .getOrPut(widgetClass.kotlin) { MutableStateFlow(false) }
            .value = hasWidgets
        recalcProviderRequirements(context)
    }

    fun getActiveWidgets(): Set<KClass<out AxionWidgetProvider>> =
        widgetActiveMap.filterValues { it.value }.keys

    fun isProviderRequired(providerClass: KClass<out AxionProvider<*>>): StateFlow<Boolean> {
        return providerRequiredFlows.getOrPut(providerClass) { MutableStateFlow(false) }
    }

    private fun recalcProviderRequirements(context: Context) {
        val activeWidgets = getActiveWidgets()
        val activeProviders = activeWidgets
            .flatMap { widgetCls ->
                val widgetInstance = runCatching { widgetCls.java.newInstance() }.getOrNull()
                widgetInstance?.requiredProviders().orEmpty()
            }
            .toSet()

        for (provider in activeProviders) {
            val flow = providerRequiredFlows.getOrPut(provider) { MutableStateFlow(false) }
            flow.value = true
        }

        providerRequiredFlows
            .filterKeys { it !in activeProviders }
            .forEach { (_, flow) -> flow.value = false }
    }

    fun refreshAll(context: Context) {
        widgetActiveMap.keys.forEach { cls ->
            updateWidgetStatus(context, cls.java)
        }
    }
}
