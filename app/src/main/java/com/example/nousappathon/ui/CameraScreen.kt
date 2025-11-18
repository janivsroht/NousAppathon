package com.example.nousappathon.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CameraScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // permissions
    var cameraGranted by remember { mutableStateOf(false) }
    var storageGranted by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        cameraGranted = perms[Manifest.permission.CAMERA] ?: false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            storageGranted = perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        } else {
            storageGranted = true
        }
    }

    LaunchedEffect(Unit) {
        val toRequest = mutableListOf<String>()
        toRequest.add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            toRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(toRequest.toTypedArray())
    }

    // ensure system back acts same as onBack
    BackHandler(enabled = true) { onBack() }

    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraGranted && storageGranted) {
            // Camera preview (fills screen) and gives ImageCapture instance via callback
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onImageCaptureReady = { ic -> imageCaptureRef = ic }
            )

            // Top-left back arrow
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            // Top-right: small gallery icon (placeholder)
            IconButton(
                onClick = { /* optionally open gallery preview later */ },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.Photo, contentDescription = "Gallery")
            }

            // Bottom center: typical camera shutter
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color = Color(0xFF3F51B5), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner white circle (shutter)
                    IconButton(
                        onClick = {
                            if (isSaving) return@IconButton
                            val ic = imageCaptureRef ?: return@IconButton
                            isSaving = true
                            // Save to gallery (async)
                            savePhotoToGallery(
                                context = context,
                                imageCapture = ic,
                                onSuccess = { uriStr ->
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(context, "Saved: $uriStr", Toast.LENGTH_SHORT).show()
                                        isSaving = false
                                    }
                                },
                                onError = { exc ->
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(context, "Save failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                                        isSaving = false
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(color = MaterialTheme.colorScheme.onPrimary, shape = CircleShape)
                    ) {
                        // no icon inside to mimic camera shutter; you can put an icon if you prefer
                    }
                }
            }

            // If saving, show small indicator (optional)
            if (isSaving) {
                Text(
                    "Saving...",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 160.dp)
                )
            }

        } else {
            // permission prompt UI
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission required")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val toRequest = mutableListOf<String>()
                        toRequest.add(Manifest.permission.CAMERA)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            toRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(toRequest.toTypedArray())
                    }) {
                        Text("Grant permissions")
                    }
                }
            }
        }
    }
}

/**
 * Save using ImageCapture + MediaStore so the image ends up in the user's gallery.
 */
fun savePhotoToGallery(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val filename = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NousAppathon")
        }
    }

    val resolver = context.contentResolver
    val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(resolver, contentUri, contentValues)
        .build()

    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                onSuccess(savedUri?.toString() ?: "unknown")
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
