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

package com.android.axion.widgets.cardlab.countdown

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CountdownConfigureActivity : ComponentActivity() {

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

        val existing = CountdownPrefs.get(this, widgetId)

        setContent {
            CountdownTheme {
                CountdownConfigScreen(existing) { config ->
                    CountdownPrefs.set(this, widgetId, config)
                    AxCountdownReceiver.updateWidget(this, widgetId, config)
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
private fun CountdownTheme(content: @Composable () -> Unit) {
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

@Composable
private fun CountdownConfigScreen(existing: CountdownConfig?, onSave: (CountdownConfig) -> Unit) {
    val context = LocalContext.current
    var eventName by remember { mutableStateOf(existing?.eventName ?: "") }
    var selectedDateMillis by remember {
        mutableStateOf(
            existing?.targetDateMillis
                ?: LocalDate.now()
                    .plusDays(7)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedDate =
        remember(selectedDateMillis) {
            Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Countdown",
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("Event name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Target date",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text =
                            "%d %s %d"
                                .format(
                                    selectedDate.dayOfMonth,
                                    selectedDate.month.name
                                        .lowercase()
                                        .replaceFirstChar { it.uppercase() }
                                        .take(3),
                                    selectedDate.year,
                                ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (eventName.isNotBlank()) {
                        onSave(CountdownConfig(eventName.trim(), selectedDateMillis))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = eventName.isNotBlank(),
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showDatePicker) {
            val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
