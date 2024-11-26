// ColorPicker.kt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun ColorPicker(
    modifier: Modifier = Modifier,
    initialColor: Color = Color.Red,
    showAlphaBar: Boolean = false,
    onColorChanged: (Color) -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }
    var alpha by remember { mutableStateOf(1f) }

    LaunchedEffect(initialColor) {
        // Convert initial color to HSV
        val hsv = initialColor.let { color ->
            val max = maxOf(color.red, color.green, color.blue)
            val min = minOf(color.red, color.green, color.blue)
            val delta = max - min

            val h = when (max) {
                min -> 0f
                color.red -> (60 * ((color.green - color.blue) / delta) + 360) % 360
                color.green -> 60 * ((color.blue - color.red) / delta) + 120
                else -> 60 * ((color.red - color.green) / delta) + 240
            }

            val s = if (max == 0f) 0f else delta / max
            val v = max

            Triple(h, s, v)
        }

        hue = hsv.first
        saturation = hsv.second
        value = hsv.third
        alpha = initialColor.alpha
    }

    Column(modifier = modifier) {
        // Main color panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ColorPanel(
                hue = hue,
                saturation = saturation,
                value = value,
                onSaturationValueChanged = { s, v ->
                    saturation = s
                    value = v
                    onColorChanged(Color.hsv(hue, saturation, value, alpha))
                }
            )
        }

        // Hue bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(vertical = 8.dp)
        ) {
            HueBar(
                hue = hue,
                onHueChanged = { h ->
                    hue = h
                    onColorChanged(Color.hsv(hue, saturation, value, alpha))
                }
            )
        }

        // Alpha bar (optional)
        if (showAlphaBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(vertical = 8.dp)
            ) {
                AlphaBar(
                    color = Color.hsv(hue, saturation, value, 1f),
                    alpha = alpha,
                    onAlphaChanged = { a ->
                        alpha = a
                        onColorChanged(Color.hsv(hue, saturation, value, alpha))
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorPanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChanged: (Float, Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val position = down.position
                        val size = Size(size.width.toFloat(), size.height.toFloat())
                        val newSaturation = (position.x / size.width).coerceIn(0f, 1f)
                        val newValue = (1f - position.y / size.height).coerceIn(0f, 1f)
                        onSaturationValueChanged(newSaturation, newValue)

                        drag(down.id) { change ->
                            val dragPosition = change.position
                            val s = (dragPosition.x / size.width).coerceIn(0f, 1f)
                            val v = (1f - dragPosition.y / size.height).coerceIn(0f, 1f)
                            onSaturationValueChanged(s, v)
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // Draw saturation gradient (white to pure color)
        val saturationGradient = LinearGradientShader(
            from = Offset.Zero,
            to = Offset(width, 0f),
            colors = listOf(Color.White, Color.hsv(hue, 1f, 1f))
        )

        // Draw value gradient (transparent to black)
        val valueGradient = LinearGradientShader(
            from = Offset(0f, 0f),
            to = Offset(0f, height),
            colors = listOf(Color.Transparent, Color.Black)
        )

        drawRect(brush = ShaderBrush(saturationGradient))
        drawRect(brush = ShaderBrush(valueGradient))

        // Draw selection circle
        val circleX = saturation * width
        val circleY = (1f - value) * height
        val circleRadius = 8.dp.toPx()

        // Draw outer white circle
        drawCircle(
            color = Color.White,
            radius = circleRadius,
            center = Offset(circleX, circleY),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw inner black circle
        drawCircle(
            color = Color.Black,
            radius = circleRadius - 1.dp.toPx(),
            center = Offset(circleX, circleY),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onHueChanged: (Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val position = down.position
                        val newHue = (position.x / size.width * 360f).coerceIn(0f, 360f)
                        onHueChanged(newHue)

                        drag(down.id) { change ->
                            val dragPosition = change.position
                            val updatedHue = (dragPosition.x / size.width * 360f).coerceIn(0f, 360f)
                            onHueChanged(updatedHue)
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // Create hue gradient
        val hueGradient = LinearGradientShader(
            from = Offset.Zero,
            to = Offset(width, 0f),
            colors = listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red
            )
        )

        // Draw background
        drawRect(brush = ShaderBrush(hueGradient))

        // Draw slider
        val sliderX = (hue / 360f) * width
        val sliderWidth = 4.dp.toPx()
        val sliderHeight = height

        // Draw white outer rectangle
        drawRect(
            color = Color.White,
            topLeft = Offset(sliderX - sliderWidth / 2 - 1.dp.toPx(), 0f),
            size = Size(sliderWidth + 2.dp.toPx(), sliderHeight),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw black inner rectangle
        drawRect(
            color = Color.Black,
            topLeft = Offset(sliderX - sliderWidth / 2, 0f),
            size = Size(sliderWidth, sliderHeight),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun AlphaBar(
    color: Color,
    alpha: Float,
    onAlphaChanged: (Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val position = down.position
                        val newAlpha = (position.x / size.width).coerceIn(0f, 1f)
                        onAlphaChanged(newAlpha)

                        drag(down.id) { change ->
                            val dragPosition = change.position
                            val updatedAlpha = (dragPosition.x / size.width).coerceIn(0f, 1f)
                            onAlphaChanged(updatedAlpha)
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // Create checkerboard pattern for transparency
        val checkerboardSize = 8.dp.toPx()
        for (x in 0..((width / checkerboardSize).toInt())) {
            for (y in 0..((height / checkerboardSize).toInt())) {
                val isGray = (x + y) % 2 == 0
                drawRect(
                    color = if (isGray) Color.LightGray else Color.White,
                    topLeft = Offset(x * checkerboardSize, y * checkerboardSize),
                    size = Size(checkerboardSize, checkerboardSize)
                )
            }
        }

        // Draw alpha gradient
        val alphaGradient = LinearGradientShader(
            from = Offset.Zero,
            to = Offset(width, 0f),
            colors = listOf(
                color.copy(alpha = 0f),
                color.copy(alpha = 1f)
            )
        )

        drawRect(brush = ShaderBrush(alphaGradient))

        // Draw slider
        val sliderX = alpha * width
        val sliderWidth = 4.dp.toPx()
        val sliderHeight = height

        // Draw white outer rectangle
        drawRect(
            color = Color.White,
            topLeft = Offset(sliderX - sliderWidth / 2 - 1.dp.toPx(), 0f),
            size = Size(sliderWidth + 2.dp.toPx(), sliderHeight),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw black inner rectangle
        drawRect(
            color = Color.Black,
            topLeft = Offset(sliderX - sliderWidth / 2, 0f),
            size = Size(sliderWidth, sliderHeight),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}