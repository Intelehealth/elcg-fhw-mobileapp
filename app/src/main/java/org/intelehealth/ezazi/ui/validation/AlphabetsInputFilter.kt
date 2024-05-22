package org.intelehealth.ezazi.ui.validation

import android.text.InputFilter
import android.text.Spanned

class AlphabetsInputFilter : InputFilter{
    private val regex = Regex("^[a-zA-Z0-9\\p{Punct} ]*$")

    override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
    ): CharSequence? {
        val input = source.subSequence(start, end).toString()

        // Filter to keep only valid characters
        val filtered = input.filter { regex.matches(it.toString()) }

        // If the filtered text differs from the original, return it
        return if (filtered != input) filtered else null
    }
}