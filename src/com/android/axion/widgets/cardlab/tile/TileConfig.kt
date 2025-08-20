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
package com.android.axion.widgets.cardlab.tile

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TileConfig(
    val type: String,
    val observeState: () -> Boolean,
    val toggle: () -> Boolean,
    val getIcon: (Boolean) -> Int,
    val getLabel: (() -> String)? = null,
    val spec: String
) {
    companion object {
        fun from(
            type: String,
            getter: () -> Boolean,
            setter: (() -> Boolean)? = null,
            context: Context,
            labelProvider: (() -> String)? = null,
            spec: String
        ): TileConfig {
            val observeState: () -> Boolean = getter
            val toggle: () -> Boolean = { setter?.invoke() ?: !getter() }
            val getIcon: (Boolean) -> Int = { active ->
                val baseName = spec ?: type.lowercase().replace(" ", "_")
                val resName = "ic_${baseName}_${if (active) "on" else "off"}"
                context.resources.getIdentifier(resName, "drawable", context.packageName).takeIf { it != 0 }
                    ?: context.resources.getIdentifier("ic_foreground", "drawable", context.packageName)
            }
            return TileConfig(type, observeState, toggle, getIcon, labelProvider, spec)
        }
    }
}
