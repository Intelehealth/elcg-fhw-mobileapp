package org.intelehealth.ezazi.utilities

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object GregorianDateUtils {

    const val GREG_FMT = "dd/MM/yyyy"

    /**
     * Parses a stored dd/MM/yyyy value to epoch millis, or null when nothing is stored or it will not
     * parse. Never fabricates a date on failure.
     */
    fun gregStringToMillis(greg: String?): Long? {
        if (greg.isNullOrEmpty()) return null
        return try {
            utcFormat().parse(greg)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Pinned to UTC because NepaliDateConverter produces UTC midnight. On a device east of UTC an
     * unpinned formatter shifts the date forward and can roll it to the next day.
     */
    private fun utcFormat(): SimpleDateFormat =
        SimpleDateFormat(GREG_FMT, Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
}
