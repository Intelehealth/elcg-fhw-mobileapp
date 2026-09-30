package org.intelehealth.ezazi.utilities

import org.intelehealth.ezazi.app.AppConstants

/**
 * The stored formats of the obstetric values, shared by the two writers: registration, which keys
 * them by attribute-type uuid into tbl_patient_attribute, and the Admission screen, which keys them
 * by concept uuid into obs. Only the key namespace differs; the formats must not.
 */
object ObstetricValueFormats {

    const val DOCTOR_SEPARATOR = "@#@"
    const val MEMBRANE_UNKNOWN = "U"
    const val MEMBRANE_INTACT = "I"
    const val OPTION_UNKNOWN = "Unknown"
    const val OPTION_INTACT = "Intact"
    const val OPTION_OTHER = "other"

    /** Comma, no space. */
    fun parity(births: String, miscarriages: String): String = "$births,$miscarriages"

    /** One separator space; the time carries its own trailing space. */
    fun timestamp(date: String, time: String): String = "$date $time"

    /** Case-sensitive, and Known is the else branch. */
    fun membraneRuptured(selected: String?, date: String, time: String): String = when (selected) {
        OPTION_UNKNOWN -> MEMBRANE_UNKNOWN
        OPTION_INTACT -> MEMBRANE_INTACT
        else -> timestamp(date, time)
    }

    /** The Other-High-Risk label is substituted with the typed reason. */
    fun riskFactors(riskFactors: String, otherLabel: String, otherText: String): String =
        if (riskFactors.contains(otherLabel)) riskFactors.replace(otherLabel, otherText) else riskFactors

    /** The literal "other" is never stored; the typed facility replaces it. */
    fun hospitalMaternity(selection: String, otherText: String): String =
        if (selection.trim().equals(OPTION_OTHER, ignoreCase = true)) otherText else selection

    fun doctor(uuid: String, name: String): String = uuid + DOCTOR_SEPARATOR + name

    /** Blank becomes "NA", not the empty string. */
    fun bedNumber(value: String): String = value.ifEmpty { AppConstants.NOT_APPLICABLE }
}
