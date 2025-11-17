package com.example.nousappathon

import android.os.Bundle
import kotlin.random.Random
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        // static wheel (shown while no random animation is playing)
        val wheelComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.nothing))

        // your list mapped to names & hex
        val randomColorInfos = listOf(
            ColorInfo(R.raw.red, "Red", "#E63946"),
            ColorInfo(R.raw.orange, "Orange", "#F3722C"),
            ColorInfo(R.raw.yellow, "Yellow", "#F9C74F"),
            ColorInfo(R.raw.green, "Green", "#4CAF50"),
            ColorInfo(R.raw.cyan, "Cyan", "#00BCD4"),
            ColorInfo(R.raw.skyblue, "Blue", "#64B5F6"),
            ColorInfo(R.raw.indigo, "Indigo", "#3F51B5"),
            ColorInfo(R.raw.violet, "Violet", "#9D4EDD")
        )

        // state controlling which color/animation is chosen (null => show wheel)
        var currentInfo by remember { mutableStateOf<ColorInfo?>(null) }

        // whether an animation is currently playing (disable button while true)
        var isPlaying by remember { mutableStateOf(false) }

        // whether the played animation has finished (used to show color text and re-enable button)
        var animationFinished by remember { mutableStateOf(false) }

        // Conditional composition state for the chosen animation; call composable APIs directly
        val randomCompositionState = if (currentInfo != null) {
            rememberLottieComposition(LottieCompositionSpec.RawRes(currentInfo!!.resId))
        } else {
            remember { mutableStateOf<LottieComposition?>(null) }
        }
        val randomComposition by randomCompositionState

        // Use a LottieAnimatable to control playback precisely and detect completion
        val animatable = rememberLottieAnimatable()

        // When a new randomComposition becomes available (i.e., currentInfo was set),
        // launch a coroutine to animate it once, set flags when complete.
        LaunchedEffect(randomComposition) {
            // reset flags if no composition
            if (randomComposition == null) {
                isPlaying = false
                animationFinished = false
                return@LaunchedEffect
            }

            // start playing
            isPlaying = true
            animationFinished = false

            try {
                // animate the chosen composition exactly once
                animatable.animate(
                    composition = randomComposition,
                    iterations = 1, // play only once -> avoids cycling
                )
                // when animate() returns, the animation has finished
                animationFinished = true
            } catch (e: Exception) {
                // if something goes wrong (parse/load error), avoid locking the UI
                animationFinished = true
            } finally {
                isPlaying = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Center animation area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                contentAlignment = Alignment.Center
            ) {
                // If no animation selected -> show static wheel (fixed frame).
                // If animation selected -> show the animatable (plays once).
                if (currentInfo == null) {
                    LottieAnimation(
                        composition = wheelComposition,
                        progress = { 0f }, // static frame
                        modifier = Modifier.size(260.dp)
                    )
                } else {
                    // show the animatable; use its current value (no flicker)
                    LottieAnimation(
                        composition = randomComposition,
                        progress = { animatable.progress },
                        modifier = Modifier.size(300.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show color name + hex ONLY after animationFinished == true
            if (animationFinished && currentInfo != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = currentInfo!!.displayName)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Button directly below the wheel.
            // - disabled while isPlaying is true
            // - on click: choose random, reset flags, and let LaunchedEffect handle playback
            Button(
                onClick = {
                    // pick random info and reset states for a fresh run
                    val pick = randomColorInfos[Random.nextInt(randomColorInfos.size)]
                    currentInfo = pick
                    animationFinished = false
                    // animatable will play when randomComposition becomes available (handled in LaunchedEffect)
                },
                enabled = !isPlaying && !(animationFinished && currentInfo != null) // optionally prevent re-click immediately after finish if you want
            ) {
                Text(text = if (isPlaying) "Playing..." else "Play a Random Animation")
            }
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