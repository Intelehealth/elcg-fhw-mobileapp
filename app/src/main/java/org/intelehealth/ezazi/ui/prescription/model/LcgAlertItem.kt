package org.intelehealth.ezazi.ui.prescription.model

data class LcgAlertItem(
    val conceptId: String,            // stable identifier for DiffUtil
    val parameterName: String,          // e.g. "Fetal heart rate"
    val currentValueFormatted: String,  // e.g. "168 bpm"
    val alertThresholdFormatted: String, // e.g. "<110 or >160 bpm"
)