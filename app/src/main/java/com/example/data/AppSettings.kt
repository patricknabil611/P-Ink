package com.example.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.R

data class PadBackgroundOption(
    val id: String,
    @param:StringRes val labelRes: Int,
    val backgroundColor: Color,
    val guideLineColor: Color,
    val isLight: Boolean,
)

data class PenColorOption(
    val id: String,
    @param:StringRes val labelRes: Int,
    val strokeColor: Color,
    val isLight: Boolean,
)

enum class WritingPadSize(val heightFraction: Float, val minDp: Int, val maxDp: Int) {
    COMPACT(0.24f, 125, 205),
    STANDARD(0.32f, 145, 265),
    TALL(0.42f, 185, 340),
}

object InkColorPalettes {
    val backgroundOptions: List<PadBackgroundOption> = listOf(
        PadBackgroundOption(
            id = "obsidian",
            labelRes = R.string.color_bg_obsidian,
            backgroundColor = Color(0xFF141B26),
            guideLineColor = Color(0xFF233044),
            isLight = false,
        ),
        PadBackgroundOption(
            id = "midnight",
            labelRes = R.string.color_bg_midnight,
            backgroundColor = Color(0xFF0E172A),
            guideLineColor = Color(0xFF1E293B),
            isLight = false,
        ),
        PadBackgroundOption(
            id = "graphite",
            labelRes = R.string.color_bg_graphite,
            backgroundColor = Color(0xFF1C1F26),
            guideLineColor = Color(0xFF2E3440),
            isLight = false,
        ),
        PadBackgroundOption(
            id = "plum",
            labelRes = R.string.color_bg_plum,
            backgroundColor = Color(0xFF211428),
            guideLineColor = Color(0xFF392344),
            isLight = false,
        ),
        PadBackgroundOption(
            id = "forest",
            labelRes = R.string.color_bg_forest,
            backgroundColor = Color(0xFF10211B),
            guideLineColor = Color(0xFF1E3B31),
            isLight = false,
        ),
        PadBackgroundOption(
            id = "cream",
            labelRes = R.string.color_bg_cream,
            backgroundColor = Color(0xFFFDF8EE),
            guideLineColor = Color(0xFFE2DAC5),
            isLight = true,
        ),
        PadBackgroundOption(
            id = "paper_white",
            labelRes = R.string.color_bg_white,
            backgroundColor = Color(0xFFFFFFFF),
            guideLineColor = Color(0xFFE2E8F0),
            isLight = true,
        ),
    )

    val penOptions: List<PenColorOption> = listOf(
        PenColorOption(
            id = "ice_white",
            labelRes = R.string.color_pen_ice,
            strokeColor = Color(0xFFF1F5F9),
            isLight = true,
        ),
        PenColorOption(
            id = "cyan",
            labelRes = R.string.color_pen_cyan,
            strokeColor = Color(0xFF38BDF8),
            isLight = true,
        ),
        PenColorOption(
            id = "pink",
            labelRes = R.string.color_pen_pink,
            strokeColor = Color(0xFFF472B6),
            isLight = true,
        ),
        PenColorOption(
            id = "emerald",
            labelRes = R.string.color_pen_emerald,
            strokeColor = Color(0xFF34D399),
            isLight = true,
        ),
        PenColorOption(
            id = "amber",
            labelRes = R.string.color_pen_amber,
            strokeColor = Color(0xFFFBBF24),
            isLight = true,
        ),
        PenColorOption(
            id = "violet",
            labelRes = R.string.color_pen_violet,
            strokeColor = Color(0xFFA78BFA),
            isLight = true,
        ),
        PenColorOption(
            id = "charcoal",
            labelRes = R.string.color_pen_charcoal,
            strokeColor = Color(0xFF0F172A),
            isLight = false,
        ),
        PenColorOption(
            id = "royal_blue",
            labelRes = R.string.color_pen_royal_blue,
            strokeColor = Color(0xFF1D4ED8),
            isLight = false,
        ),
    )

    fun findBackground(id: String): PadBackgroundOption =
        backgroundOptions.firstOrNull { it.id == id } ?: backgroundOptions.first()

    fun findPen(id: String): PenColorOption =
        penOptions.firstOrNull { it.id == id } ?: penOptions.first()
}

data class AppSettings(
    val padBackgroundId: String = "obsidian",
    val penColorId: String = "ice_white",
    val showGuideLines: Boolean = true,
    val autoConvertEnabled: Boolean = true,
    val writingPadSize: WritingPadSize = WritingPadSize.STANDARD,
    val compactLists: Boolean = false,
    val cloudSyncEnabled: Boolean = true,
) {
    val backgroundOption: PadBackgroundOption
        get() = InkColorPalettes.findBackground(padBackgroundId)

    val penOption: PenColorOption
        get() = InkColorPalettes.findPen(penColorId)
}
