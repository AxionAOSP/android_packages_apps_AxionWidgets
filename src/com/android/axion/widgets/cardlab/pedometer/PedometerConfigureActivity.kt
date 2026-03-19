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

package com.android.axion.widgets.cardlab.pedometer

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import java.text.NumberFormat
import kotlin.math.roundToInt

class PedometerConfigureActivity : ComponentActivity() {

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

        val existingGoal = PedometerPrefs.getGoal(this, widgetId)

        setContent {
            PedometerTheme {
                PedometerConfigScreen(existingGoal) { goal ->
                    PedometerPrefs.setGoal(this, widgetId, goal)
                    AxPedometerReceiver.updateWidget(this, widgetId)
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
private fun PedometerTheme(content: @Composable () -> Unit) {
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
private fun PedometerConfigScreen(initialGoal: Int, onSave: (Int) -> Unit) {
    val context = LocalContext.current
    val numberFormat = remember { NumberFormat.getNumberInstance() }
    var sliderValue by remember { mutableFloatStateOf(initialGoal.toFloat()) }
    val goal = sliderValue.roundToInt()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Pedometer",
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
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Daily step goal",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = numberFormat.format(goal),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily(Typeface.create("nothingdot57", Typeface.NORMAL)),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "steps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 1000f..50000f,
                steps = 48,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "1,000",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Text(
                    text = "50,000",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { onSave(goal) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
