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

package com.android.axion.widgets.cardlab.photo

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.android.axion.compose.scaffold.AxionPinnedTopAppBar
import com.android.axion.widgets.R
import kotlinx.coroutines.*

class PhotoWidgetConfigureActivity : ComponentActivity() {

    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        widgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(Activity.RESULT_CANCELED)

        setContent {
            val context = LocalContext.current
            val interactor = remember { PhotoInteractor(context) }
            val isDarkTheme = isSystemInDarkTheme()
            val colorScheme =
                if (isDarkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)

            MaterialExpressiveTheme(
                colorScheme = colorScheme,
                motionScheme = MotionScheme.expressive(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    PhotoWidgetConfigureScreen(widgetId, interactor) { success ->
                        if (success) {
                            val resultValue =
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            setResult(Activity.RESULT_OK, resultValue)
                        }
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoWidgetConfigureScreen(
    widgetId: Int,
    interactor: PhotoInteractor,
    onFinish: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var grayscale by remember { mutableStateOf(interactor.loadGrayscalePref(widgetId)) }
    val selectedUris = remember {
        mutableStateListOf<Uri>().apply { addAll(interactor.getImageUris(widgetId)) }
    }
    var showManageScreen by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf(interactor.loadShuffleInterval(widgetId)) }
    var allPhotosLoaded by remember { mutableStateOf(false) }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                val newUris = uris.filter { it !in selectedUris }
                val omittedCount = uris.size - newUris.size
                if (newUris.isNotEmpty()) {
                    selectedUris.addAll(newUris)
                    allPhotosLoaded = false
                }
                if (omittedCount > 0) {
                    Toast.makeText(
                            context,
                            context.resources.getQuantityString(
                                R.plurals.photos_already_exist,
                                omittedCount,
                                omittedCount,
                            ),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }
            }
        }

    if (showManageScreen) {
        PhotoManageScreen(
            widgetId = widgetId,
            selectedUris = selectedUris,
            onBack = { showManageScreen = false },
            onAllDeleted = {
                val views = interactor.updateWidget(widgetId, null)
                AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
            },
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(98.dp))
            Text(
                text = stringResource(id = R.string.photo_title),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 32.sp,
                fontFamily = FontFamily(Typeface.create("nothingdot57", Typeface.NORMAL)),
            )
            Spacer(modifier = Modifier.height(50.dp))

            PhotoSelectionSection(
                selectedUris = selectedUris,
                grayscale = grayscale,
                interactor = interactor,
                onManageClick = { showManageScreen = true },
                onAddPhotosClick = { imagePickerLauncher.launch("image/*") },
                onPhotoLoaded = { allPhotosLoaded = true },
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = stringResource(R.string.display_options),
                style = MaterialTheme.typography.titleSmall,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(28.dp))

            DisplayOptionsSection(grayscale = grayscale, onGrayscaleChange = { grayscale = it })

            Spacer(modifier = Modifier.height(24.dp))

            ShuffleIntervalSelector(
                widgetId = widgetId,
                selectedUris = selectedUris,
                selectedInterval = selectedInterval,
                onIntervalChange = { selectedInterval = it },
            )

            Spacer(modifier = Modifier.height(24.dp))

            SaveButtonSection(
                widgetId = widgetId,
                selectedUris = selectedUris,
                grayscale = grayscale,
                selectedInterval = selectedInterval,
                interactor = interactor,
                scope = scope,
                context = context,
                onFinish = onFinish,
            )
        }
    }
}

@Composable
fun PhotoSelectionSection(
    selectedUris: List<Uri>,
    grayscale: Boolean,
    interactor: PhotoInteractor,
    onManageClick: () -> Unit,
    onAddPhotosClick: () -> Unit,
    onPhotoLoaded: (() -> Unit)? = null,
) {
    val bitmaps =
        rememberBitmaps(selectedUris.toList(), grayscale, interactor, 88.dp, onPhotoLoaded)

    AnimatedVisibility(
        visible = selectedUris.isNotEmpty(),
        enter =
            slideInVertically(
                initialOffsetY = { fullHeight -> -fullHeight },
                animationSpec = tween(400, easing = FastOutSlowInEasing),
            ),
        exit =
            slideOutVertically(
                targetOffsetY = { fullHeight -> -fullHeight },
                animationSpec = tween(400, easing = FastOutSlowInEasing),
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val displayCount = minOf(selectedUris.size, 4)

            for (index in 0 until displayCount) {
                if (index == 3 && selectedUris.size > 4) {
                    val extraCount = selectedUris.size - 4
                    Box(
                        modifier =
                            Modifier.size(88.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onManageClick() }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+$extraCount",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily =
                                FontFamily(Typeface.create("nothingdot57", Typeface.NORMAL)),
                        )
                    }
                } else {
                    val uri = selectedUris[index]
                    val bmp = bitmaps[uri]

                    Box(
                        modifier =
                            Modifier.size(88.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onManageClick() }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds,
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    Row(
        modifier =
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 14.dp).clickable {
                onAddPhotosClick()
            },
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.add_photos_cd),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(30.dp))
        Text(
            text = stringResource(R.string.add_photos),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 20.sp,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
fun DisplayOptionsSection(grayscale: Boolean, onGrayscaleChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.grayscale_mode),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = grayscale, onCheckedChange = onGrayscaleChange)
    }
}

@Composable
fun ShuffleIntervalSelector(
    widgetId: Int,
    selectedUris: List<Uri>,
    selectedInterval: Long,
    onIntervalChange: (Long) -> Unit,
) {
    val intervals =
        listOf(
            60_000L to stringResource(R.string.interval_1_minute),
            300_000L to stringResource(R.string.interval_5_minutes),
            900_000L to stringResource(R.string.interval_15_minutes),
            3_600_000L to stringResource(R.string.interval_1_hour),
        )
    val canShuffle = selectedUris.size > 1

    var showIntervalDialog by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.shuffle_interval),
        style = MaterialTheme.typography.titleSmall,
        fontSize = 12.sp,
    )
    Spacer(modifier = Modifier.height(12.dp))

    TextButton(
        onClick = { if (canShuffle) showIntervalDialog = true },
        enabled = canShuffle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Text(intervals.find { it.first == selectedInterval }?.second ?: "Select interval")
        }
    }

    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text(stringResource(R.string.select_shuffle_interval)) },
            text = {
                Column {
                    intervals.forEach { (value, label) ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        onIntervalChange(value)
                                        showIntervalDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedInterval == value,
                                onClick = {
                                    onIntervalChange(value)
                                    showIntervalDialog = false
                                },
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntervalDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun SaveButtonSection(
    widgetId: Int,
    selectedUris: List<Uri>,
    grayscale: Boolean,
    selectedInterval: Long,
    interactor: PhotoInteractor,
    scope: CoroutineScope,
    context: Context,
    onFinish: (Boolean) -> Unit,
) {
    Button(
        onClick = {
            if (selectedUris.isNotEmpty()) {
                scope.launch {
                    val copiedUris = interactor.copyUrisToFilesAndGetUris(selectedUris, widgetId)
                    if (copiedUris.isNotEmpty()) {
                        interactor.saveImageUris(widgetId, copiedUris)
                        interactor.saveGrayscalePref(widgetId, grayscale)
                        interactor.saveShuffleInterval(widgetId, selectedInterval)
                        val firstBitmap =
                            interactor.loadBitmapFromUri(copiedUris.first())?.let {
                                if (grayscale) interactor.toGrayscale(it) else it
                            }
                        val views = interactor.updateWidget(widgetId, firstBitmap)
                        AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
                        onFinish(true)
                    } else {
                        onFinish(false)
                    }
                }
            } else {
                onFinish(false)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.save_changes))
    }
}

@Composable
fun PhotoManageScreen(
    widgetId: Int,
    selectedUris: MutableList<Uri>,
    onBack: () -> Unit,
    onAllDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val interactor = remember { PhotoInteractor(context) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedForDelete by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val bitmaps =
        rememberBitmaps(uris = selectedUris, grayscale = false, interactor = interactor, 180.dp)

    BackHandler {
        if (selectionMode && selectedForDelete.isNotEmpty()) {
            selectedForDelete = emptySet()
            selectionMode = false
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            PhotoManageTopBar(
                selectionMode = selectionMode,
                hasSelection = selectedForDelete.isNotEmpty(),
                onBack = onBack,
                onDeleteClicked = { showConfirmDialog = true },
            )
        }
    ) { padding ->
        PhotoGrid(
            uris = selectedUris,
            bitmaps = bitmaps,
            selectionMode = selectionMode,
            selectedForDelete = selectedForDelete,
            onSelectUri = { uri ->
                val isSelected = uri in selectedForDelete
                selectedForDelete =
                    if (isSelected) selectedForDelete - uri else selectedForDelete + uri
                if (selectedForDelete.isEmpty()) selectionMode = false
            },
            onStartSelection = { uri ->
                if (!selectionMode) {
                    selectionMode = true
                    selectedForDelete = setOf(uri)
                }
            },
            modifier = Modifier.padding(padding),
        )
    }

    if (showConfirmDialog) {
        ConfirmDeleteDialog(
            count = selectedForDelete.size,
            onConfirm = {
                selectedUris.removeAll(selectedForDelete)
                interactor.removeImageUris(widgetId, selectedForDelete)
                selectedForDelete = emptySet()
                selectionMode = false
                showConfirmDialog = false
                if (selectedUris.isEmpty()) {
                    val views = interactor.updateWidget(widgetId, null)
                    AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
                    onAllDeleted()
                }
            },
            onDismiss = { showConfirmDialog = false },
        )
    }
}

@Composable
fun PhotoManageTopBar(
    selectionMode: Boolean,
    hasSelection: Boolean,
    onBack: () -> Unit,
    onDeleteClicked: () -> Unit,
) {
    AxionPinnedTopAppBar(
        title = stringResource(R.string.manage_photos),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        actions = {
            if (selectionMode && hasSelection) {
                IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
        },
    )
}

@Composable
fun PhotoGrid(
    uris: List<Uri>,
    bitmaps: Map<Uri, Bitmap?>,
    selectionMode: Boolean,
    selectedForDelete: Set<Uri>,
    onSelectUri: (Uri) -> Unit,
    onStartSelection: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(uris) { uri ->
            val isSelected = uri in selectedForDelete
            val scale by animateFloatAsState(targetValue = if (isSelected) 0.9f else 1f)

            Box(
                modifier =
                    Modifier.aspectRatio(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { if (selectionMode) onSelectUri(uri) },
                            onLongClick = { onStartSelection(uri) },
                        )
            ) {
                val bmp = bitmaps[uri]
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp).align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_delete_title)) },
        text = { Text(stringResource(R.string.confirm_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
fun rememberBitmaps(
    uris: List<Uri>,
    grayscale: Boolean,
    interactor: PhotoInteractor,
    targetSizeDp: Dp,
    onPhotoLoaded: (() -> Unit)? = null,
): Map<Uri, Bitmap?> {
    val density = LocalDensity.current
    val targetSizePx = with(density) { targetSizeDp.roundToPx() }
    val bitmaps = remember { mutableStateMapOf<Uri, Bitmap?>() }
    val loadedCount = remember { mutableStateOf(0) }
    LaunchedEffect(uris, grayscale) {
        val toRemove = bitmaps.keys - uris.toSet()
        toRemove.forEach { bitmaps.remove(it) }
        loadedCount.value = 0
        uris.forEach { uri ->
            val bitmap =
                withContext(Dispatchers.IO) {
                    interactor.loadBitmapFromUri(uri)?.let { bmp ->
                        val bmpWidth = bmp.width
                        val bmpHeight = bmp.height
                        val targetAspectRatio = 1f
                        val bmpAspectRatio = bmpWidth.toFloat() / bmpHeight
                        val cropWidth: Int
                        val cropHeight: Int
                        val cropLeft: Int
                        val cropTop: Int
                        if (bmpAspectRatio > targetAspectRatio) {
                            cropHeight = bmpHeight
                            cropWidth = (bmpHeight * targetAspectRatio).toInt()
                            cropLeft = (bmpWidth - cropWidth) / 2
                            cropTop = 0
                        } else {
                            cropWidth = bmpWidth
                            cropHeight = (bmpWidth / targetAspectRatio).toInt()
                            cropLeft = 0
                            cropTop = (bmpHeight - cropHeight) / 2
                        }
                        val croppedBmp =
                            Bitmap.createBitmap(
                                bmp,
                                cropLeft.coerceAtLeast(0),
                                cropTop.coerceAtLeast(0),
                                cropWidth.coerceAtLeast(1),
                                cropHeight.coerceAtLeast(1),
                            )
                        Bitmap.createScaledBitmap(croppedBmp, targetSizePx, targetSizePx, true)
                            .let { if (grayscale) interactor.toGrayscale(it) else it }
                    }
                }
            withContext(Dispatchers.Main) {
                bitmaps[uri] = bitmap
                loadedCount.value = loadedCount.value + 1
            }
        }
    }
    LaunchedEffect(loadedCount.value, uris.size) {
        if (uris.isNotEmpty() && loadedCount.value >= uris.size) {
            onPhotoLoaded?.invoke()
        }
    }
    return bitmaps
}
