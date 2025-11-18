package com.example.nousappathon

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.nousappathon.ui.CameraScreen
import com.example.nousappathon.ui.theme.NousAppathonTheme
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

private val LockedButtonColor = Color(0xFF0B4F3A)
private val CaveatFontFamily = FontFamily(
    Font(R.font.caveat_regular, weight = FontWeight.Normal),
    Font(R.font.caveat_bold, weight = FontWeight.Bold)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NousAppathonTheme {
                MaterialTheme {
                    // use Compose Surface (not android.view.Surface)
                    Surface(modifier = Modifier.fillMaxSize()) {
                        var showCamera by remember { mutableStateOf(false) }

                        // shutter color state (default white)
                        var shutterColor by remember { mutableStateOf(Color.White) }

                        var selectedColorIndex by rememberSaveable { mutableStateOf<Int?>(null) }
                        var isColorConfirmed by rememberSaveable { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!showCamera) {

                                // pass callback so ColorWheel can report its picked color
                                ColorWheelCenterWithRandomPlay(
                                    selectedIndex = selectedColorIndex,
                                    isSelectionConfirmed = isColorConfirmed,
                                    onSelectionChanged = { info ->
                                        val idx = randomColorInfos.indexOf(info)
                                        selectedColorIndex = if (idx >= 0) idx else null
                                        isColorConfirmed = false
                                        try {
                                            val intColor = info.hex.toColorInt()
                                            shutterColor = Color(intColor)
                                        } catch (_: Exception) {
                                            shutterColor = Color.White
                                        }
                                    },
                                    onConfirmationChange = { confirmed ->
                                        isColorConfirmed = confirmed
                                    }
                                )

                                FloatingActionButton(
                                    onClick = { showCamera = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 20.dp, bottom = 30.dp)
                                        .size(56.dp),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 8.dp,
                                        pressedElevation = 12.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Open Camera",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                            } else {
                                CameraScreen(
                                    onBack = { showCamera = false },
                                    shutterOuterColor = shutterColor    // pass the color to CameraScreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- helper data + composable (kept inside Activity as you had) ---
    data class ColorInfo(val resId: Int, val displayName: String, val hex: String)

    private val randomColorInfos = listOf(
        ColorInfo(R.raw.red, "Red", "#E63946"),
        ColorInfo(R.raw.orange, "Orange", "#F3722C"),
        ColorInfo(R.raw.yellow, "Yellow", "#F9C74F"),
        ColorInfo(R.raw.green, "Green", "#4CAF50"),
        ColorInfo(R.raw.cyan, "Cyan", "#00BCD4"),
        ColorInfo(R.raw.skyblue, "Blue", "#64B5F6"),
        ColorInfo(R.raw.indigo, "Indigo", "#3F51B5"),
        ColorInfo(R.raw.violet, "Violet", "#9D4EDD")
    )

    @Composable
    fun ColorWheelCenterWithRandomPlay(
        selectedIndex: Int?,
        isSelectionConfirmed: Boolean,
        onSelectionChanged: (ColorInfo) -> Unit,
        onConfirmationChange: (Boolean) -> Unit
    ) {
        val wheelComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.nothing))
        val currentInfo = selectedIndex?.let { index ->
            randomColorInfos.getOrNull(index)
        }
        var isRandomPlaying by remember { mutableStateOf(false) }
        var animationFinished by remember { mutableStateOf(false) }

        val wheelProgress by animateLottieCompositionAsState(
            composition = wheelComposition,
            isPlaying = (currentInfo == null && !isRandomPlaying),
            iterations = LottieConstants.IterateForever
        )

        val haptic = LocalHapticFeedback.current
        val ctx = LocalContext.current

        val randomComposition by if (currentInfo != null) {
            rememberLottieComposition(LottieCompositionSpec.RawRes(currentInfo.resId))
        } else {
            remember { mutableStateOf<LottieComposition?>(null) }
        }
        val animatable = rememberLottieAnimatable()

        LaunchedEffect(randomComposition, isSelectionConfirmed) {
            val comp = randomComposition ?: run {
                isRandomPlaying = false
                animationFinished = false
                return@LaunchedEffect
            }

            if (isSelectionConfirmed) {
                isRandomPlaying = false
                animationFinished = true
                return@LaunchedEffect
            }

            isRandomPlaying = true
            animationFinished = false

            try {
                animatable.animate(
                    composition = comp,
                    iterations = 1
                )
                animationFinished = true
                vibrateOnce(ctx, durationMs = 70L, amplitude = VibrationEffect.DEFAULT_AMPLITUDE)
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                animationFinished = true
            } finally {
                isRandomPlaying = false
            }
        }

        LaunchedEffect(isSelectionConfirmed, currentInfo) {
            if (currentInfo != null && isSelectionConfirmed) {
                animationFinished = true
            }
        }

        val hasSelection = currentInfo != null
        val getEnabled = !isRandomPlaying && (currentInfo == null || !isSelectionConfirmed)
        val changeEnabled = hasSelection && !isRandomPlaying
        val confirmEnabled = hasSelection && !isSelectionConfirmed

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 80.dp,
                    bottom = 140.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nous",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = CaveatFontFamily,fontSize = 74.sp,          // adjust to taste
                    lineHeight = 68.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))



            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                shape = RoundedCornerShape(36.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentInfo == null) {
                        LottieAnimation(
                            composition = wheelComposition,
                            progress = { wheelProgress },
                            modifier = Modifier.size(400.dp)
                        )
                    } else if (randomComposition != null) {
                        LottieAnimation(
                            composition = randomComposition,
                            progress = { if (isSelectionConfirmed) 1f else animatable.progress },
                            modifier = Modifier.size(400.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(0.dp))

            val selection = currentInfo
            androidx.compose.animation.AnimatedVisibility(
                visible = animationFinished && selection != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selection?.let { chosen ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = chosen.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chosen.hex,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (currentInfo == null) {
                SegmentedActionButton(
                    text = when {
                        isRandomPlaying -> "Generating"
                        else -> "Randomize"
                    },
                    enabled = getEnabled,
                    onClick = {
                        if (!getEnabled) return@SegmentedActionButton
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        vibrateOnce(ctx, 40L)
                        val pick = randomColorInfos[Random.nextInt(randomColorInfos.size)]
                        animationFinished = false
                        onConfirmationChange(false)
                        onSelectionChanged(pick)
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            } else {
                val groupShape = RoundedCornerShape(8.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(groupShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SegmentedActionButton(
                        text = when {
                            isRandomPlaying -> "Generating"
                            animationFinished -> "Randomize"
                            else -> "Generating"
                        },
                        enabled = getEnabled,
                        onClick = {
                            if (!getEnabled) return@SegmentedActionButton
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            vibrateOnce(ctx, 40L)
                            val pick = randomColorInfos[Random.nextInt(randomColorInfos.size)]
                            animationFinished = false
                        onConfirmationChange(false)
                        onSelectionChanged(pick)
                        },
                        shape = RoundedCornerShape(
                            topStart = 28.dp,
                            bottomStart = 28.dp,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        ),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    SegmentedActionButton(
                        text = "Change",
                        enabled = changeEnabled,
                        onClick = {
                            if (!changeEnabled) return@SegmentedActionButton
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            vibrateOnce(ctx, 30L)
                            val pick = randomColorInfos[Random.nextInt(randomColorInfos.size)]
                            animationFinished = false
                        onConfirmationChange(false)
                        onSelectionChanged(pick)
                        },
                        shape = RoundedCornerShape(0.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    SegmentedActionButton(
                        text = if (isSelectionConfirmed) "Locked" else "Confirm",
                        enabled = confirmEnabled,
                        onClick = {
                            if (!confirmEnabled) return@SegmentedActionButton
                            animationFinished = true
                            isRandomPlaying = false
                            onConfirmationChange(true)
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            vibrateOnce(ctx, 30L)
                        },
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = 28.dp,
                            bottomEnd = 28.dp
                        ),
                        containerColor = if (isSelectionConfirmed) LockedButtonColor else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Composable
    private fun SegmentedActionButton(
        text: String,
        enabled: Boolean,
        onClick: () -> Unit,
        shape: RoundedCornerShape,
        containerColor: Color,
        contentColor: Color,
        modifier: Modifier
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = 0.4f),
                disabledContentColor = contentColor.copy(alpha = 0.6f)
            ),
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = modifier
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    // Vibration helper
    fun vibrateOnce(context: android.content.Context, durationMs: Long = 40L, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                val vibrator = vm.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                val vibrator = context.getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        NousAppathonTheme {
            val previewSelection = remember { mutableStateOf<Int?>(null) }
            val previewConfirmed = remember { mutableStateOf(false) }
            ColorWheelCenterWithRandomPlay(
                selectedIndex = previewSelection.value,
                isSelectionConfirmed = previewConfirmed.value,
                onSelectionChanged = { info ->
                    previewSelection.value = randomColorInfos.indexOf(info)
                    previewConfirmed.value = false
                },
                onConfirmationChange = { confirmed ->
                    previewConfirmed.value = confirmed
                }
            )
        }
    }
}

