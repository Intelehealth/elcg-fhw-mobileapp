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
import org.intelehealth.ezazi.utilities.UuidDictionary

class MultiSelectDropdownHandlerForDataCapture(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val accessMode: PartogramConstants.AccessMode
) {

    private fun getComplicationOptions(conceptUUID: String?): Array<String> {
        return when (conceptUUID) {
            UuidDictionary.ONGOING_COMPLICATIONS_MOTHER -> arrayOf(
                "Vaginal bleeding (≥300 ml)",
                "Fever (Temp ≥99.5 °F)",
                "High blood pressure",
                "Moderate pallor",
                "Severe pallor",
                AppConstants.OTHER_OPTION
            )
            UuidDictionary.ONGOING_COMPLICATIONS_NEWBORN -> arrayOf(
                "Respiratory distress",
                "Hypothermia",
                "Sepsis",
                "Poor feeding",
                AppConstants.OTHER_OPTION
            )
            else -> emptyArray()
        }
    }

    fun bind(tempView: View, info: ParamInfo) {
        val options            = getComplicationOptions(info.conceptUUID)
        val tvSelected         = tempView.findViewById<TextView>(R.id.tvSelectedValue)
        val clOtherContainer   = tempView.findViewById<ConstraintLayout>(R.id.clOtherContainer)
        val etOtherText        = tempView.findViewById<EditText>(R.id.etOtherText)
        val clOngoingNextLayout = tempView.findViewById<ConstraintLayout>(R.id.clOngoingNextLayout)

        val isEditable = accessMode != PartogramConstants.AccessMode.READ
        tvSelected.isEnabled   = isEditable
        tvSelected.isClickable = isEditable
        etOtherText.isEnabled  = isEditable

        //  Restore saved state ──────────────────────────────────────────────
        restoreState(info, tvSelected, clOtherContainer, etOtherText, clOngoingNextLayout)

        tvSelected.setOnClickListener {
            if (!isEditable) return@setOnClickListener
            showDialog(options, info, tvSelected, clOtherContainer, etOtherText)
        }

        //  Other EditText
        etOtherText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                persistValue(
                    info,
                    selectedLabelsFromView(tvSelected),
                    s?.toString()?.trim() ?: ""
                )
            }
        })
    }


    private fun showDialog(
        options: Array<String>,
        info: ParamInfo,
        tvSelected: TextView,
        clOtherContainer: ConstraintLayout,
        etOtherText: EditText
    ) {
        val preSelected = selectedLabelsFromView(tvSelected)

        val adapter = GenericMultiChoiceAdapter(
            context,
            ArrayList(options.toList()),
            context.getString(R.string.none_option)
        )

        // Pre-tick previously selected items
        options.forEachIndexed { index, opt ->
            if (preSelected.contains(opt)) adapter.selectItem(index)
        }

        val dialog = MultiChoiceDialogFragment.Builder<String>(context)
            .title(R.string.select)
            .positiveButtonLabel(R.string.save_button)
            .build()
        dialog.isSearchable(true)
        dialog.setAdapter(adapter)

        dialog.setListener { selectedItems ->
            val otherPicked = selectedItems.contains(AppConstants.OTHER_OPTION)

            // Show / hide Other free-text row
            if (otherPicked) {
                // setText before VISIBLE so the container measures correct height
                val existingOther = extractOtherText(info.capturedValue)
                if (existingOther.isNotEmpty() && etOtherText.text.isNullOrEmpty()) {
                    etOtherText.setText(existingOther)
                    etOtherText.setSelection(existingOther.length)
                }
                clOtherContainer.visibility = View.VISIBLE
                clOtherContainer.requestLayout()
            } else {
                clOtherContainer.visibility = View.GONE
                clOtherContainer.requestLayout()
                etOtherText.setText("")
            }

            // Update display text in the dropdown TextView
            tvSelected.text = selectedItems.joinToString(", ")

            // Persist
            persistValue(info, selectedItems, etOtherText.text?.toString()?.trim() ?: "")
        }

        dialog.show(fragmentManager, MultiChoiceDialogFragment::class.java.canonicalName)
    }

    private fun restoreState(
        info: ParamInfo,
        tvSelected: TextView,
        clOtherContainer: ConstraintLayout,
        etOtherText: EditText,
        clOngoingNextLayout: ConstraintLayout
    ) {
        // ✅ Always reset to clean slate first — prevents stale state on re-bind
        tvSelected.text = ""
        clOtherContainer.visibility = View.GONE
        etOtherText.setText("")

        val saved = info.capturedValue ?: return
        if (saved.isEmpty()) return

        try {
            val json          = org.json.JSONObject(saved)
            val yesNo         = json.optString("any ongoing complication", "")
            val complications = json.optString("complications", "")
            val otherValue    = json.optString("other value", "")

            // Only restore dropdown/Other when YES — if NO, clean slate above is correct
            if (!"yes".equals(yesNo, ignoreCase = true)) return

            if (complications.isNotEmpty()) tvSelected.text = complications

            val hasOther = complications.split(",")
                .map { it.trim() }
                .any { it.equals(AppConstants.OTHER_OPTION, ignoreCase = true) }

            if (hasOther) {
                if (otherValue.isNotEmpty()) {
                    etOtherText.setText(otherValue)
                    etOtherText.setSelection(otherValue.length)
                }
                clOtherContainer.visibility = View.VISIBLE
                clOtherContainer.requestLayout()
                clOngoingNextLayout.requestLayout()
            }
            // else: Other not selected → container stays GONE from reset above

        } catch (e: org.json.JSONException) {
            // malformed JSON — clean slate already applied at the top
        }
    }

    // JSON format:
    // {
    //   "any ongoing complication": "yes",
    //   "complications": "Fever, High blood pressure, Other",
    //   "other value": "Some free text"       - only present when Other is selected
    // }
    private fun persistValue(
        info: ParamInfo,
        selectedLabels: List<String>,
        otherFreeText: String
    ) {
        try {
            val json = org.json.JSONObject()
            json.put("any ongoing complication", "yes")
            json.put("complications", selectedLabels.joinToString(", "))
            // Only write "other value" key when Other is actually selected
            if (selectedLabels.any { it.equals(AppConstants.OTHER_OPTION, ignoreCase = true) }) {
                json.put("other value", otherFreeText)
            }
            info.capturedValue = json.toString()
        } catch (e: org.json.JSONException) {
            e.printStackTrace()
        }
    }

    // Parse tvSelected display text into a plain label list
    private fun selectedLabelsFromView(tvSelected: TextView): List<String> {
        val text = tvSelected.text?.toString()?.trim() ?: return emptyList()
        if (text.isEmpty()) return emptyList()
        return text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // Extract the "other value" from saved JSON
    private fun extractOtherText(capturedValue: String?): String {
        if (capturedValue.isNullOrEmpty()) return ""
        return try {
            org.json.JSONObject(capturedValue).optString("other value", "")
        } catch (e: org.json.JSONException) {
            ""
        }
    }
}