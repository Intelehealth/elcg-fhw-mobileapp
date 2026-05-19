package org.intelehealth.ezazi.activities.homeActivity.riskscores

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun String?.toMillis(): Long {
    if (this.isNullOrBlank()) return 0L

    val formatter = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.US
    )
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.parse(this)?.time ?: 0L
}