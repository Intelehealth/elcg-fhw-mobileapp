package org.intelehealth.ezazi.stage3.Utils

data class ValidationResult(
    val isValid: Boolean,
    val field: Field? = null,
    val messageRes: Int? = null
)

enum class Field {
    DATE_OF_DELIVERY,
    TIME_OF_DELIVERY,
    MODE_OTHER,
    DEGREE_OF_TEAR,
    PLACENTA_STATUS,
    PLACENTA_TIME,
    AMTSL_OTHER,
    TYPE_OF_BIRTH,
    BABY_GENDER,
    APGAR1,
    APGAR5,
    BIRTH_WEIGHT,
    CONGENITAL_OPTION,
    CONGENITAL_OTHER
}