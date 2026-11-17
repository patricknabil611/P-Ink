package com.example.util

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection

object TextDirectionUtil {

    /**
     * Determines whether text should be laid out right-to-left using the Unicode
     * first-strong directional character rule (handles Arabic, English, and mixed tasks).
     */
    fun isRtl(text: String): Boolean {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            when (Character.getDirectionality(codePoint)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE -> return true

                Character.DIRECTIONALITY_LEFT_TO_RIGHT,
                Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING,
                Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE -> return false
            }
            index += Character.charCount(codePoint)
        }
        return false
    }

    fun textDirectionFor(text: String): TextDirection =
        if (isRtl(text)) TextDirection.Rtl else TextDirection.Ltr

    fun textAlignFor(text: String): TextAlign =
        if (isRtl(text)) TextAlign.Right else TextAlign.Left

    fun layoutDirectionFor(text: String): LayoutDirection =
        if (isRtl(text)) LayoutDirection.Rtl else LayoutDirection.Ltr
}
