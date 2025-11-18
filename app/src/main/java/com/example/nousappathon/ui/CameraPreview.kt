package com.example.nousappathon.ui

import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.geometry.Offset

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val frameShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .clip(frameShape)
            .background(Color.Black)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = frameShape
            )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    setBackgroundColor(Color.Black.toArgb())
                }.also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        onImageCaptureReady(imageCapture)

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraPreview", "bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            update = { view ->
                view.scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        )

        // top scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .height(140.dp)
        )

        // bottom scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                )
                .height(160.dp)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color.White.copy(alpha = 0.12f)
            val strokeWidth = 1.dp.toPx()
            val thirdWidth = size.width / 3f
            val thirdHeight = size.height / 3f

            drawLine(
                color = lineColor,
                start = Offset(thirdWidth, 0f),
                end = Offset(thirdWidth, size.height),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(thirdWidth * 2, 0f),
                end = Offset(thirdWidth * 2, size.height),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(0f, thirdHeight),
                end = Offset(size.width, thirdHeight),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(0f, thirdHeight * 2),
                end = Offset(size.width, thirdHeight * 2),
                strokeWidth = strokeWidth
            )
        }
    }
}
