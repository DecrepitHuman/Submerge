package net.nicholas.submerge.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text

@Composable
fun NitrogenLoadingBar(
    modifier: Modifier = Modifier,
    progress: Float, // From 0.0 - 1.0
    strokeWidth: Float = 10f,
    color: Color = Color.Cyan
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val size = size.minDimension
            val radius = size / 2
            val startAngle = -90f

            drawArc(
                color = Color.LightGray,
                startAngle = startAngle,
                sweepAngle = 360f,
                useCenter = false,
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = 360f * progress,
                useCenter = false,
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun DiveScreen() {
    // Configurable variables
    var depthState = remember { mutableStateOf(0.0) }
    var depth by depthState
    var timeState = remember { mutableStateOf(0) }
    var time by timeState
    var fO2State = remember { mutableStateOf(0.21) }
    var fO2 by fO2State
    var ppO2 by remember { mutableStateOf(1.6) }
    var pFactor by remember { mutableStateOf(1) } // Safety factor: 0 (None) - 2 (Highest)

    // Non-configurable variables
    var n2State = remember { mutableStateOf(0.5) }
    var n2 by n2State
    var si by remember { mutableStateOf("00:00") }

    // Statuses
    var noflyState = remember { mutableStateOf(0) }
    var noFly by noflyState
    var noDecoState = remember { mutableStateOf(0) }
    var noDeco by noDecoState
    var decoState = remember { mutableStateOf(0) }
    var deco by decoState

    // Text states
    var ppO2_colour by remember { mutableStateOf(Color.White.copy(alpha = 0.3f)) }
    var mod_colour by remember { mutableStateOf(Color.White) }
    var ppO2_mod_text by remember { mutableStateOf(String.format("%.1fm", (ppO2 / fO2 - 1) * 10)) } // Default MOD

    // Inputs
    var currentField by remember { mutableStateOf<String?>(null) }
    var currentValue by remember { mutableStateOf("") }

    // Algorithm
    val buhlmann = Buhlmann(
        depthState,
        timeState,
        fO2State,
        pFactor,

        n2State,
        noflyState,
        noDecoState,
        decoState,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(230.dp)
    ) {
        NitrogenLoadingBar(
            progress = n2.toFloat(),
            strokeWidth = 15f,
            color = Color.Cyan,
            modifier = Modifier.fillMaxSize()
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Top section
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top row text
                        Column {
                            Row(modifier = Modifier.clickable {
                                pFactor = if (pFactor == 2) 0 else pFactor + 1
                            }) {
                                Text(
                                    "depth",
                                    modifier = Modifier
                                        .scale(0.6f)
                                        .padding(start = 60.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onLongPress = {
                                                currentField = "depth"
                                                currentValue = depth.toString()
                                            })
                                        }
                                )

                                Spacer(modifier = Modifier.width(15.dp))

                                // Safety Factor
                                Box {
                                    Text(
                                        "P",
                                        color = if (pFactor >= 1) Color.White else Color.White.copy(
                                            alpha = 0.3f
                                        ),
                                        modifier = Modifier.scale(0.9f)
                                    )

                                    Text(
                                        "+",
                                        color = if (pFactor >= 1) Color.White else Color.White.copy(
                                            alpha = 0.3f
                                        ),
                                        modifier = Modifier
                                            .scale(0.9f)
                                            .padding(start = 9.dp)
                                    )

                                    Text(
                                        "+",
                                        color = if (pFactor == 2) Color.White else Color.White.copy(
                                            alpha = 0.3f
                                        ),
                                        modifier = Modifier
                                            .scale(0.9f)
                                            .padding(start = 18.dp)
                                    )
                                }
                            }

                            Text(
                                "${depth}m",
                                modifier = Modifier
                                    .scale(1.5f)
                                    .padding(start = 40.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(onLongPress = {
                                            currentField = "depth"
                                            currentValue = depth.toString()
                                        })
                                    }
                            )
                        }

                        // No fly time
                        Column(horizontalAlignment = Alignment.End) {
                            val noflyStr by remember { derivedStateOf { noFly.toString() } }

                            Text(
                                "no fly",
                                color = if (noFly == 0) Color.White.copy(alpha = 0.3f) else Color.White,
                                modifier = Modifier
                                    .scale(0.6f)
                                    .padding(end = 49.dp)
                            )

                            Row {
                                if (noFly >= 10) {
                                    Text(
                                        "${noflyStr[0]}",
                                        modifier = Modifier
                                            .scale(1.5f)
                                            .padding(end = 10.dp)
                                    )
                                } else {
                                    Text(
                                        "0",
                                        color = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier
                                            .scale(1.5f)
                                            .padding(end = 10.dp)

                                    )
                                }

                                Text(
                                    "${noflyStr[if (noFly >= 10) 1 else 0]}h",
                                    color = if (noFly == 0) Color.White.copy(alpha = 0.3f) else Color.White,
                                    modifier = Modifier
                                        .scale(1.5f)
                                        .padding(end = 30.dp)
                                )
                            }
                        }
                    }
                }

                // Middle section
                Box(modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            currentField = "time"
                            currentValue = time.toString()
                        })
                    }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Bottom time
                        Box(modifier = Modifier.padding(start = 18.dp)) {
                            Column {
                                Text(
                                    "time",
                                    modifier = Modifier
                                        .scale(0.6f)
                                        .padding(start = 1.dp)
                                )

                                Row {
                                    val timeStr by remember { derivedStateOf { time.toString() } }

                                    if (timeStr.length == 2) {
                                        Text(
                                            "${timeStr[0]}",
                                            modifier = Modifier
                                                .scale(1.5f)
                                                .padding(end = 9.dp)
                                        )
                                    } else {
                                        Text(
                                            "0",
                                            color = Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .scale(1.5f)
                                                .padding(end = 11.dp)
                                        )
                                    }

                                    Text(
                                        "${timeStr[if (timeStr.length == 2) 1 else 0]}m",
                                        modifier = Modifier
                                            .scale(1.5f)
                                            .padding(end = 30.dp)
                                    )
                                }
                            }
                        }

                        // Deco time
                        Box(modifier = Modifier.offset(3.dp)) {
                            Column {
                                Row {
                                    Text(
                                        "no",
                                        color =  if (deco == 0) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier
                                            .scale(0.6f)
                                            .offset((-13).dp)
                                    )

                                    Text(
                                        "deco",
                                        modifier = Modifier
                                            .scale(0.6f)
                                            .offset((-22).dp),
                                    )
                                }

                                Row {
                                    val decoStr by remember { derivedStateOf {
                                        if (deco == 0) noDeco.toString() else deco.toString()
                                    } }

                                    if (decoStr.length == 2) {
                                        Text(
                                            "${decoStr[0]}",
                                            modifier = Modifier
                                                .scale(1.5f)
                                                .padding(end = 9.dp)
                                        )
                                    } else {
                                        Text(
                                            "0",
                                            color = Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .scale(1.5f)
                                                .padding(end = 9.dp)
                                        )
                                    }

                                    Text(
                                        "${decoStr[if (decoStr.length == 2) 1 else 0]}",
                                        modifier = Modifier
                                            .scale(1.5f)
                                            .padding(end = 30.dp)
                                    )
                                }
                            }
                        }

                        // PPO2 / MOD
                        Box(modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (mod_colour == Color.White) {
                                        // Show PPO2
                                        mod_colour = Color.White.copy(alpha = 0.3f)
                                        ppO2_colour = Color.White
                                        ppO2_mod_text = ppO2.toString()
                                    } else {
                                        // Show MOD
                                        mod_colour = Color.White
                                        ppO2_colour = Color.White.copy(alpha = 0.3f)
                                        ppO2_mod_text = String.format("%.1fm", (ppO2 / fO2 - 1) * 10)
                                    }
                                },
                                onLongPress = {
                                    // Only let you change ppO2 when it's visible
                                    if (ppO2_colour == Color.White) {
                                        currentField = "ppO2"
                                        currentValue = ppO2.toString()
                                    }
                            })
                        }) {
                            Column {
                                Row {
                                    Text(
                                        "ppO2",
                                        color = ppO2_colour,
                                        modifier = Modifier
                                            .scale(0.6f)
                                            .offset((-10).dp)
                                    )

                                    Text(
                                        "mod",
                                        color = mod_colour,
                                        modifier = Modifier
                                            .scale(0.6f)
                                            .offset((-20).dp)
                                    )
                                }

                                Text(
                                    ppO2_mod_text,
                                    modifier = Modifier.scale(1.2f)
                                )
                            }
                        }
                    }
                }

                // Bottom section
                Box(modifier = Modifier
                    .weight(1f)
                    .offset(y = (-30).dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.padding(start = 30.dp)) {
                            Text(
                                "s.i.",
                                modifier = Modifier
                                    .scale(0.6f)
                                    .padding(start = 20.dp)
                            )

                            Text(
                                "00:00m",
                                modifier = Modifier.scale(1.2f)
                            )
                        }

                        Column(modifier = Modifier
                            .padding(end = 10.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = {
                                    currentField = "fO2"
                                    currentValue = (fO2 * 100)
                                        .toInt()
                                        .toString()
                                })
                            }) {
                            Text(
                                "fO2%",
                                modifier = Modifier
                                    .scale(0.6f)
                                    .offset(0.dp)
                            )

                            Text(
                                "${(fO2 * 100).toInt()}",
                                modifier = Modifier.scale(1.2f)
                            )
                        }
                    }
                }
            }

            // Horizontal lines overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = size.minDimension
                val radius = size / 2
                val centerX = size / 2
                val centerY = size / 2

                // Calculate row positions for even spacing
                val topY = centerY - radius * 0.4f    // Adjusted multiplier
                val middleY = centerY
                val bottomY = centerY + radius * 0.4f // Adjusted multiplier

                // Calculate line length based on circle width
                val lineLength = size * 0.8f  // Using 80% of the circle width

                // Calculate start and end X coordinates
                val startX = centerX - lineLength / 2
                val endX = centerX + lineLength / 2

                // Draw the three lines
                drawLine(
                    start = Offset(x = startX, y = topY),
                    end = Offset(x = endX, y = topY),
                    color = Color.White,
                    strokeWidth = 2f
                )

                drawLine(
                    start = Offset(x = startX - 20, y = middleY),
                    end = Offset(x = endX + 20, y = middleY),
                    color = Color.White,
                    strokeWidth = 2f
                )

                drawLine(
                    start = Offset(x = startX, y = bottomY),
                    end = Offset(x = endX, y = bottomY),
                    color = Color.White,
                    strokeWidth = 2f
                )
            }
        }
    }

    currentField?.let {
        InputDialog(
            currentValue = currentValue
        ) { newInput ->
            when (it) {
                "depth" -> {
                    depth = newInput.toDoubleOrNull() ?: 0.0
                }

                "time" -> {
                    time = newInput.toIntOrNull() ?: 0
                    buhlmann.loadToBottom() // TODO: Try to optimize re-calculating constantly?
                }

                "ppO2" -> {
                    ppO2 = newInput.toDoubleOrNull() ?: 1.6
                    ppO2_mod_text = ppO2.toString()
                }

                "fO2" -> {
                    fO2 = (newInput.toDoubleOrNull() ?: 0.21) / 100
                    buhlmann.loadToBottom() // TODO: Try to optimize re-calculating constantly?
                }
            }

            currentField = null
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DiveScreenPreview() {
    DiveScreen()
}