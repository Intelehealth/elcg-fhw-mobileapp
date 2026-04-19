package org.intelehealth.ezazi.partogram.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.partogram.PartogramConstants
import org.intelehealth.ezazi.partogram.model.ParamInfo
import org.intelehealth.ezazi.stage3.Utils.GenericMultiChoiceAdapter
import org.intelehealth.ezazi.ui.dialog.MultiChoiceDialogFragment


class MultiSelectDropdownHandlerForDataCapture(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val accessMode: PartogramConstants.AccessMode
) {

    // called from adapter's onBindViewHolder

    fun bind(
        tempView: View,
        info: ParamInfo
    ) {
        val tvParamName       = tempView.findViewById<TextView>(R.id.tvParamName)
        val tvSelected        = tempView.findViewById<TextView>(R.id.tvSelectedValues)
        val etOther           = tempView.findViewById<EditText>(R.id.etOtherText)
        val clOtherContainer  = tempView.findViewById<ConstraintLayout>(R.id.clOtherContainer)

        val isEditable = accessMode != PartogramConstants.AccessMode.READ
        tvSelected.isEnabled = isEditable
        etOther.isEnabled    = isEditable

        tvParamName.text = info.paramName

        // Restore saved state when row is bound
        restoreState(info, tvSelected, etOther, clOtherContainer)

        // Open dialog on tap
        tvSelected.setOnClickListener {
            if (!isEditable) return@setOnClickListener
            showDialog(info, tvSelected, etOther, clOtherContainer)
        }

        // Persist free-text as user types
        etOther.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentLabels = labelsFromView(tvSelected)
                saveValue(info, currentLabels, s?.toString()?.trim() ?: "")
            }
        })
    }

    // Dialog
    private fun showDialog(
        info: ParamInfo,
        tvSelected: TextView,
        etOther: EditText,
        clOtherContainer: ConstraintLayout
    ) {
        val options     = info.options
        val preSelected = buildPreSelectedList(info)

        val dialog = MultiChoiceDialogFragment.Builder<String>(context)
            .title(R.string.select)
            .positiveButtonLabel(R.string.save_button)
            .build()
        dialog.isSearchable(true)

        val adapter = GenericMultiChoiceAdapter(
            context,
            ArrayList(options.toList()),
            context.getString(R.string.none_option)
        )
        dialog.setAdapter(adapter)

        // Pre-tick previously selected items
        options.forEachIndexed { index, opt ->
            val shouldSelect = when {
                opt.equals(AppConstants.OTHER_OPTION, ignoreCase = true) ->
                    info.capturedValue?.contains(AppConstants.OTHER_OPTION + "|") == true
                else ->
                    preSelected.contains(opt)
            }
            if (shouldSelect) adapter.selectItem(index)
        }

        dialog.setListener { selectedItems ->
            val otherPicked = selectedItems.contains(AppConstants.OTHER_OPTION)

            if (otherPicked) {
                clOtherContainer.visibility = View.VISIBLE
                // Pre-fill already-typed free text if EditText is empty
                val existing = extractOtherFreeText(info.capturedValue)
                if (existing.isNotEmpty() && etOther.text.isNullOrEmpty()) {
                    etOther.setText(existing)
                }
            } else {
                clOtherContainer.visibility = View.GONE
                etOther.setText("")
            }

            tvSelected.text = selectedItems.joinToString(", ")
            saveValue(info, selectedItems, etOther.text?.toString()?.trim() ?: "")
        }

        dialog.show(fragmentManager, MultiChoiceDialogFragment::class.java.canonicalName)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State restore
    // ─────────────────────────────────────────────────────────────────────────

    private fun restoreState(
        info: ParamInfo,
        tvSelected: TextView,
        etOther: EditText,
        clOtherContainer: ConstraintLayout
    ) {
        val saved = info.capturedValue
        if (saved.isNullOrEmpty()) return

        val displayParts  = mutableListOf<String>()
        var otherFreeText = ""

        saved.split(",").forEach { raw ->
            val token = raw.trim()
            when {
                token.startsWith(AppConstants.OTHER_OPTION + "|") -> {
                    otherFreeText = token.removePrefix(AppConstants.OTHER_OPTION + "|")
                    displayParts += if (otherFreeText.isEmpty()) AppConstants.OTHER_OPTION
                    else "${AppConstants.OTHER_OPTION}: $otherFreeText"
                }
                token.isNotEmpty() -> displayParts += token
            }
        }

        tvSelected.text = displayParts.joinToString(", ")

        if (otherFreeText.isNotEmpty()) {
            clOtherContainer.visibility = View.VISIBLE
            etOther.setText(otherFreeText)
        } else {
            clOtherContainer.visibility = View.GONE
        }
    }



     //* Builds plain label list from capturedValue for pre-ticking the dialog.
     //* "Other|<text>" - "Other"

    private fun buildPreSelectedList(info: ParamInfo): List<String> {
        val saved = info.capturedValue ?: return emptyList()
        return saved.split(",").mapNotNull { raw ->
            val token = raw.trim()
            when {
                token.startsWith(AppConstants.OTHER_OPTION + "|") -> AppConstants.OTHER_OPTION
                token.isNotEmpty() -> token
                else -> null
            }
        }
    }


      //Serialises selected labels into capturedValue.
      //Format: "Label1,Label2,Other|<free text>"

    private fun saveValue(
        info: ParamInfo,
        selectedLabels: List<String>,
        otherFreeText: String
    ) {
        if (selectedLabels.isEmpty()) {
            info.capturedValue = ""
            return
        }
        info.capturedValue = selectedLabels.joinToString(",") { label ->
            if (label.equals(AppConstants.OTHER_OPTION, ignoreCase = true))
                "${AppConstants.OTHER_OPTION}|$otherFreeText"
            else
                label
        }
    }


     //* Reads tvSelected display text back into plain labels.
     //* "Other: <text>" → "Other"

    private fun labelsFromView(tvSelected: TextView): List<String> {
        val text = tvSelected.text?.toString()?.trim() ?: return emptyList()
        return text.split(",").mapNotNull { part ->
            val trimmed = part.trim()
            when {
                trimmed.startsWith(AppConstants.OTHER_OPTION) -> AppConstants.OTHER_OPTION
                trimmed.isNotEmpty() -> trimmed
                else -> null
            }
        }
    }


     //* Extracts free text after "Other|" from capturedValue.

    private fun extractOtherFreeText(capturedValue: String?): String {
        if (capturedValue.isNullOrEmpty()) return ""
        return capturedValue.split(",").firstOrNull { token ->
            token.trim().startsWith(AppConstants.OTHER_OPTION + "|")
        }?.trim()?.removePrefix(AppConstants.OTHER_OPTION + "|") ?: ""
    }
}