// app/MainActivity.kt
package com.gardengraph.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gardengraph.app.ui.theme.GardenGraphTheme
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GardenGraphTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GardenConfigurator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenConfigurator() {
    var numberOfGardens by remember { mutableStateOf("4") }
    val connections = remember { mutableStateListOf<Pair<Int, Int>>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showGraph by remember { mutableStateOf(false) }
    var selectedFrom by remember { mutableStateOf(0) }
    var selectedTo by remember { mutableStateOf(1) }

    var fromMenuExpanded by remember { mutableStateOf(false) }
    var toMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Garden Configuration", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = numberOfGardens,
            onValueChange = {
                numberOfGardens = it.filter { c -> c.isDigit() }
                connections.clear()
                showGraph = false
            },
            label = { Text("Number of gardens") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (numberOfGardens.toIntOrNull() != null && numberOfGardens.toInt() > 1) {
            val gardenOptions = (1..numberOfGardens.toInt()).toList()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = fromMenuExpanded,
                    onExpandedChange = { fromMenuExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = (selectedFrom + 1).toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromMenuExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = fromMenuExpanded,
                        onDismissRequest = { fromMenuExpanded = false }
                    ) {
                        gardenOptions.forEach { index ->
                            DropdownMenuItem(
                                text = { Text(index.toString()) },
                                onClick = {
                                    selectedFrom = index - 1
                                    fromMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = toMenuExpanded,
                    onExpandedChange = { toMenuExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = (selectedTo + 1).toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = toMenuExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = toMenuExpanded,
                        onDismissRequest = { toMenuExpanded = false }
                    ) {
                        gardenOptions.forEach { index ->
                            DropdownMenuItem(
                                text = { Text(index.toString()) },
                                onClick = {
                                    selectedTo = index - 1
                                    toMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Button(onClick = {
                if (selectedFrom != selectedTo) {
                    val sortedConnection = if (selectedFrom < selectedTo) Pair(selectedFrom, selectedTo) else Pair(selectedTo, selectedFrom)
                    if (!connections.contains(sortedConnection)) {
                        connections.add(sortedConnection)
                        errorMessage = null
                    }
                } else {
                    errorMessage = "Invalid connection: Select different gardens"
                }
            }) {
                Text("Add Connection")
            }
        }

        Button(onClick = { connections.clear() }) {
            Text("Clear Connections")
        }

        errorMessage?.let {
            Text(text = it, color = Color.Red, modifier = Modifier.padding(horizontal = 4.dp))
        }

        if (connections.isNotEmpty()) {
            Text("Current Connections:")
            connections.forEach { (from, to) ->
                Text("${from + 1} - ${to + 1}")
            }
        }

        Button(onClick = {
            if (numberOfGardens.toIntOrNull() == null || numberOfGardens.toInt() < 1) {
                errorMessage = "Please enter a valid number of gardens"
            } else {
                showGraph = true
                errorMessage = null
            }
        }) {
            Text("Generate Garden Network")
        }

        if (showGraph) {
            GardenGraphView(
                gardenCount = numberOfGardens.toInt(),
                connections = connections,
                modifier = Modifier.fillMaxWidth().height(600.dp)
            )
        }
    }
}

@Composable
fun GardenGraphView(
    gardenCount: Int,
    connections: List<Pair<Int, Int>>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFF0077B6),
        Color(0xFFFFD166)
    )


    val positions = remember { calculatePositions(gardenCount) }
    val nodeColors = remember { mutableStateMapOf<Int, Color>() }

    LaunchedEffect(gardenCount, connections) {
        nodeColors.clear()
        if (gardenCount < 1) return@LaunchedEffect

        val graph = Array(gardenCount) { mutableListOf<Int>() }
        connections.forEach { (from, to) ->
            if (from < gardenCount && to < gardenCount) {
                graph[from].add(to)
                graph[to].add(from)
            }
        }

        val assignedColors = IntArray(gardenCount) { -1 }

        for (i in 0 until gardenCount) {
            val usedColors = graph[i].mapNotNull { neighbor ->
                if (neighbor < assignedColors.size) assignedColors[neighbor] else null
            }.toSet()

            val availableColor = (0 until colors.size).firstOrNull { it !in usedColors } ?: 0
            assignedColors[i] = availableColor
            nodeColors[i] = colors[availableColor]
        }
    }

    Box(
        modifier = modifier
            .background(Color.White)
            .border(1.dp, Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            connections.forEach { (from, to) ->
                if (from < positions.size && to < positions.size) {
                    drawLine(
                        color = Color.DarkGray,
                        start = positions[from],
                        end = positions[to],
                        strokeWidth = 4f
                    )
                }
            }

            positions.forEachIndexed { index, offset ->
                drawCircle(
                    color = nodeColors[index] ?: Color.LightGray,
                    center = offset,
                    radius = 40f
                )
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 40f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText(
                        (index + 1).toString(),
                        offset.x,
                        offset.y + 15,
                        paint
                    )
                }
            }
        }
    }
}

private fun calculatePositions(count: Int): List<Offset> {
    if (count == 0) return emptyList()
    val radius = 250f
    val center = Offset(400f, 400f)
    val angleStep = 2 * Math.PI / count

    return List(count) { i ->
        val angle = i * angleStep
        Offset(
            (center.x + radius * cos(angle)).toFloat(),
            (center.y + radius * sin(angle)).toFloat()
        )
    }
}