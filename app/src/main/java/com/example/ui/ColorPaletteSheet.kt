package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppSettings
import com.example.data.InkColorPalettes
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface

@Composable
fun CompactInkPalettePanel(
    settings: AppSettings,
    onSelectBackground: (String) -> Unit,
    onSelectPenColor: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .testTag("quick_palette_panel"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.palette_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.btn_dismiss),
                        tint = CoolSlate,
                    )
                }
            }

            PaletteSwatchRow(
                label = stringResource(R.string.palette_bg_label),
                items = InkColorPalettes.backgroundOptions.map { option ->
                    SwatchItem(
                        id = option.id,
                        color = option.backgroundColor,
                        isLight = option.isLight,
                        label = stringResource(option.labelRes),
                        selected = settings.padBackgroundId == option.id,
                        testTag = "bg_swatch_${option.id}",
                    )
                },
                onSelect = onSelectBackground,
            )

            Spacer(modifier = Modifier.height(6.dp))

            PaletteSwatchRow(
                label = stringResource(R.string.palette_pen_label),
                items = InkColorPalettes.penOptions.map { option ->
                    SwatchItem(
                        id = option.id,
                        color = option.strokeColor,
                        isLight = option.isLight,
                        label = stringResource(option.labelRes),
                        selected = settings.penColorId == option.id,
                        testTag = "pen_swatch_${option.id}",
                    )
                },
                onSelect = onSelectPenColor,
            )
        }
    }
}

@Composable
fun InkColorSettingsSection(
    settings: AppSettings,
    onSelectBackground: (String) -> Unit,
    onSelectPenColor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgOption = settings.backgroundOption
    val penOption = settings.penOption

    Column(modifier = modifier.fillMaxWidth()) {
        // Live mini preview of the writing box with active colors
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgOption.backgroundColor)
                .border(1.5.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .testTag("color_preview_pad"),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                if (settings.showGuideLines) {
                    val step = size.height / 3f
                    for (i in 1..2) {
                        val y = step * i
                        drawLine(
                            color = bgOption.guideLineColor,
                            start = Offset(16.dp.toPx(), y),
                            end = Offset(size.width - 16.dp.toPx(), y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.14f, h * 0.62f)
                    quadraticTo(w * 0.25f, h * 0.18f, w * 0.36f, h * 0.56f)
                    quadraticTo(w * 0.47f, h * 0.85f, w * 0.58f, h * 0.42f)
                    quadraticTo(w * 0.70f, h * 0.16f, w * 0.84f, h * 0.52f)
                }
                drawPath(
                    path = path,
                    color = penOption.strokeColor,
                    style = Stroke(
                        width = 3.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        PaletteSwatchRow(
            label = stringResource(R.string.palette_bg_label),
            items = InkColorPalettes.backgroundOptions.map { option ->
                SwatchItem(
                    id = option.id,
                    color = option.backgroundColor,
                    isLight = option.isLight,
                    label = stringResource(option.labelRes),
                    selected = settings.padBackgroundId == option.id,
                    testTag = "settings_bg_swatch_${option.id}",
                )
            },
            onSelect = onSelectBackground,
        )

        Spacer(modifier = Modifier.height(10.dp))

        PaletteSwatchRow(
            label = stringResource(R.string.palette_pen_label),
            items = InkColorPalettes.penOptions.map { option ->
                SwatchItem(
                    id = option.id,
                    color = option.strokeColor,
                    isLight = option.isLight,
                    label = stringResource(option.labelRes),
                    selected = settings.penColorId == option.id,
                    testTag = "settings_pen_swatch_${option.id}",
                )
            },
            onSelect = onSelectPenColor,
        )
    }
}

private data class SwatchItem(
    val id: String,
    val color: Color,
    val isLight: Boolean,
    val label: String,
    val selected: Boolean,
    val testTag: String,
)

@Composable
private fun PaletteSwatchRow(
    label: String,
    items: List<SwatchItem>,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CoolSlate,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(item.id) }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag(item.testTag),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(item.color)
                            .border(
                                width = if (item.selected) 2.5.dp else 1.dp,
                                color = if (item.selected) CyanPrimary else BorderSubtle,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = item.label,
                                tint = if (item.isLight) ObsidianBg else IceWhite,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.selected) IceWhite else CoolSlate,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
