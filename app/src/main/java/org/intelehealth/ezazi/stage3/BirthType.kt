package org.intelehealth.ezazi.stage3

enum class BirthType(val value: String) {
    LIVE_BIRTH("Live Birth"),
    STILLBIRTH("Stillbirth");

    companion object {
        fun from(value: String?): BirthType? {
            return values().find { it.value.equals(value, ignoreCase = true) }
        }
    }
}