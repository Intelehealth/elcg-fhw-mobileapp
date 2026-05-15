package org.intelehealth.ezazi.stage3.Utils

import android.text.InputFilter
import android.text.Spanned

class DecimalDigitsInputFilter(
    private val maxDigitsBeforeDecimal: Int,
    private val maxDigitsAfterDecimal: Int
) :
    InputFilter {
    override fun filter(
        source: CharSequence, start: Int, end: Int,
        dest: Spanned, dstart: Int, dend: Int
    ): CharSequence? {
        // Build the resulting string after this edit

        val destStr = dest.toString()
        val resultStr = (destStr.substring(0, dstart)
                + source.subSequence(start, end)
                + destStr.substring(dend))

        // Allow empty (deletion)
        if (resultStr.isEmpty()) return null

        // Reject if more than one decimal point
        var dotCount = 0
        for (c in resultStr.toCharArray()) {
            if (c == '.') dotCount++
        }
        if (dotCount > 1) return ""

        val dotIndex = resultStr.indexOf('.')

        if (dotIndex >= 0) {
            // Has decimal point
            val beforeDot = resultStr.substring(0, dotIndex)
            val afterDot = resultStr.substring(dotIndex + 1)

            if (beforeDot.length > maxDigitsBeforeDecimal) return ""
            if (afterDot.length > maxDigitsAfterDecimal) return ""
        } else {
            // No decimal point yet — only check digits before decimal
            if (resultStr.length > maxDigitsBeforeDecimal) return ""
        }

        return null // accept
    }
}