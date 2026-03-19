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

package com.android.axion.widgets

import android.Manifest
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.android.axion.compose.preferences.ListPreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.preferences.SwitchPreference
import com.android.axion.compose.scaffold.AxionScaffold
import com.android.axion.compose.theme.AxionTheme
import com.android.axion.widgets.manager.QuickLookDataManager
import com.android.axion.widgets.quicklook.MessageProvider
import com.android.axion.widgets.quicklook.QuickLookPrefs

class SettingsActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId =
            intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        enableEdgeToEdge()
        setContent {
            AxionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                SettingsScreen(
                    onApply = {
                        refreshWidget(this)
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finish()
                    }
                )
                }
            }
        }
    }
}

private fun refreshWidget(context: Context) {
    QuickLookDataManager.get(context).onDataUpdated()
}

@Composable
private fun SettingsScreen(onApply: () -> Unit) {
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                        context,
                        context.getString(R.string.toast_calendar_permission_required),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }

    val calendarGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val showDate = remember { mutableStateOf(QuickLookPrefs.showDate(context)) }
    val dateFormat = remember { mutableIntStateOf(QuickLookPrefs.getDateFormat(context)) }
    val calLookahead = remember { mutableIntStateOf(QuickLookPrefs.getCalendarLookahead(context)) }
    val msgInterval = remember { mutableIntStateOf(QuickLookPrefs.getMessageInterval(context)) }

    val calendarEnabled = remember {
        mutableStateOf(QuickLookPrefs.isEnabled(context, QuickLookPrefs.SOURCE_CALENDAR))
    }
    val mediaEnabled = remember {
        mutableStateOf(QuickLookPrefs.isEnabled(context, QuickLookPrefs.SOURCE_MEDIA))
    }
    val batteryEnabled = remember {
        mutableStateOf(QuickLookPrefs.isEnabled(context, QuickLookPrefs.SOURCE_BATTERY))
    }
    val weatherEnabled = remember {
        mutableStateOf(QuickLookPrefs.isEnabled(context, QuickLookPrefs.SOURCE_WEATHER))
    }
    val messagesEnabled = remember {
        mutableStateOf(QuickLookPrefs.isEnabled(context, QuickLookPrefs.SOURCE_MESSAGES))
    }

    val dateFormatOptions =
        listOf(
            "0" to stringResource(R.string.ql_date_default),
            "1" to stringResource(R.string.ql_date_short),
            "2" to stringResource(R.string.ql_date_full),
        )
    val lookaheadOptions =
        listOf(
            "15" to stringResource(R.string.ql_lookahead_15),
            "30" to stringResource(R.string.ql_lookahead_30),
            "60" to stringResource(R.string.ql_lookahead_60),
            "120" to stringResource(R.string.ql_lookahead_120),
        )
    val msgIntervalOptions =
        listOf(
            "30" to stringResource(R.string.ql_msg_30m),
            "60" to stringResource(R.string.ql_msg_1h),
            "180" to stringResource(R.string.ql_msg_3h),
            "360" to stringResource(R.string.ql_msg_6h),
            "1440" to stringResource(R.string.ql_msg_daily),
        )

    AxionScaffold(
        title = stringResource(R.string.setup_title),
        onBackClick = onApply,
        collapsedByDefault = false,
        actions = {
            IconButton(onClick = onApply) {
                Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.ql_apply))
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!calendarGranted.value) {
                PreferenceGroup {
                    item {
                        SwitchPreference(
                            title = stringResource(R.string.allow_calendar_access),
                            summary = stringResource(R.string.needed_for_calendar_events),
                            icon = Icons.Filled.Security,
                            checked = false,
                            onCheckedChange = {
                                permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            },
                        )
                    }
                }
            }

            PreferenceGroup(title = stringResource(R.string.ql_display)) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.ql_show_date),
                        summary = stringResource(R.string.ql_show_date_desc),
                        icon = Icons.Filled.Today,
                        checked = showDate.value,
                        onCheckedChange = {
                            showDate.value = it
                            QuickLookPrefs.setShowDate(context, it)
                            refreshWidget(context)
                        },
                    )
                }
                item {
                    ListPreference(
                        title = stringResource(R.string.ql_date_format),
                        summary =
                            dateFormatOptions
                                .first { it.first == dateFormat.intValue.toString() }
                                .second,
                        options = dateFormatOptions,
                        value = dateFormat.intValue.toString(),
                        enabled = showDate.value,
                        onValueChange = {
                            val v = it.toInt()
                            dateFormat.intValue = v
                            QuickLookPrefs.setDateFormat(context, v)
                            refreshWidget(context)
                        },
                    )
                }
            }

            PreferenceGroup(title = stringResource(R.string.ql_data_sources)) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.ql_source_calendar),
                        summary = stringResource(R.string.ql_source_calendar_desc),
                        icon = Icons.Filled.CalendarToday,
                        checked = calendarEnabled.value,
                        onCheckedChange = {
                            calendarEnabled.value = it
                            QuickLookPrefs.setEnabled(context, QuickLookPrefs.SOURCE_CALENDAR, it)
                            refreshWidget(context)
                        },
                    )
                }
                item {
                    SwitchPreference(
                        title = stringResource(R.string.ql_source_media),
                        summary = stringResource(R.string.ql_source_media_desc),
                        icon = Icons.Filled.MusicNote,
                        checked = mediaEnabled.value,
                        onCheckedChange = {
                            mediaEnabled.value = it
                            QuickLookPrefs.setEnabled(context, QuickLookPrefs.SOURCE_MEDIA, it)
                            refreshWidget(context)
                        },
                    )
                }
                item {
                    SwitchPreference(
                        title = stringResource(R.string.ql_source_battery),
                        summary = stringResource(R.string.ql_source_battery_desc),
                        icon = Icons.Filled.BatteryChargingFull,
                        checked = batteryEnabled.value,
                        onCheckedChange = {
                            batteryEnabled.value = it
                            QuickLookPrefs.setEnabled(context, QuickLookPrefs.SOURCE_BATTERY, it)
                            refreshWidget(context)
                        },
                    )
                }
                item {
                    SwitchPreference(
                        title = stringResource(R.string.ql_source_weather),
                        summary = stringResource(R.string.ql_source_weather_desc),
                        icon = Icons.Filled.Cloud,
                        checked = weatherEnabled.value,
                        onCheckedChange = {
                            weatherEnabled.value = it
                            QuickLookPrefs.setEnabled(context, QuickLookPrefs.SOURCE_WEATHER, it)
                            refreshWidget(context)
                        },
                    )
                }
                item {
                    SwitchPreference(
                        title = stringResource(R.string.ql_source_messages),
                        summary = stringResource(R.string.ql_source_messages_desc),
                        icon = Icons.Filled.Lightbulb,
                        checked = messagesEnabled.value,
                        onCheckedChange = {
                            messagesEnabled.value = it
                            QuickLookPrefs.setEnabled(context, QuickLookPrefs.SOURCE_MESSAGES, it)
                            refreshWidget(context)
                        },
                    )
                }
            }

            if (calendarEnabled.value) {
                PreferenceGroup(title = stringResource(R.string.ql_cal_settings)) {
                    item {
                        ListPreference(
                            title = stringResource(R.string.ql_cal_lookahead),
                            summary =
                                lookaheadOptions
                                    .first { it.first == calLookahead.intValue.toString() }
                                    .second,
                            options = lookaheadOptions,
                            value = calLookahead.intValue.toString(),
                            onValueChange = {
                                val v = it.toInt()
                                calLookahead.intValue = v
                                QuickLookPrefs.setCalendarLookahead(context, v)
                                refreshWidget(context)
                            },
                        )
                    }
                }
            }

            if (messagesEnabled.value) {
                PreferenceGroup(title = stringResource(R.string.ql_msg_settings)) {
                    item {
                        ListPreference(
                            title = stringResource(R.string.ql_msg_interval),
                            summary =
                                msgIntervalOptions
                                    .first { it.first == msgInterval.intValue.toString() }
                                    .second,
                            options = msgIntervalOptions,
                            value = msgInterval.intValue.toString(),
                            onValueChange = {
                                val v = it.toInt()
                                msgInterval.intValue = v
                                QuickLookPrefs.setMessageInterval(context, v)
                                val mp = (context.applicationContext as AxionApp)
                                    .appComponent.messageProvider()
                                mp.scheduleRotation()
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
