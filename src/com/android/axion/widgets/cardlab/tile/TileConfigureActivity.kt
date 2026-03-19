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

package com.android.axion.widgets.cardlab.tile

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
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
import com.android.axion.widgets.R

class TileConfigureActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var tileManager: TileManager
    private lateinit var tileRepository: TileRepository

    private fun initDependencies(context: Context) {
        if (::tileManager.isInitialized && ::tileRepository.isInitialized) return
        tileManager = TileManager.get(context.applicationContext)
        tileRepository = TileRepository.get(context.applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        widgetId =
            intent.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        initDependencies(this)

        setContent {
            TileConfigureTheme {
                WidgetConfigScreen(widgetId, tileRepository) { selectedSpec ->
                    val currentSpec = WidgetPrefs.getWidgetAction(this, widgetId)
                    if (currentSpec == selectedSpec) {
                        finish()
                        return@WidgetConfigScreen
                    }
                    WidgetPrefs.setWidgetAction(this, widgetId, selectedSpec)
                    tileManager.setTileForWidget(widgetId, selectedSpec)
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
fun TileConfigureTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()

    val dynamicColorScheme =
        if (isDarkTheme) {
            darkColorScheme(background = colorResource(id = android.R.color.system_neutral1_900))
        } else {
            lightColorScheme(background = colorResource(id = android.R.color.system_neutral1_50))
        }

    MaterialExpressiveTheme(
        colorScheme = dynamicColorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography(),
        content = content,
    )
}

@Composable
fun WidgetConfigScreen(
    widgetId: Int,
    tileRepository: TileRepository,
    onTileSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val availableTiles by tileRepository.queryAvailableTiles().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.quick_settings),
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
                scrollBehavior = null,
            )
        },
        content = { innerPadding ->
            if (availableTiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                ) {
                    items(availableTiles) { tile ->
                        TileRow(
                            label = tile.label,
                            icon = tileIcon(tile.spec),
                            onClick = { onTileSelected(tile.spec) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        },
    )
}

private val tileIconsMap =
    mapOf(
        "wifi" to Icons.Filled.Wifi,
        "internet" to Icons.Filled.Wifi,
        "bt" to Icons.Filled.Bluetooth,
        "bluetooth" to Icons.Filled.Bluetooth,
        "airplane" to Icons.Filled.AirplanemodeActive,
        "cell" to Icons.Filled.NetworkCell,
        "mobiledata" to Icons.Filled.NetworkCell,
        "dark" to Icons.Filled.DarkMode,
        "ui_mode_night" to Icons.Filled.DarkMode,
        "flashlight" to Icons.Filled.FlashlightOn,
        "dnd" to Icons.Filled.DoNotDisturb,
        "rotation" to Icons.Filled.ScreenRotation,
        "hotspot" to Icons.Filled.WifiTethering,
        "location" to Icons.Filled.LocationOn,
        "battery" to Icons.Filled.BatterySaver,
        "saver" to Icons.Filled.BatterySaver,
        "inversion" to Icons.Filled.InvertColors,
        "color_correction" to Icons.Filled.InvertColors,
        "night" to Icons.Filled.Bedtime,
        "nfc" to Icons.Filled.Nfc,
        "cast" to Icons.Filled.Cast,
        "data_saver" to Icons.Filled.DataSaverOn,
        "reduce_brightness" to Icons.Filled.BrightnessLow,
        "extra_dim" to Icons.Filled.BrightnessLow,
        "aod" to Icons.Filled.PhoneAndroid,
        "ambient_display" to Icons.Filled.PhoneAndroid,
        "screenrecord" to Icons.Filled.FiberManualRecord,
        "screenshot" to Icons.Filled.Screenshot,
        "work" to Icons.Filled.Work,
        "caffeine" to Icons.Filled.LocalCafe,
        "heads_up" to Icons.Filled.NotificationsActive,
        "reading_mode" to Icons.Filled.MenuBook,
    )

@Composable
fun tileIcon(spec: String): @Composable () -> Unit = {
    val normalizedSpec = spec.lowercase()
    val icon =
        tileIconsMap.entries.firstOrNull { (key, _) -> normalizedSpec.contains(key) }?.value
            ?: Icons.Filled.Settings
    Icon(icon, contentDescription = spec)
}

@Composable
fun TileRow(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 27.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(24.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
