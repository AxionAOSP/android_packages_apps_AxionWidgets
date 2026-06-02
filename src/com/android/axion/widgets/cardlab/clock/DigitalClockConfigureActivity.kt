/*
 * Copyright (C) 2026 AxionOS Project
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.android.axion.compose.scaffold.AxionScaffold
import com.android.axion.compose.theme.AxionTheme
import com.android.axion.compose.preferences.ListPreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.widgets.R

private val NothingDotFontFamily = FontFamily(
    Typeface.create("nothingdot57", Typeface.NORMAL)
)

class DigitalClockConfigureActivity : ComponentActivity() {

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

        val existingBg = ClockPrefs.getBackground(this, widgetId)

        setContent {
            AxionTheme {
                DigitalClockConfigScreen(
                    widgetId = widgetId,
                    initialBg = existingBg,
                    onSave = { bg ->
                        ClockPrefs.setBackground(this, widgetId, bg)
                        AxDigitalClockReceiver.updateWidget(this, widgetId)
                        val resultIntent =
                            Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId) }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun PreviewOptionCard(
    title: String,
    description: String,
    bgType: ClockBackground,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (bgType == ClockBackground.TRANSPARENT) {
                                Color.Transparent
                            } else {
                                colorResource(id = R.color.battery_bg_color)
                            }
                        )
                        .then(
                            if (bgType == ClockBackground.TRANSPARENT) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .padding(start = 12.dp, end = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "10:10",
                            fontFamily = NothingDotFontFamily,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.battery_device_primary_color),
                            lineHeight = 36.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Mon, 3 Jun",
                            fontFamily = NothingDotFontFamily,
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.battery_device_primary_color).copy(alpha = 0.6f),
                            lineHeight = 12.sp,
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DigitalClockConfigScreen(
    widgetId: Int,
    initialBg: ClockBackground,
    onSave: (ClockBackground) -> Unit
) {
    val context = LocalContext.current
    var selectedBg by remember { mutableStateOf(initialBg) }

    AxionScaffold(
        title = stringResource(R.string.label_digital_clock),
        onBackClick = { (context as? Activity)?.finish() },
        collapsedByDefault = false,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        actions = {
            IconButton(onClick = { onSave(selectedBg) }) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.ql_apply))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                PreviewOptionCard(
                    title = stringResource(R.string.clock_bg_translucent),
                    description = stringResource(R.string.clock_bg_translucent_desc),
                    bgType = ClockBackground.TRANSLUCENT,
                    isSelected = selectedBg == ClockBackground.TRANSLUCENT,
                    onClick = { selectedBg = ClockBackground.TRANSLUCENT }
                )
            }
            item {
                PreviewOptionCard(
                    title = stringResource(R.string.clock_bg_transparent),
                    description = stringResource(R.string.clock_bg_transparent_desc),
                    bgType = ClockBackground.TRANSPARENT,
                    isSelected = selectedBg == ClockBackground.TRANSPARENT,
                    onClick = { selectedBg = ClockBackground.TRANSPARENT }
                )
            }
        }
    }
}
