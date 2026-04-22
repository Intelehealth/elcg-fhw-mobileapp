package org.intelehealth.ezazi.partogram.utils

import android.widget.RadioGroup
import android.widget.TextView
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.partogram.model.ParamInfo
import org.intelehealth.ezazi.utilities.UuidDictionary


class DataCaptureGenericRadioFieldHandler() {

    fun isGenericConcept(conceptUUID: String): Boolean {
        return !(UuidDictionary.IV_FLUIDS == conceptUUID ||
                UuidDictionary.OXYTOCIN_UL_DROPS_MIN == conceptUUID ||
                UuidDictionary.MEDICINE == conceptUUID ||
                UuidDictionary.PLAN == conceptUUID ||
                UuidDictionary.ASSESSMENT == conceptUUID)
    }

    fun normalizeRadioValue(value: String?): String? {
        if (value == null) return null

        return when {
            value.equals("Y", true) -> "YES"
            value.equals("N", true) -> "NO"
            else -> value.uppercase()
        }
    }

    fun restoreRadioState(group: RadioGroup, info: ParamInfo) {
        val raw = info.capturedValue
        val value = normalizeRadioValue(raw)

        group.setOnCheckedChangeListener(null)
        group.clearCheck()

        when (value) {
            "YES" -> group.check(R.id.radioYes)
            "NO" -> group.check(R.id.radioNo)
        }
    }

    fun syncRadioState(info: ParamInfo, group: RadioGroup, selected: TextView) {
        val value = info.capturedValue

        if (value == null) {
            group.check(R.id.radioNo)
            selected.text = "NO"
            return
        }

        if (value.equals("YES", true)) {
            group.check(R.id.radioYes)
            selected.text = "YES"
        } else {
            group.check(R.id.radioNo)
            selected.text = "NO"
        }
    }

}