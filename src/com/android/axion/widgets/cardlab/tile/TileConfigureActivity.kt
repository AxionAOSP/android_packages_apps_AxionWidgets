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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.android.axion.widgets.cardlab.tile

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import com.android.axion.compose.scaffold.AxionScaffold
import com.android.axion.compose.theme.AxionTheme
import com.android.axion.compose.preferences.ListPreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.preferences.SliderPreference
import com.android.axion.compose.preferences.SwitchPreference
import com.android.axion.widgets.R
import kotlin.math.roundToInt

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
            AxionTheme {
                WidgetConfigScreen(
                    widgetId = widgetId,
                    tileRepository = tileRepository,
                    onTileSelected = { selectedSpec ->
                        val currentSpec = WidgetPrefs.getWidgetAction(this, widgetId)
                        if (currentSpec != selectedSpec) {
                            WidgetPrefs.setWidgetAction(this, widgetId, selectedSpec)
                            tileManager.setTileForWidget(widgetId, selectedSpec)
                        }
                        val resultIntent =
                            Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onSizingChanged = { refreshWidget() },
                )
            }
        }
    }

    private fun refreshWidget() {
        val spec = WidgetPrefs.getWidgetAction(this, widgetId) ?: return
        tileManager.setTileForWidget(widgetId, spec)
    }
}

@Composable
fun WidgetConfigScreen(
    widgetId: Int,
    tileRepository: TileRepository,
    onTileSelected: (String) -> Unit,
    onSizingChanged: () -> Unit,
) {
    val context = LocalContext.current
    val availableTiles by tileRepository.queryAvailableTiles().collectAsState(initial = emptyList())

    AxionScaffold(
        title = stringResource(R.string.quick_settings),
        onBackClick = { (context as? Activity)?.finish() },
        collapsedByDefault = false,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
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
                        .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TileSelectionSection(
                        widgetId = widgetId,
                        tiles = availableTiles,
                        onTileSelected = onTileSelected,
                    )
                }
                item {
                    SizingSection(widgetId = widgetId, onChanged = onSizingChanged)
                }
            }
        }
    }
}

@Composable
private fun TileSelectionSection(
    widgetId: Int,
    tiles: List<TileRepository.AvailableTile>,
    onTileSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val currentSpec = remember(widgetId) {
        WidgetPrefs.getWidgetAction(context, widgetId).orEmpty()
    }
    val options = remember(tiles) { tiles.map { it.spec to it.label } }
    PreferenceGroup(title = stringResource(R.string.tile_selection_group_title)) {
        item {
            ListPreference(
                title = stringResource(R.string.tile_select_title),
                summary = stringResource(R.string.tile_select_summary),
                options = options,
                value = currentSpec,
                onValueChange = onTileSelected,
            )
        }
    }
}

@Composable
private fun SizingSection(widgetId: Int, onChanged: () -> Unit) {
    val context = LocalContext.current

    var tileSize by remember {
        mutableFloatStateOf(WidgetPrefs.getTileSizeDp(context, widgetId).toFloat())
    }
    var pillHeight by remember {
        mutableFloatStateOf(WidgetPrefs.getPillHeightDp(context, widgetId).toFloat())
    }
    var pillWidth by remember {
        mutableFloatStateOf(WidgetPrefs.getPillWidthDp(context, widgetId).toFloat())
    }
    var autoFit by remember {
        mutableStateOf(WidgetPrefs.isPillAutoFitWidth(context, widgetId))
    }

    PreferenceGroup(title = stringResource(R.string.tile_size_group_title)) {
        item {
            SliderPreference(
                title = stringResource(R.string.tile_circle_size_title),
                summary = stringResource(R.string.tile_circle_size_summary),
                value = tileSize,
                onValueChange = { tileSize = it },
                onValueChangeFinished = {
                    WidgetPrefs.setTileSizeDp(context, widgetId, tileSize.roundToInt())
                    onChanged()
                },
                valueRange =
                    WidgetPrefs.MIN_TILE_SIZE_DP.toFloat()..WidgetPrefs.MAX_TILE_SIZE_DP.toFloat(),
                steps = 0,
                displayValue = formatDp(tileSize),
                onReset = {
                    tileSize = WidgetPrefs.DEFAULT_TILE_SIZE_DP.toFloat()
                    WidgetPrefs.setTileSizeDp(context, widgetId, WidgetPrefs.DEFAULT_TILE_SIZE_DP)
                    onChanged()
                },
            )
        }
        item {
            SliderPreference(
                title = stringResource(R.string.pill_height_title),
                summary = stringResource(R.string.pill_height_summary),
                value = pillHeight,
                onValueChange = { pillHeight = it },
                onValueChangeFinished = {
                    WidgetPrefs.setPillHeightDp(context, widgetId, pillHeight.roundToInt())
                    onChanged()
                },
                valueRange =
                    WidgetPrefs.MIN_PILL_HEIGHT_DP.toFloat()..
                        WidgetPrefs.MAX_PILL_HEIGHT_DP.toFloat(),
                steps = 0,
                displayValue = formatDp(pillHeight),
                onReset = {
                    pillHeight = WidgetPrefs.DEFAULT_PILL_HEIGHT_DP.toFloat()
                    WidgetPrefs.setPillHeightDp(
                        context,
                        widgetId,
                        WidgetPrefs.DEFAULT_PILL_HEIGHT_DP,
                    )
                    onChanged()
                },
            )
        }
        item {
            SwitchPreference(
                title = stringResource(R.string.pill_autofit_title),
                summary = stringResource(R.string.pill_autofit_summary),
                checked = autoFit,
                onCheckedChange = {
                    autoFit = it
                    WidgetPrefs.setPillAutoFitWidth(context, widgetId, it)
                    onChanged()
                },
            )
        }
        item {
            SliderPreference(
                title = stringResource(R.string.pill_width_title),
                summary = stringResource(R.string.pill_width_summary),
                value = pillWidth,
                onValueChange = { pillWidth = it },
                onValueChangeFinished = {
                    WidgetPrefs.setPillWidthDp(context, widgetId, pillWidth.roundToInt())
                    onChanged()
                },
                valueRange =
                    WidgetPrefs.MIN_PILL_WIDTH_DP.toFloat()..
                        WidgetPrefs.MAX_PILL_WIDTH_DP.toFloat(),
                steps = 0,
                displayValue = formatDp(pillWidth),
                enabled = !autoFit,
                onReset = {
                    pillWidth = WidgetPrefs.DEFAULT_PILL_WIDTH_DP.toFloat()
                    WidgetPrefs.setPillWidthDp(context, widgetId, WidgetPrefs.DEFAULT_PILL_WIDTH_DP)
                    onChanged()
                },
            )
        }
    }
}

private fun formatDp(value: Float): String = "${value.roundToInt()} dp"
