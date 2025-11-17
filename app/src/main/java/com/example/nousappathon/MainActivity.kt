package com.example.nousappathon

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import kotlin.random.Random
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.nousappathon.ui.theme.NousAppathonTheme
import kotlinx.coroutines.flow.collectLatest
import kotlin.coroutines.cancellation.CancellationException


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NousAppathonTheme {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ColorWheelCenterWithRandomPlay()
                    }
                }
            }
        }
    }
    data class ColorInfo(val resId: Int, val displayName: String, val hex: String)
    @Composable
    fun ColorWheelCenterWithRandomPlay() {
        // static wheel composition (but will be looped via animateLottieCompositionAsState)
        val wheelComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.nothing))

        // your color/animation list
        val randomColorInfos = remember {
            listOf(
                ColorInfo(R.raw.red, "Red", "#E63946"),
                ColorInfo(R.raw.orange, "Orange", "#F3722C"),
                ColorInfo(R.raw.yellow, "Yellow", "#F9C74F"),
                ColorInfo(R.raw.green, "Green", "#4CAF50"),
                ColorInfo(R.raw.cyan, "Cyan", "#00BCD4"),
                ColorInfo(R.raw.skyblue, "Blue", "#64B5F6"),
                ColorInfo(R.raw.indigo, "Indigo", "#3F51B5"),
                ColorInfo(R.raw.violet, "Violet", "#9D4EDD")
            )
        }

        // currently chosen color/animation (null means "no random chosen" -> show looping wheel)
        var currentInfo by remember { mutableStateOf<ColorInfo?>(null) }

        // whether a random animation is actively playing (used to disable button)
        var isRandomPlaying by remember { mutableStateOf(false) }

        // whether the random animation finished (used to show text and re-enable button)
        var animationFinished by remember { mutableStateOf(false) }

        // --- Wheel playback: loop while no random animation is chosen/playing ---
        val wheelProgress by animateLottieCompositionAsState(
            composition = wheelComposition,
            // play wheel when currentInfo == null and a random isn't playing
            isPlaying = (currentInfo == null && !isRandomPlaying),
            iterations = LottieConstants.IterateForever
        )

        // HAPTICS setup
        val haptic = LocalHapticFeedback.current
        val ctx = LocalContext.current

        // --- Conditional load of the chosen random composition (call composable functions directly) ---
        val randomCompositionState = if (currentInfo != null) {
            rememberLottieComposition(LottieCompositionSpec.RawRes(currentInfo!!.resId))
        } else {
            remember { mutableStateOf<LottieComposition?>(null) }
        }
        val randomComposition by randomCompositionState

        // LottieAnimatable to play the chosen composition once and detect completion
        val animatable = rememberLottieAnimatable()

        // When randomComposition becomes available, play it once in a coroutine and set flags
        LaunchedEffect(randomComposition) {
            val comp = randomComposition ?: run {
                isRandomPlaying = false
                animationFinished = false
                return@LaunchedEffect
            }

            isRandomPlaying = true
            animationFinished = false

            try {
                // play once
                animatable.animate(
                    composition = comp,
                    iterations = 1
                )
                // finished playing -> selection made
                animationFinished = true

                // stronger vibration to indicate selection
                vibrateOnce(ctx, durationMs = 70L, amplitude = VibrationEffect.DEFAULT_AMPLITUDE)
                // optional: light haptic "confirm" as well
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Center area for wheel / random animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                contentAlignment = Alignment.Center
            ) {
                // Show wheel when no random chosen; else show animatable driven composition
                if (currentInfo == null) {
                    // looping wheel (no flicker)
                    LottieAnimation(
                        composition = wheelComposition,
                        progress = { wheelProgress },
                        modifier = Modifier.size(380.dp)
                    )
                } else {
                    // show the animating random composition driven by animatable (no flicker)
                    if (randomComposition != null) {
                        LottieAnimation(
                            composition = randomComposition,
                            progress = { animatable.progress },
                            modifier = Modifier.size(380.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = (animationFinished && currentInfo != null),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(currentInfo?.displayName ?: "")
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button
            val buttonEnabled = !isRandomPlaying && (currentInfo == null || animationFinished)

            Button(
                onClick = {
                    if (!buttonEnabled) return@Button

                    // immediate haptic on button press
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    vibrateOnce(ctx, 40L)

                    // pick a random color animation and reset flags
                    val pick = randomColorInfos[Random.nextInt(randomColorInfos.size)]
                    currentInfo = pick
                    animationFinished = false
                    // animatable playback will start automatically because randomComposition changes and triggers LaunchedEffect
                },
                enabled = buttonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = when {
                        isRandomPlaying -> "Generating..."
                        currentInfo == null -> "Get a Random Color"
                        animationFinished -> "Get a Random Color"
                        else -> "Generating..."
                    }
                )
            }
        }
    }

    // Vibration helper (add near top-level of file, inside the Activity file as you had earlier)
    fun vibrateOnce(context: android.content.Context, durationMs: Long = 40L, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                val vibrator = vm.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }





    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        NousAppathonTheme {
            ColorWheelCenterWithRandomPlay()
        }
    }
}