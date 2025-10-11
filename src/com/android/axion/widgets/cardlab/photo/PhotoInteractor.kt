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
package com.android.axion.widgets.cardlab.photo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.content.FileProvider
import com.android.axion.widgets.R
import java.io.File

class PhotoInteractor(internal val context: Context) {

    companion object {
        private const val PREFS_NAME = "photo_widget_prefs"
        private const val PREF_KEY_WIDGET_URIS = "widget_uris"
        private const val TAG = "PhotoInteractor"
    }

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun getAllActiveWidgetIds(): List<Pair<Int, Int>> {
        val smallWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, PhotoWidgetSmallReceiver::class.java)
        ).map { 1 to it }

        val largeWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, PhotoWidgetLargeReceiver::class.java)
        ).map { 2 to it }

        return smallWidgetIds + largeWidgetIds
    }

    fun saveImageUris(appWidgetId: Int, uris: List<Uri>) {
        val allUris = prefs.getStringSet(PREF_KEY_WIDGET_URIS, mutableSetOf())!!.toMutableSet()
        allUris.removeIf { it.startsWith("$appWidgetId|") }
        uris.forEach { uri -> allUris.add("$appWidgetId|$uri") }
        prefs.edit { putStringSet(PREF_KEY_WIDGET_URIS, allUris) }
    }

    fun getImageUris(appWidgetId: Int): List<Uri> {
        val allUris = prefs.getStringSet(PREF_KEY_WIDGET_URIS, emptySet())
        val matches = allUris?.filter { it.startsWith("$appWidgetId|") }
            ?.mapNotNull { Uri.parse(it.substringAfter("|")) } ?: emptyList()
        return if (matches.isEmpty()) getImageUri(appWidgetId)?.let { listOf(it) } ?: emptyList() else matches
    }

    fun saveImageUri(appWidgetId: Int, uri: Uri) = saveImageUris(appWidgetId, listOf(uri))

    fun getImageUri(appWidgetId: Int): Uri? {
        val allUris = prefs.getStringSet(PREF_KEY_WIDGET_URIS, emptySet())
        return allUris?.find { it.startsWith("$appWidgetId|") }?.substringAfter("|")?.let { Uri.parse(it) }
    }

    private fun deleteFileIfExists(uri: Uri) {
        if (uri.scheme == "file" || (uri.scheme == "content" && uri.authority == "${context.packageName}.fileprovider")) {
            try {
                val file = File(uri.path ?: return)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
    }

    fun removeImageUri(widgetId: Int, uri: Uri) {
        val widgetPrefs = context.getSharedPreferences("photo_widget_$widgetId", Context.MODE_PRIVATE)
        val currentUris = widgetPrefs.getStringSet("uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        val uriString = uri.toString()
        if (currentUris.remove(uriString)) {
            widgetPrefs.edit { putStringSet("uris", currentUris) }
        }
        deleteFileIfExists(uri)
    }

    fun removeImageUris(widgetId: Int, urisToRemove: Set<Uri>) {
        val allUris = prefs.getStringSet(PREF_KEY_WIDGET_URIS, mutableSetOf())!!.toMutableSet()
        val urisToRemoveStrings = urisToRemove.map { it.toString() }
        allUris.removeIf { entry ->
            entry.startsWith("$widgetId|") && urisToRemoveStrings.contains(entry.substringAfter("|"))
        }
        prefs.edit { putStringSet(PREF_KEY_WIDGET_URIS, allUris) }
        urisToRemove.forEach { deleteFileIfExists(it) }
    }

    fun removeImageUris(appWidgetId: Int) {
        val allUris = prefs.getStringSet(PREF_KEY_WIDGET_URIS, mutableSetOf())!!.toMutableSet()
        val urisToDelete = allUris.filter { it.startsWith("$appWidgetId|") }
            .mapNotNull { Uri.parse(it.substringAfter("|")) }
        urisToDelete.forEach { deleteFileIfExists(it) }
        allUris.removeIf { it.startsWith("$appWidgetId|") }
        prefs.edit { putStringSet(PREF_KEY_WIDGET_URIS, allUris) }
    }

    fun saveGrayscalePref(appWidgetId: Int, grayscale: Boolean) =
        prefs.edit { putBoolean("grayscale_$appWidgetId", grayscale) }

    fun loadGrayscalePref(appWidgetId: Int): Boolean =
        prefs.getBoolean("grayscale_$appWidgetId", false)

    fun loadBitmapFromUri(uri: Uri): Bitmap? =
        try {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }

    fun toGrayscale(src: Bitmap): Bitmap {
        val bmpGray = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGray)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bmpGray
    }

    fun updateWidget(appWidgetId: Int, bitmap: Bitmap?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_photo)
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.photo_view, bitmap)
        } else {
            views.setImageViewResource(R.id.photo_view, R.drawable.photo_image_rec_2_1)
        }
        getImageUris(appWidgetId).firstOrNull()?.let { imageUri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(imageUri, "image/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.photo_view, pendingIntent)
        }
        return views
    }

    fun copyUrisToFilesAndGetUris(uris: List<Uri>, appWidgetId: Int): List<Uri> {
        return uris.mapNotNullIndexed { index, uri ->
            try {
                val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
                val file = File(imagesDir, "widget_photo_${appWidgetId}_$index.jpg")

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, this) }
                }

                val maxSize = 1080
                val scale = calculateInSampleSize(options, maxSize, maxSize)

                val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                    val scaledOptions = BitmapFactory.Options().apply { inSampleSize = scale }
                    BitmapFactory.decodeStream(input, null, scaledOptions)
                }

                bitmap?.let {
                    file.outputStream().use { out -> it.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun saveShuffleInterval(widgetId: Int, intervalMs: Long) {
        prefs.edit { putLong("shuffle_interval_$widgetId", intervalMs) }
        Log.d(TAG, "New interval $intervalMs")
    }

    fun loadShuffleInterval(widgetId: Int): Long {
        return prefs.getLong("shuffle_interval_$widgetId", 3_600_000L)
    }
}

inline fun <T, R> List<T>.mapNotNullIndexed(transform: (index: Int, T) -> R?): List<R> {
    val list = ArrayList<R>()
    forEachIndexed { index, t -> transform(index, t)?.let { list.add(it) } }
    return list
}
