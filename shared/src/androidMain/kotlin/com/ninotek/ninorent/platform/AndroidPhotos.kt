package com.ninotek.ninorent.platform

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberGalleryPicker(filePrefix: String, onPhoto: (PickedPhoto) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onPhoto)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val savedUri = try {
                val file = File(context.filesDir, "${filePrefix}_${currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                file.toURI().toString()
            } catch (e: Exception) {
                e.printStackTrace()
                uri.toString()
            }
            callback.value(PickedPhoto(savedUri, null))
        }
    }
    return { launcher.launch("image/*") }
}

@Composable
actual fun rememberCameraCapture(filePrefix: String, onPhoto: (PickedPhoto) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onPhoto)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.filesDir, "${filePrefix}_${currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                callback.value(PickedPhoto(file.toURI().toString(), bitmap.asImageBitmap()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    return { launcher.launch(null) }
}
