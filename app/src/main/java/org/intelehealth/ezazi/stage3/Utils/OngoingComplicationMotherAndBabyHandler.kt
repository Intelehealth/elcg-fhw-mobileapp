package org.intelehealth.ezazi.stage3.Utils

import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.constraintlayout.widget.ConstraintLayout
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.databinding.PartoLblRadioViewOngoingComplicationBinding
import org.intelehealth.ezazi.partogram.PartogramConstants
import org.intelehealth.ezazi.partogram.model.ParamInfo
import org.intelehealth.ezazi.partogram.utils.MultiSelectDropdownHandlerForDataCapture
import org.json.JSONException
import org.json.JSONObject


class OngoingComplicationMotherAndBabyHandler(
    private val accessMode: PartogramConstants.AccessMode,
    private val multiSelectHandler: MultiSelectDropdownHandlerForDataCapture
) {

    fun bind(tempView: View, info: ParamInfo) {

        val binding =
            PartoLblRadioViewOngoingComplicationBinding.bind(tempView)

        val radioGroup =
            tempView.findViewById<RadioGroup>(R.id.radioYesNoGroup)

        radioGroup.setOnCheckedChangeListener(null)

        val saved = info.capturedValue
        var isYesSaved = false
        var hasSavedValue = false

        if (!saved.isNullOrEmpty()) {
            try {
                val json = JSONObject(saved)
                val yesNo = json.optString("any ongoing complication", "")

                if (yesNo.isNotEmpty()) {
                    hasSavedValue = true
                    isYesSaved = yesNo.trim().equals("yes", true)
                }
            } catch (_: JSONException) {
            }
        }

        if (hasSavedValue) {
            if (isYesSaved) {
                radioGroup.check(R.id.radioYes)
                info.checkedRadioOption = ParamInfo.RadioOptions.YES
                binding.clOngoingNextLayout.visibility = View.VISIBLE
                multiSelectHandler.bind(tempView, info)
            } else {
                radioGroup.check(R.id.radioNo)
                info.checkedRadioOption = ParamInfo.RadioOptions.NO
                binding.clOngoingNextLayout.visibility = View.GONE
                clearOngoingSubViews(tempView, binding)
            }
        } else {
            radioGroup.clearCheck()
            info.checkedRadioOption = null
            binding.clOngoingNextLayout.visibility = View.GONE
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->

            val rb = tempView.findViewById<RadioButton>(checkedId)
            if (rb == null || !rb.isChecked) return@setOnCheckedChangeListener

            if (checkedId == R.id.radioYes) {

                info.checkedRadioOption = ParamInfo.RadioOptions.YES
                binding.clOngoingNextLayout.visibility = View.VISIBLE
                binding.clOngoingNextLayout.requestLayout()

                persistOngoingJson(info, true)
                multiSelectHandler.bind(tempView, info)

            } else if (checkedId == R.id.radioNo) {

                info.checkedRadioOption = ParamInfo.RadioOptions.NO

                clearOngoingSubViews(tempView, binding)
                binding.clOngoingNextLayout.visibility = View.GONE

                persistOngoingJson(info, false)
            }
        }

        for (i in 0 until radioGroup.childCount) {
            radioGroup.getChildAt(i).isEnabled =
                accessMode != PartogramConstants.AccessMode.READ
        }
    }

    private fun clearOngoingSubViews(
        tempView: View,
        binding: PartoLblRadioViewOngoingComplicationBinding
    ) {

        binding.tvSelectedValue.text = ""

        val clOtherContainer =
            tempView.findViewById<ConstraintLayout>(R.id.clOtherContainer)

        val etOtherText =
            tempView.findViewById<EditText>(R.id.etOtherText)

        clOtherContainer?.visibility = View.GONE
        etOtherText?.setText("")
    }

    private fun persistOngoingJson(info: ParamInfo, isYes: Boolean) {
        try {
            val json = JSONObject()
            json.put("any ongoing complication", if (isYes) "yes" else "no")

            if (isYes) {

                val existing = info.capturedValue
                var complications = ""
                var otherValue = ""

                if (!existing.isNullOrEmpty()) {
                    try {
                        val existingJson = JSONObject(existing)
                        complications =
                            existingJson.optString("complications", "")
                        otherValue =
                            existingJson.optString("other value", "")
                    } catch (_: JSONException) {
                    }
                }

                json.put("complications", complications)

                if (otherValue.isNotEmpty()) {
                    json.put("other value", otherValue)
                }

            } else {
                json.put("complications", "")
            }

            info.capturedValue = json.toString()

        } catch (e: JSONException) {
            Log.e("OngoingComplication", "persist error", e)
        }
    }
}