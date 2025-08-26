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

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.ContextCompat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
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

        widgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        initDependencies(this)

        setContent {
            TileConfigureTheme {
                WidgetConfigScreen(widgetId, tileRepository) { selectedTile ->
                    val currentTile = WidgetPrefs.getWidgetAction(this, widgetId)
                    if (currentTile == selectedTile) {
                        finish()
                        return@WidgetConfigScreen
                    }
                    WidgetPrefs.setWidgetAction(this, widgetId, selectedTile)
                    tileManager.setTileForWidget(widgetId, selectedTile)
                    val resultIntent = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
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

    val dynamicColorScheme = if (isDarkTheme) {
        darkColorScheme(
            background = colorResource(id = android.R.color.system_neutral1_900)
        )
    } else {
        lightColorScheme(
            background = colorResource(id = android.R.color.system_neutral1_50)
        )
    }

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    widgetId: Int,
    tileRepository: TileRepository,
    onTileSelected: (String) -> Unit
) {
    val tiles = tileRepository.tilesRegistry
    val context = LocalContext.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.quick_settings),
                        fontSize = 32.sp,
                        fontFamily = FontFamily(Typeface.create("nothingdot57", Typeface.NORMAL))
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = null
            )
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                items(tiles) { tile ->
                    TileRow(
                        tileType = tile.type,
                        icon = tileIcon(tile.spec),
                        isSelected = false,
                        onClick = { onTileSelected(tile.type) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    )
}

val tileIcons = mapOf(
    "wifi" to Icons.Filled.Wifi,
    "bluetooth" to Icons.Filled.Bluetooth,
    "airplane" to Icons.Filled.AirplanemodeActive,
    "mobile_data" to Icons.Filled.NetworkCell,
    "dark_theme" to Icons.Filled.DarkMode,
    "torch" to Icons.Filled.FlashlightOn,
    "dnd" to Icons.Filled.DoNotDisturb,
    "auto_rotate" to Icons.Filled.ScreenRotation,
    "ringer" to Icons.Filled.VolumeUp
)

@Composable
fun tileIcon(spec: String): @Composable () -> Unit = {
    val icon = tileIcons[spec] ?: Icons.Filled.Settings
    Icon(icon, contentDescription = spec)
}

@Composable
fun TileRow(
    tileType: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.background
            )
            .padding(vertical = 27.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = tileType,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
