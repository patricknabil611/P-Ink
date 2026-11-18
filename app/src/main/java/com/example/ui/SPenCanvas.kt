package com.example.ui

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ink.InkPoint
import com.example.ink.InkStroke
import com.example.ink.findIntersectedStrokeIndices
import com.example.ink.normalizeInkPressure
import com.example.ink.pressureToStrokeWidth
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.InkPadRule
import com.example.ui.theme.InkPadSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.RoseError

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SPenWritingPad(
    strokes: List<InkStroke>,
    conversionState: ConversionState,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onStrokeStarted: () -> Unit,
    onStrokeCompleted: (InkStroke) -> Unit,
    onEraseStrokes: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    padBackgroundColor: Color = InkPadSurface,
    guideLineColor: Color = InkPadRule,
    penColor: Color = IceWhite,
    showGuideLines: Boolean = true,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val eraserRadiusPx = with(density) { 18.dp.toPx() }

    val latestStrokes by rememberUpdatedState(strokes)
    val latestOnStrokeStarted by rememberUpdatedState(onStrokeStarted)
    val latestOnStrokeCompleted by rememberUpdatedState(onStrokeCompleted)
    val latestOnEraseStrokes by rememberUpdatedState(onEraseStrokes)

    var activePoints by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    var hoverOffset by remember { mutableStateOf<Offset?>(null) }
    var hoverIsEraser by remember { mutableStateOf(false) }
    var activeEraserOffset by remember { mutableStateOf<Offset?>(null) }

    val padShape = RoundedCornerShape(22.dp)
    val isDrawingActive = activePoints.isNotEmpty()
    val isErasingActive = activeEraserOffset != null
    val borderColor = when {
        isErasingActive -> RoseError
        isDrawingActive -> CyanPrimary
        conversionState is ConversionState.Converting -> PinkAccent.copy(alpha = 0.7f)
        else -> BorderSubtle
    }

    // Pure separated writing box — no badges, hints, or status text inside
    Box(
        modifier = modifier
            .clip(padShape)
            .background(padBackgroundColor)
            .border(width = 1.5.dp, color = borderColor, shape = padShape)
            .testTag("spen_writing_box")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("spen_canvas")
                .onSizeChanged(onCanvasSizeChanged)
                .pointerInteropFilter { motionEvent ->
                    if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                        val toolType = motionEvent.getToolType(motionEvent.actionIndex)
                        if (toolType == MotionEvent.TOOL_TYPE_STYLUS ||
                            toolType == MotionEvent.TOOL_TYPE_ERASER
                        ) {
                            view.requestUnbufferedDispatch(motionEvent)
                        }
                    }
                    false
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        var activePenPointerId: PointerId? = null
                        var isErasingGesture = false
                        var lastEraserPos: Offset? = null

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val isSideButtonHeld =
                                event.buttons.isSecondaryPressed || event.buttons.isTertiaryPressed

                            // Track stylus hover reticle (cyan for pen, rose ring when side button held)
                            val hoverStylus = event.changes.firstOrNull {
                                (it.type == PointerType.Stylus || it.type == PointerType.Eraser) && !it.pressed
                            }
                            if (hoverStylus != null &&
                                event.type != PointerEventType.Exit &&
                                activePenPointerId == null
                            ) {
                                hoverOffset = hoverStylus.position
                                hoverIsEraser = hoverStylus.type == PointerType.Eraser || isSideButtonHeld
                            } else if (activePenPointerId != null || event.type == PointerEventType.Exit) {
                                hoverOffset = null
                                hoverIsEraser = false
                            }

                            // Strictly accept S Pen (Stylus / Eraser) ONLY.
                            // Finger and palm touches (PointerType.Touch) never draw or erase.
                            val stylusChanges = event.changes.filter {
                                it.type == PointerType.Stylus || it.type == PointerType.Eraser
                            }
                            if (stylusChanges.isEmpty()) {
                                continue
                            }

                            val currentId = activePenPointerId
                            if (currentId == null) {
                                val downChange = stylusChanges.firstOrNull {
                                    it.changedToDownIgnoreConsumed() ||
                                        (it.pressed && !it.previousPressed)
                                }
                                if (downChange != null) {
                                    activePenPointerId = downChange.id
                                    hoverOffset = null
                                    downChange.consume()

                                    val startInEraser =
                                        downChange.type == PointerType.Eraser || isSideButtonHeld

                                    if (startInEraser) {
                                        isErasingGesture = true
                                        activePoints = emptyList()
                                        activeEraserOffset = downChange.position
                                        lastEraserPos = downChange.position

                                        val hits = findIntersectedStrokeIndices(
                                            strokes = latestStrokes,
                                            x0 = downChange.position.x,
                                            y0 = downChange.position.y,
                                            x1 = downChange.position.x,
                                            y1 = downChange.position.y,
                                            radius = eraserRadiusPx,
                                        )
                                        if (hits.isNotEmpty()) {
                                            latestOnEraseStrokes(hits)
                                        }
                                    } else {
                                        isErasingGesture = false
                                        activeEraserOffset = null
                                        lastEraserPos = null
                                        val point = InkPoint(
                                            x = downChange.position.x.coerceIn(0f, size.width.toFloat()),
                                            y = downChange.position.y.coerceIn(0f, size.height.toFloat()),
                                            pressure = downChange.pressure,
                                        )
                                        activePoints = listOf(point)
                                        latestOnStrokeStarted()
                                    }
                                }
                            } else {
                                val trackedChange = stylusChanges.firstOrNull { it.id == currentId }
                                if (trackedChange != null) {
                                    if (trackedChange.pressed) {
                                        trackedChange.consume()
                                        if (isErasingGesture) {
                                            activeEraserOffset = trackedChange.position
                                            var prevPos = lastEraserPos ?: trackedChange.position
                                            val allHits = HashSet<Int>()

                                            for (hist in trackedChange.historical) {
                                                allHits += findIntersectedStrokeIndices(
                                                    strokes = latestStrokes,
                                                    x0 = prevPos.x,
                                                    y0 = prevPos.y,
                                                    x1 = hist.position.x,
                                                    y1 = hist.position.y,
                                                    radius = eraserRadiusPx,
                                                )
                                                prevPos = hist.position
                                            }
                                            allHits += findIntersectedStrokeIndices(
                                                strokes = latestStrokes,
                                                x0 = prevPos.x,
                                                y0 = prevPos.y,
                                                x1 = trackedChange.position.x,
                                                y1 = trackedChange.position.y,
                                                radius = eraserRadiusPx,
                                            )
                                            lastEraserPos = trackedChange.position
                                            if (allHits.isNotEmpty()) {
                                                latestOnEraseStrokes(allHits)
                                            }
                                        } else {
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
                                        }
                                    } else {
                                        trackedChange.consume()
                                        if (isErasingGesture) {
                                            isErasingGesture = false
                                            activeEraserOffset = null
                                            lastEraserPos = null
                                        } else {
                                            if (activePoints.isNotEmpty()) {
                                                latestOnStrokeCompleted(InkStroke(activePoints.toList()))
                                            }
                                            activePoints = emptyList()
                                        }
                                        activePenPointerId = null
                                    }
                                } else if (stylusChanges.none { it.pressed }) {
                                    // Safety reset if pointer ID vanished without an explicit up event
                                    if (!isErasingGesture && activePoints.isNotEmpty()) {
                                        latestOnStrokeCompleted(InkStroke(activePoints.toList()))
                                    }
                                    activePoints = emptyList()
                                    isErasingGesture = false
                                    activeEraserOffset = null
                                    lastEraserPos = null
                                    activePenPointerId = null
                                }
                            }
                        }
                    }
                }
        ) {
            if (showGuideLines) {
                drawGuideLines(guideLineColor)
            }

            for (stroke in strokes) {
                drawPressureInkStroke(stroke.points, penColor)
            }
            if (activePoints.isNotEmpty()) {
                drawPressureInkStroke(activePoints, penColor)
            }

            activeEraserOffset?.let { pos ->
                drawCircle(
                    color = RoseError.copy(alpha = 0.16f),
                    radius = 18.dp.toPx(),
                    center = pos,
                )
                drawCircle(
                    color = RoseError,
                    radius = 18.dp.toPx(),
                    center = pos,
                    style = Stroke(width = 2.dp.toPx()),
                )
            } ?: hoverOffset?.let { pos ->
                if (hoverIsEraser) {
                    drawCircle(
                        color = RoseError.copy(alpha = 0.12f),
                        radius = 16.dp.toPx(),
                        center = pos,
                    )
                    drawCircle(
                        color = RoseError.copy(alpha = 0.85f),
                        radius = 16.dp.toPx(),
                        center = pos,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                } else {
                    drawCircle(
                        color = CyanPrimary.copy(alpha = 0.55f),
                        radius = 7.dp.toPx(),
                        center = pos,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGuideLines(ruleColor: Color) {
    val lineCount = (size.height / 62.dp.toPx()).toInt().coerceIn(3, 5)
    val stepY = size.height / (lineCount + 1)
    val inset = 20.dp.toPx()
    val strokePx = 1.dp.toPx()
    for (i in 1..lineCount) {
        val y = stepY * i
        drawLine(
            color = ruleColor,
            start = Offset(inset, y),
            end = Offset(size.width - inset, y),
            strokeWidth = strokePx,
        )
    }
}

private fun DrawScope.drawPressureInkStroke(points: List<InkPoint>, color: Color) {
    if (points.isEmpty()) return
    val minStrokePx = 2.0.dp.toPx()
    val maxStrokePx = 5.4.dp.toPx()

    if (points.size == 1) {
        val singleWidth = pressureToStrokeWidth(points[0].pressure, minStrokePx, maxStrokePx)
        drawCircle(
            color = color,
            radius = singleWidth * 0.6f,
            center = Offset(points[0].x, points[0].y),
        )
        return
    }

    var prevMid = Offset(points[0].x, points[0].y)
    val segmentPath = Path()

    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val mid = if (i == points.lastIndex) {
            Offset(curr.x, curr.y)
        } else {
            Offset((prev.x + curr.x) * 0.5f, (prev.y + curr.y) * 0.5f)
        }

        val avgPressure = (normalizeInkPressure(prev.pressure) + normalizeInkPressure(curr.pressure)) * 0.5f
        val segmentWidth = pressureToStrokeWidth(avgPressure, minStrokePx, maxStrokePx)

        segmentPath.reset()
        segmentPath.moveTo(prevMid.x, prevMid.y)
        if (i == points.lastIndex && points.size == 2) {
            segmentPath.lineTo(curr.x, curr.y)
        } else {
            segmentPath.quadraticTo(prev.x, prev.y, mid.x, mid.y)
        }

        drawPath(
            path = segmentPath,
            color = color,
            style = Stroke(
                width = segmentWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        prevMid = mid
    }
}
