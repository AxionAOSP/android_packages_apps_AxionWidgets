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
package com.android.axion.widgets

import android.app.Activity.RESULT_OK
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class SettingsActivity : ComponentActivity() {

    private val calendarPermission = Manifest.permission.READ_CALENDAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_OK)

        enableEdgeToEdge()

        setContent {
            val isDark = isSystemInDarkTheme()
            val context = LocalContext.current

            val window = (context as? ComponentActivity)?.window
            DisposableEffect(isDark) {
                window?.let {
                    val wic = WindowCompat.getInsetsController(it, it.decorView)
                    wic.isAppearanceLightStatusBars = !isDark
                }
                onDispose { setResult(RESULT_OK) }
            }

            val bgColorRes =
                if (isDark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_50
            val cardColorRes =
                if (isDark) android.R.color.system_neutral1_800 else android.R.color.system_neutral1_0

            val bgColor = Color(ContextCompat.getColor(context, bgColorRes))
            val cardColor = Color(ContextCompat.getColor(context, cardColorRes))

            MaterialTheme(
                colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = bgColor
                ) {
                    SettingsScreen(
                        calendarPermission = calendarPermission,
                        cardBgColor = cardColor,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    calendarPermission: String,
    cardBgColor: Color,
    isDark: Boolean
) {
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.toast_calendar_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionGranted = remember { mutableStateOf(false) }
    val calendarPermissionGranted = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        notificationPermissionGranted.value = isNotificationListenerEnabled(context)
        calendarPermissionGranted.value = ContextCompat.checkSelfPermission(
            context,
            calendarPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.setup_title),
                        style = TextStyle(
                            fontFamily = FontFamily(
                                Typeface.create("nothingdot57", Typeface.NORMAL)
                            ),
                            fontSize = 36.sp
                        )
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = if (isDark) Color.White else Color.Black
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    activity?.setResult(RESULT_OK)
                    activity?.finish()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Exit")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            LazyHorizontalGrid(
                rows = GridCells.Fixed(1),
                modifier = Modifier.height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                content = {
                    item {
                        BoxWithConstraints {
                            val cardWidth = maxWidth / 2 - 8.dp
                            SettingsCard(
                                icon = Icons.Filled.Notifications,
                                title = stringResource(id = R.string.allow_notification_access),
                                subtitle = if (notificationPermissionGranted.value)
                                    stringResource(id = R.string.access_granted)
                                else
                                    stringResource(id = R.string.needed_for_now_playing),
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                },
                                backgroundColor = cardBgColor,
                                textColor = if (isDark) Color.White else Color.Black,
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                    }
                    item {
                        BoxWithConstraints {
                            val cardWidth = maxWidth / 2 - 8.dp
                            SettingsCard(
                                icon = Icons.Filled.CalendarToday,
                                title = stringResource(id = R.string.allow_calendar_access),
                                subtitle = if (calendarPermissionGranted.value)
                                    stringResource(id = R.string.access_granted)
                                else
                                    stringResource(id = R.string.needed_for_calendar_events),
                                onClick = {
                                    if (!calendarPermissionGranted.value) {
                                        permissionLauncher.launch(calendarPermission)
                                    }
                                },
                                backgroundColor = cardBgColor,
                                textColor = if (isDark) Color.White else Color.Black,
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily(
                            Typeface.create("sans-serif", Typeface.NORMAL)
                        ),
                        fontSize = 18.sp
                    ),
                    color = textColor,
                    maxLines = 2
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = FontFamily(
                            Typeface.create("sans-serif", Typeface.NORMAL)
                        ),
                        fontSize = 13.sp
                    ),
                    color = textColor,
                    maxLines = 2
                )
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    return enabledListeners.contains(context.packageName)
}
