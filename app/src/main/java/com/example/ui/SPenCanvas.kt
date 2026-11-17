package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ink.InkPoint
import com.example.ink.InkStroke
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanOnContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.InkPadRule
import com.example.ui.theme.InkPadSurface
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PinkAccent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SPenWritingPad(
    strokes: List<InkStroke>,
    conversionState: ConversionState,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onStrokeStarted: () -> Unit,
    onStrokeCompleted: (InkStroke) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activePoints by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    var hoverOffset by remember { mutableStateOf<Offset?>(null) }

    val padShape = RoundedCornerShape(22.dp)
    val isDrawingActive = activePoints.isNotEmpty()
    val borderColor = when {
        isDrawingActive -> CyanPrimary
        conversionState is ConversionState.Converting -> PinkAccent.copy(alpha = 0.7f)
        else -> BorderSubtle
    }

    Column(
        modifier = modifier
            .clip(padShape)
            .background(InkPadSurface)
            .border(width = 1.5.dp, color = borderColor, shape = padShape)
    ) {
        // Top status bar inside the ink pad
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                color = CyanContainer.copy(alpha = 0.75f),
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Draw,
                        contentDescription = null,
                        tint = CyanOnContainer,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.badge_spen_only),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanOnContainer,
                    )
                }
            }

            when (conversionState) {
                ConversionState.Converting -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("converting_indicator"),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = CyanPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.status_converting),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                ConversionState.AutoConvertPending -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PinkAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.status_auto_convert_pending),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate,
                        )
                    }
                }
                else -> Unit
            }
        }

        // Drawing Surface — strictly accepts PointerType.Stylus ("pen") only
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("spen_canvas")
                    .onSizeChanged(onCanvasSizeChanged)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            var activePenPointerId: PointerId? = null
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)

                                // Track stylus hover cursor when hovering near the Galaxy screen
                                val hoverPen = event.changes.firstOrNull {
                                    it.type == PointerType.Stylus && !it.pressed
                                }
                                hoverOffset = if (
                                    hoverPen != null &&
                                    event.type != PointerEventType.Exit &&
                                    activePenPointerId == null
                                ) {
                                    hoverPen.position
                                } else if (activePenPointerId != null || event.type == PointerEventType.Exit) {
                                    null
                                } else {
                                    hoverOffset
                                }

                                // Strictly filter for S Pen (PointerType.Stylus) ONLY.
                                // Finger and palm (PointerType.Touch) never draw.
                                val penChanges = event.changes.filter {
                                    it.type == PointerType.Stylus
                                }
                                if (penChanges.isEmpty()) {
                                    continue
                                }

                                val currentId = activePenPointerId
                                if (currentId == null) {
                                    val downChange = penChanges.firstOrNull {
                                        it.changedToDownIgnoreConsumed() ||
                                            (it.pressed && !it.previousPressed)
                                    }
                                    if (downChange != null) {
                                        activePenPointerId = downChange.id
                                        hoverOffset = null
                                        downChange.consume()
                                        val point = InkPoint(
                                            x = downChange.position.x.coerceIn(0f, size.width.toFloat()),
                                            y = downChange.position.y.coerceIn(0f, size.height.toFloat()),
                                            pressure = downChange.pressure,
                                        )
                                        activePoints = listOf(point)
                                        onStrokeStarted()
                                    }
                                } else {
                                    val trackedChange = penChanges.firstOrNull { it.id == currentId }
                                    if (trackedChange != null) {
                                        if (trackedChange.pressed) {
                                            trackedChange.consume()
                                            val maxW = size.width.toFloat()
                                            val maxH = size.height.toFloat()
                                            val batch = buildList {
                                                for (hist in trackedChange.historical) {
                                                    add(
                                                        InkPoint(
                                                            x = hist.position.x.coerceIn(0f, maxW),
                                                            y = hist.position.y.coerceIn(0f, maxH),
                                                            pressure = trackedChange.pressure,
                                                        )
                                                    )
                                                }
                                                add(
                                                    InkPoint(
                                                        x = trackedChange.position.x.coerceIn(0f, maxW),
                                                        y = trackedChange.position.y.coerceIn(0f, maxH),
                                                        pressure = trackedChange.pressure,
                                                    )
                                                )
                                            }
                                            activePoints = activePoints + batch
                                        } else {
                                            trackedChange.consume()
                                            if (activePoints.isNotEmpty()) {
                                                onStrokeCompleted(InkStroke(activePoints.toList()))
                                            }
                                            activePoints = emptyList()
                                            activePenPointerId = null
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                drawGuideLines()

                for (stroke in strokes) {
                    drawInkStroke(stroke.points, IceWhite)
                }
                if (activePoints.isNotEmpty()) {
                    drawInkStroke(activePoints, CyanPrimary)
                }

                hoverOffset?.let { pos ->
                    drawCircle(
                        color = CyanPrimary.copy(alpha = 0.55f),
                        radius = 7.dp.toPx(),
                        center = pos,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }

            if (strokes.isEmpty() && activePoints.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.canvas_hint_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = CoolSlate.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.canvas_hint_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGuideLines() {
    val lineCount = 3
    val stepY = size.height / (lineCount + 1)
    for (i in 1..lineCount) {
        val y = stepY * i
        drawLine(
            color = InkPadRule,
            start = Offset(20.dp.toPx(), y),
            end = Offset(size.width - 20.dp.toPx(), y),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private fun DrawScope.drawInkStroke(points: List<InkPoint>, color: Color) {
    if (points.isEmpty()) return
    val baseStrokeWidth = 3.6.dp.toPx()

    if (points.size == 1) {
        drawCircle(
            color = color,
            radius = baseStrokeWidth * 0.6f,
            center = Offset(points[0].x, points[0].y),
        )
        return
    }

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) * 0.5f
            val midY = (prev.y + curr.y) * 0.5f
            quadraticTo(prev.x, prev.y, midX, midY)
        }
        lineTo(points.last().x, points.last().y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = baseStrokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}
