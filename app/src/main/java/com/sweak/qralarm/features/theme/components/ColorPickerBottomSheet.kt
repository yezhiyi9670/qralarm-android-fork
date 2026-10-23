package com.sweak.qralarm.features.theme.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.sweak.qralarm.R
import com.sweak.qralarm.core.designsystem.theme.space
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A hue wheel with tint and shade sliders, mirroring the custom color picker of the original
 * app: the wheel selects a fully saturated hue and the sliders move it towards white and
 * towards black. Both sliders show the base hue on the left, so the position of the thumb is
 * what the resulting color looks like.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorPicked: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var baseHueColor by remember(initialColor) {
        mutableStateOf(Color(AndroidColor.HSVToColor(floatArrayOf(
            initialColor.toHsvHue(),
            1f,
            1f
        ))))
    }
    var tint by remember { mutableFloatStateOf(0f) }
    var shade by remember { mutableFloatStateOf(0f) }

    val resultingColor = remember(baseHueColor, tint, shade) {
        composeColor(baseHueColor, tint, shade)
    }
    val hue = remember(baseHueColor) { baseHueColor.toHsvHue() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = MaterialTheme.space.medium,
                    end = MaterialTheme.space.medium,
                    bottom = MaterialTheme.space.mediumLarge
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.custom_color),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(MaterialTheme.space.mediumLarge))

            HueWheel(
                hue = hue,
                thumbColor = resultingColor,
                onHueChanged = { newHue ->
                    baseHueColor = Color(
                        AndroidColor.HSVToColor(floatArrayOf(newHue, 1f, 1f))
                    )
                }
            )

            Spacer(modifier = Modifier.height(MaterialTheme.space.mediumLarge))

            GradientSlider(
                colors = listOf(baseHueColor, Color.White),
                value = tint,
                onValueChange = { tint = it },
                description = stringResource(R.string.custom_color)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.space.medium))

            GradientSlider(
                colors = listOf(baseHueColor, Color.Black),
                value = shade,
                onValueChange = { shade = it },
                description = stringResource(R.string.custom_color)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.space.mediumLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    MaterialTheme.space.medium,
                    Alignment.End
                )
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        onColorPicked(
                            String.format(
                                Locale.US,
                                "#%08X",
                                resultingColor.toArgb()
                            )
                        )
                    }
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun HueWheel(
    hue: Float,
    thumbColor: Color,
    onHueChanged: (Float) -> Unit
) {
    val hueColors = remember {
        (0..360 step 10).map { degrees ->
            Color(AndroidColor.HSVToColor(floatArrayOf(degrees.toFloat(), 1f, 1f)))
        }
    }

    Box(
        modifier = Modifier
            .size(240.dp)
            .semantics { contentDescription = "Hue" }
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onHueChanged(offset.toHue(size.width.toFloat(), size.height.toFloat()))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onHueChanged(offset.toHue(size.width.toFloat(), size.height.toFloat()))
                        },
                        onDrag = { change, _ ->
                            onHueChanged(
                                change.position.toHue(
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )
                            )
                        }
                    )
                }
        ) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.sweepGradient(hueColors, center),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = radius * 0.58f,
                center = center
            )

            val angleRadians = Math.toRadians(hue.toDouble())
            val thumbCenter = Offset(
                x = center.x + (cos(angleRadians) * radius * 0.79f).toFloat(),
                y = center.y + (sin(angleRadians) * radius * 0.79f).toFloat()
            )
            drawCircle(color = thumbColor, radius = 18.dp.toPx(), center = thumbCenter)
            drawCircle(
                color = Color.White,
                radius = 18.dp.toPx(),
                center = thumbCenter,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = 18.dp.toPx(),
                center = thumbCenter,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun GradientSlider(
    colors: List<Color>,
    value: Float,
    onValueChange: (Float) -> Unit,
    description: String
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(colors),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )

        val thumbX = (value.coerceIn(0f, 1f) * size.width)
        drawLine(
            color = Color.White,
            start = Offset(thumbX, size.height * 0.1f),
            end = Offset(thumbX, size.height * 0.9f),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(thumbX, size.height * 0.1f),
            end = Offset(thumbX, size.height * 0.9f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun Offset.toHue(width: Float, height: Float): Float {
    val centerX = width / 2f
    val centerY = height / 2f
    val degrees = Math.toDegrees(
        atan2(y - centerY, x - centerX).toDouble()
    ).toFloat()
    return (degrees + 360f) % 360f
}

private fun Color.toHsvHue(): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    return hsv[0]
}

private fun composeColor(
    baseColor: Color,
    tint: Float,
    shade: Float
): Color {
    fun channel(value: Float): Float {
        val tinted = (value * 255f) + ((255f - (value * 255f)) * tint.coerceIn(0f, 1f))
        return (tinted * (1f - shade.coerceIn(0f, 1f))) / 255f
    }

    return Color(
        red = channel(baseColor.red).coerceIn(0f, 1f),
        green = channel(baseColor.green).coerceIn(0f, 1f),
        blue = channel(baseColor.blue).coerceIn(0f, 1f),
        alpha = 1f
    )
}
