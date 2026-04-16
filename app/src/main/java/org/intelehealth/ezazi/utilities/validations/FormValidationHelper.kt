package org.intelehealth.ezazi.utilities.validations

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import org.intelehealth.ezazi.R

object FormValidationHelper {

    @JvmStatic
    fun applyFieldValidation(
        context: Context,
        focusView: View?,
        errorTextView: TextView?,
        cardView: MaterialCardView?,
        errorMessage: String?
    ) {

        if (errorMessage != null) {

            focusView?.requestFocus()

            errorTextView?.apply {
                visibility = View.VISIBLE
                text = errorMessage
            }

            cardView?.strokeColor =
                ContextCompat.getColor(context, R.color.error_red)

        } else {

            errorTextView?.visibility = View.GONE

            cardView?.strokeColor =
                ContextCompat.getColor(context, R.color.colorScrollbar)
        }
    }
}