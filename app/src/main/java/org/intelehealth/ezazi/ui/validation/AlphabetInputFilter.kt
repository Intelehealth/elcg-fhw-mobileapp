package org.intelehealth.ezazi.ui.validation

import android.text.InputFilter
import android.text.Spanned

class AlphabetInputFilter : InputFilter {
    // Regular expression for validating alphabets and spaces
    private val regex = Regex("^[a-zA-Z ]*$")

    override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
    ): CharSequence? {
        // Check if the new input contains only alphabets and spaces
        val input = source.subSequence(start, end).toString()
        if (!regex.matches(input)) {
            return "" // If invalid, block the input
        }

        // If it's the start of the text, convert the first letter to uppercase
        if (dstart == 0 && input.isNotEmpty()) {
            // Get the first character and convert it to uppercase
            val firstChar = input[0].uppercase()
            // Append the rest of the input if there's more than one character
            val restOfInput = if (input.length > 1) input.substring(1) else ""
            return firstChar + restOfInput
        }

        return null // Accept all other valid input
    }

}
   /* override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
    ): CharSequence? {
        val regex = Regex("^[a-zA-Z ]*$")
        return if (regex.matches(source)) {
            null
        } else {
            ""
        }
    }
}*/