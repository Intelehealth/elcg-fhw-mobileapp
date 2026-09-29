package org.intelehealth.ezazi.utilities

import org.intelehealth.ezazi.stage3.Utils.NepaliDateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
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
     * Renders a stored Gregorian date for display. Nepal renders Bikram Sambat; every other region
     * shows the stored string unchanged. Falls back to the stored string if it will not convert.
     */
    fun gregToDisplay(greg: String?): String {
        if (greg.isNullOrEmpty()) return ""
        if (!AppRegion.usesBikramSambat()) return greg
        val bs = NepaliDateUtils.gregStringToBs(greg) ?: return greg
        return NepaliDateUtils.formatBsDate(bs[0], bs[1], bs[2])
    }

    /**
     * Naegele's Rule as EZ-956 defines it: LMP + 7 days - 3 months + 1 year, applied in that order
     * because Calendar arithmetic is order-dependent at month ends. Empty string if LMP will not parse.
     */
    fun eddFromLmp(lmpGreg: String?): String {
        val millis = gregStringToMillis(lmpGreg) ?: return ""
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_MONTH, 7)
            add(Calendar.MONTH, -3)
            add(Calendar.YEAR, 1)
        }
        return utcFormat().format(cal.time)
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
