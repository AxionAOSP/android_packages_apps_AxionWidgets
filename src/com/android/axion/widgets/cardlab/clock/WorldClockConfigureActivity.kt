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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.android.axion.widgets.cardlab.clock

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import java.util.TimeZone

class WorldClockConfigureActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        widgetId =
            intent.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            WorldClockTheme {
                WorldClockConfigScreen { config ->
                    WorldClockPrefs.set(this, widgetId, config)
                    AxWorldClockReceiver.updateWidget(this, widgetId, config)
                    val resultIntent =
                        Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId) }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }
}

@Composable
private fun WorldClockTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val colorScheme =
        if (isDarkTheme) {
            darkColorScheme(background = colorResource(id = android.R.color.system_neutral1_900))
        } else {
            lightColorScheme(background = colorResource(id = android.R.color.system_neutral1_50))
        }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography(),
        content = content,
    )
}

private data class TimeZoneCity(val id: String, val cityName: String, val gmtOffset: String)

private fun buildTimeZoneList(): List<TimeZoneCity> {
    return TimeZone.getAvailableIDs()
        .filter { it.contains("/") && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
        .map { id ->
            val tz = TimeZone.getTimeZone(id)
            val offsetMs = tz.rawOffset
            val hours = offsetMs / 3600000
            val minutes = Math.abs(offsetMs % 3600000 / 60000)
            val gmtOffset =
                if (minutes > 0) {
                    "GMT%+d:%02d".format(hours, minutes)
                } else {
                    "GMT%+d".format(hours)
                }
            val city = id.substringAfterLast("/").replace("_", " ")
            TimeZoneCity(id, city, gmtOffset)
        }
        .distinctBy { it.cityName }
        .sortedBy { it.cityName }
}

@Composable
private fun WorldClockConfigScreen(onSelected: (WorldClockConfig) -> Unit) {
    val context = LocalContext.current
    val timeZones = remember { buildTimeZoneList() }
    var searchQuery by remember { mutableStateOf("") }

    val filtered =
        remember(searchQuery) {
            if (searchQuery.isBlank()) timeZones
            else
                timeZones.filter {
                    it.cityName.contains(searchQuery, ignoreCase = true) ||
                        it.id.contains(searchQuery, ignoreCase = true)
                }
        }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "World Clock",
                        fontSize = 32.sp,
                        fontFamily = FontFamily(Typeface.create("nothingdot57", Typeface.NORMAL)),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search city...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered) { tz ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { onSelected(WorldClockConfig(tz.id, tz.cityName)) }
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tz.cityName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = tz.id,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        Text(
                            text = tz.gmtOffset,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}
