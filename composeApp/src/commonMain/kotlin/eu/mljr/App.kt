package eu.mljr

import ColorPicker
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ExperimentalGraphicsApi
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

import kotlin.math.max
import kotlin.math.min

@Composable
@Preview
fun App() {
    var gameState by remember { mutableStateOf(GameState.PAUSED) }
    var zoom by remember { mutableStateOf(0.5f) }
    val game by remember { mutableStateOf(Game(500, 500)) }
    var offset by remember { mutableStateOf(Pair(game.width / 2, game.height / 2)) }
    var faster by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Cyan) }
    var selectedIndex by remember { mutableStateOf(0) } // rules
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text(text = "Controls: ", modifier = Modifier.align(Alignment.CenterVertically))
                        if (gameState == GameState.PAUSED) {
                            Button(
                                onClick = {
                                    gameState = GameState.RUNNING
                                    game.startGame()
                                },
                                colors = (ButtonDefaults.buttonColors(Color(35, 222, 91))),
                                modifier = Modifier.padding(all = 3.dp)
                            )
                            {
                                Text("Start")
                            }
                        } else {
                            Button(
                                onClick = {
                                    gameState = GameState.PAUSED
                                    game.stopGame()
                                },
                                colors = (ButtonDefaults.buttonColors(Color(189, 4, 65))),
                                modifier = Modifier.padding(all = 3.dp)
                            )
                            {
                                Text("Stop")
                            }
                        }
                        var speed by remember { mutableStateOf(300f) }
                        Button(
                            onClick = {
                                gameState = GameState.PAUSED
                                game.resetGame()
                            },
                            colors = (ButtonDefaults.buttonColors(Color.Black)),
                            modifier = Modifier.padding(all = 3.dp)
                        )
                        {
                            Text("Reset")
                        }
                        Button(
                            onClick = {
                                game.stopGame()
                                gameState = GameState.PAUSED
                                settings = true
                            },
                            colors = (ButtonDefaults.buttonColors(Color.White)),
                            modifier = Modifier.padding(all = 3.dp)
                        )
                        {
                            Text("Settings")
                        }
                        Column {
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                // add a trailing zero to the rounded number to keep it at consistent 2 decimals
                                val speedString = speed.toInt().toString()
                                Text("Speed (ms): ")
                                Text(text = speedString)
                            }
                            Slider(
                                value = speed,
                                onValueChange = {
                                    speed = it
                                },
                                colors = SliderDefaults.colors(
                                    activeTickColor = Color.Gray,
                                    inactiveTickColor = Color.Gray,
                                    inactiveTrackColor = Color.White,
                                    activeTrackColor = Color.White,
                                    thumbColor = Color.White,
                                ),
                                modifier = Modifier.padding(start = 5.dp, end = 5.dp),
                                valueRange = 10f..1000f,
                                onValueChangeFinished = {
                                    game.changeSpeed(speed, gameState)
                                }
                            )
                        }
                    }
                },
            )
        },
        content = {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val scope = this
                val minCellsSize = 5
                val maxZoomFactor = 50
                var cellSize =
                    Size(minCellsSize + (zoom * maxZoomFactor), minCellsSize + (zoom * maxZoomFactor))
                val nrCellsWidth = (scope.maxWidth.value / cellSize.width).toInt()
                val nrCellsHeight = (scope.maxHeight.value / (cellSize.height)).toInt()
                Canvas(
                    Modifier.align(Alignment.Center).fillMaxSize()
                        .scrollable(orientation = Orientation.Vertical, state = rememberScrollableState { delta ->
                            offset = Pair(
                                offset.first + 0,
                                max(0, min(game.height, offset.second - delta.toInt() / 10))
                            ) //only Vertical Scroll since adding horizontal scroll seems to trigger vertical as well -> diagonal Scroll ?
                            delta
                        }).pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { tap ->
                                    // Recalculate Cell Size when necessary because on tap does not react to slider change value
                                    if (!settings) { // do not allow changing cells when in settings menu
                                        cellSize =
                                            Size(
                                                minCellsSize + (zoom * maxZoomFactor),
                                                minCellsSize + (zoom * maxZoomFactor)
                                            )
                                        val tapOffset =
                                            Pair((tap.x / cellSize.width).toInt(), (tap.y / cellSize.height).toInt())
                                        game.set(offset + tapOffset, !(game get (offset + tapOffset)).state)
                                        //println("CLicked at: $tap. -> / ${cellSize.width}; ${cellSize.height} -> Cell ${offset + tapOffset}")
                                        //Output for designing pre-made Cell structures
                                        //println("set(offset + Pair(${(offset + tapOffset).first},${(offset + tapOffset).second}), CellState.ALIVE)")
                                    }
                                }
                            )

                        }) {
                    inset {
                        //drawRect(Color.LightGray, Offset(0f, 0f), Size(scope.maxWidth.value, scope.maxHeight.value))
                        for (w in 0 until nrCellsWidth) {
                            for (h in 0 until nrCellsHeight) {
                                if ((Pair(w, h) + offset).first >= game.width || (Pair(
                                        w,
                                        h
                                    ) + offset).second >= game.height
                                ) break
                                drawRect(
                                    size = cellSize,
                                    color = if ((game get (Pair(
                                            w,
                                            h
                                        ) + offset)).state == CellState.ALIVE
                                    ) selectedColor else selectedColor.complement(),
                                    topLeft = Offset(cellSize.width * w, cellSize.height * h)
                                )
                                drawRect(
                                    size = Size(cellSize.width + 1, cellSize.height + 1),
                                    color = Color.Gray,
                                    topLeft = Offset(cellSize.width * w, cellSize.height * h),
                                    style = Stroke(2f)
                                )
                                // For Debugging print index of every cell
                                /*drawContext.canvas.nativeCanvas.apply {
                                    drawString(
                                        "${w + offset.first},${h + offset.second}",
                                        Offset(cellSize.width * w, cellSize.height * h).x,
                                        Offset(cellSize.width * w, cellSize.height * h + cellSize.height / 2).y,
                                        Font(Typeface.makeDefault()),
                                        Paint()
                                    )
                                }*/
                            }
                        }
                    }
                }
                // Zoom
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).width(((5 * scope.maxWidth.value) / 8).dp)
                        .background(Color(0.423529f, 0.623529f, 0.639215f, 0.85f)).padding(10.dp).border(
                            BorderStroke(1.dp, Color.Black)
                        )
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(start = 5.dp, top = 5.dp)) {
                        Text("Zoom: ")
                        // add a trailing zero to the rounded number to keep it at consistent 2 decimals
                        val speedString = ((zoom * 100).toInt() / 100f).toString()
                        Text(text = if (speedString.length == 3) speedString + "0" else speedString)
                    }
                    Slider(
                        value = zoom,
                        onValueChange = {
                            zoom = it
                        },
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                    )
                }
                // Movement of Grid + random placement + Structures
                // TODO: Change buttons when choosing different rules to place different structures?
                Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp)) {
                    val steps = if (faster) 10 else 1
                    Button(
                        onClick = {
                            game.addGlider(gameState = gameState)
                        },
                        colors = (ButtonDefaults.buttonColors(Color.Cyan)),
                        modifier = Modifier.padding(all = 0.dp).align(Alignment.End),
                        border = BorderStroke(1.dp, Color.Black)
                    )
                    {
                        Text("Glider")
                    }
                    Button(
                        onClick = {
                            game.addGliderGun(gameState = gameState)
                        },
                        colors = (ButtonDefaults.buttonColors(Color.Cyan)),
                        modifier = Modifier.padding(all = 0.dp).align(Alignment.End),
                        border = BorderStroke(1.dp, Color.Black)
                    )
                    {
                        Text("Glider Gun")
                    }
                    Button(
                        onClick = {
                            game.addPentomino(gameState = gameState)
                        },
                        colors = (ButtonDefaults.buttonColors(Color.Cyan)),
                        modifier = Modifier.padding(all = 0.dp).align(Alignment.End),
                        border = BorderStroke(1.dp, Color.Black)
                    )
                    {
                        Text("f-Pentomino")
                    }
                    Button(
                        onClick = {
                            game.random(gameState = gameState, offset)
                        },
                        colors = (ButtonDefaults.buttonColors(Color.Cyan)),
                        modifier = Modifier.padding(all = 0.dp).align(Alignment.End),
                        border = BorderStroke(1.dp, Color.Black)
                    )
                    {
                        Text("Random")
                    }
                    BoxWithConstraints(
                        modifier = Modifier.background(Color(0.423529f, 0.623529f, 0.639215f, 0.75f)).align(
                            Alignment.End
                        )
                    ) {
                        Column(verticalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "10 Steps:",
                                modifier = Modifier.padding(end = 5.dp)
                            )
                            Checkbox(
                                checked = faster,
                                onCheckedChange = {
                                    faster = it
                                }
                            )
                            Text(
                                "X,Y: ${offset.first}, ${offset.second}",
                                modifier = Modifier.padding(end = 5.dp)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            offset = Pair(offset.first, max(0, offset.second - steps))
                        },
                        colors = (ButtonDefaults.buttonColors(Color.Yellow)),
                        modifier = Modifier.padding(all = 0.dp).align(Alignment.CenterHorizontally),
                        border = BorderStroke(1.dp, Color.Black)
                    )
                    {
                        Text("Up")
                    }
                    Row {
                        Button(
                            onClick = {
                                offset = Pair(max(0, offset.first - steps), offset.second)
                            },
                            colors = (ButtonDefaults.buttonColors(Color.Yellow)),
                            modifier = Modifier.padding(all = 0.dp),
                            border = BorderStroke(1.dp, Color.Black)
                        )
                        {
                            Text("Left")
                        }
                        Button(
                            onClick = {
                                offset = Pair(offset.first, min(game.height, offset.second + steps))
                            },
                            colors = (ButtonDefaults.buttonColors(Color.Yellow)),
                            modifier = Modifier.padding(all = 0.dp),
                            border = BorderStroke(1.dp, Color.Black)
                        )
                        {
                            Text("Down")
                        }
                        Button(
                            onClick = {
                                offset = Pair(min(game.width, offset.first + steps), offset.second)
                            },
                            colors = (ButtonDefaults.buttonColors(Color.Yellow)),
                            modifier = Modifier.padding(all = 0.dp),
                            border = BorderStroke(1.dp, Color.Black)
                        )
                        {
                            Text("Right")
                        }
                    }
                }
                // Settings Menu
                if (settings) {
                    BoxWithConstraints(
                        modifier = Modifier.size((scope.maxWidth.value * 0.75).dp, (scope.maxHeight.value * 0.75).dp)
                            .align(
                                Alignment.Center
                            ).background(Color(0.023529f, 0.223529f, 0.439215f, 0.75f))
                    ) {

                        Row(modifier = Modifier.align(Alignment.TopStart)) {
                            //Color Picker
                            Column {
                                ColorPicker(
                                    modifier = Modifier.size(300.dp),
                                    initialColor = selectedColor,
                                    showAlphaBar = false
                                ) { color ->
                                    selectedColor = color
                                }
                                Text(
                                    "Color: ${(selectedColor.red * 255).toInt()}, ${(selectedColor.green * 255).toInt()}, ${(selectedColor.blue * 255).toInt()}",
                                    modifier = Modifier.padding(start = 15.dp),
                                    color = Color.White
                                )
                            }
                            //Rules
                            Column {
                                var expanded by remember { mutableStateOf(false) }
                                val rules = listOf("Conway", "3/3 World", "13/3 World", "34/3 World") // https://de.wikipedia.org/wiki/Conways_Spiel_des_Lebens#:~:text=5%5D%5B6%5D-,Abweichende%20Regeln,-%5BBearbeiten%20%7C
                                Box(
                                    modifier = Modifier.size((scope.maxWidth.value * 0.25).dp)
                                        .wrapContentSize(Alignment.TopStart).padding(15.dp)
                                ) {
                                    Text(
                                        rules[selectedIndex],
                                        modifier = Modifier.fillMaxWidth().clickable(onClick = { expanded = true })
                                            .background(
                                                Color.Gray
                                            )
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.size((scope.maxWidth.value * 0.25).dp).background(
                                            Color.Gray
                                        )
                                    ) {
                                        rules.forEachIndexed { index, s ->
                                            DropdownMenuItem(onClick = {
                                                selectedIndex = index
                                                expanded = false
                                                game.rule = index
                                            }) {
                                                Text(s)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    //val fileChooser = JFileChooser()
                                    //fileChooser.showOpenDialog(null)
                                    //val file = fileChooser.selectedFile
                                    //if (file != null) {
                                    //    File(file.absoluteFile.toString()).writeText(game.toString())
                                    //}
                                    // Use FileKit for multiplatform file handling
                                    CoroutineScope(Dispatchers.Main).launch {
                                        FileKit.saveFile(
                                            baseName = "game",
                                            extension = "txt",
                                            bytes = game.toString().encodeToByteArray()
                                        )
                                    }
                                },
                                colors = (ButtonDefaults.buttonColors(Color.White)),
                                modifier = Modifier.padding(all = 15.dp)
                            )
                            {
                                Text("Save to File")
                            }
                            Button(
                                onClick = {
                                    //val fileChooser = JFileChooser()
                                    //fileChooser.showOpenDialog(null)
                                    //val file = fileChooser.selectedFile
                                    //if (file != null) {
                                    //    try {
                                    //        val lines = File(file.absoluteFile.toString()).readLines()
                                    //        game.initialize(lines.map {
                                    //            Pair(
                                    //                it.split(",").first().toInt(),
                                    //                it.split(",").last().toInt()
                                    //            )
                                    //        })
                                    //    } catch (e: Exception) {
                                    //        println("something went wrong while processing file: ${file.absoluteFile}")
                                    //    }
                                    //}
                                    // Use FileKit for multiplatform file handling
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val file = FileKit.pickFile(type = PickerType.File(extensions = listOf("txt")),
                                            mode = PickerMode.Single, title = "Pick a saved Game file")
                                        if (file != null) {
                                            try {
                                                val lines = file.readBytes().decodeToString().split("\n")
                                                for (line in lines) {
                                                    println(line)
                                                }
                                                game.initialize(lines.map {
                                                    Pair(
                                                        it.split(",").first().toInt(),
                                                        it.split(",").last().toInt()
                                                    )
                                                })
                                            } catch (e: Exception) {
                                                println("something went wrong while processing file: ${file.path}")
                                            }
                                        }
                                    }
                                },
                                colors = (ButtonDefaults.buttonColors(Color.White)),
                                modifier = Modifier.padding(all = 15.dp)
                            )
                            {
                                Text("Load from File")
                            }
                            Button(
                                onClick = {
                                    settings = false
                                },
                                colors = (ButtonDefaults.buttonColors(Color.White)),
                                modifier = Modifier.padding(all = 15.dp)
                            )
                            {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    )
}

// Code to convert rgb to hsv from: https://www.geeksforgeeks.org/program-change-rgb-color-model-hsv-color-model/
@OptIn(ExperimentalGraphicsApi::class) // to be able to use the hsv initializer of Color
fun Color.complement(): Color {
    val max = red.coerceAtLeast(green.coerceAtLeast(blue)) // maximum of r, g, b
    val min = red.coerceAtMost(green.coerceAtMost(blue)) // minimum of r, g, b
    val diff = max - min // diff of max and min.
    var h = -1f

    when (max) {
        min -> h = 0f
        red -> h = (60 * ((green - blue) / diff) + 360) % 360
        green -> h = (60 * ((blue - red) / diff) + 120) % 360
        blue -> h = (60 * ((red - green) / diff) + 240) % 360
    }

    val s = if (max == 0f) 0f else (diff / max)

    // code for contrast color calculation from: https://gamedev.stackexchange.com/questions/38536/given-a-rgb-color-x-how-to-find-the-most-contrasting-color-y
    return Color.hsv(
        (h + 180f) % 360,
        s,
        1f - max
    )
}