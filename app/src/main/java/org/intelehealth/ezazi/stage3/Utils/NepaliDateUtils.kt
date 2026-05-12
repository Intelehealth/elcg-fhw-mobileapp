package org.intelehealth.ezazi.stage3.Utils

import android.util.Log
import android.widget.NumberPicker
import androidx.annotation.Nullable
import org.intelehealth.ezazi.utilities.NepaliDateConverter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


object NepaliDateUtils {

    private const val GREG_FMT = "dd/MM/yyyy"

    val BS_MONTH_NAMES = arrayOf(
        "Baisakh", "Jestha", "Asar", "Shrawan",
        "Bhadra", "Ashwin", "Kartik", "Mangsir",
        "Poush", "Magh", "Falgun", "Chaitra"
    )

    // ─────────────────────────────────────────────
    // Listener
    // ─────────────────────────────────────────────
    fun interface OnBsDateSelectedListener {
        fun onSelected(year: Int, month: Int, day: Int)
    }

    // ─────────────────────────────────────────────
    // Day refresh
    // ─────────────────────────────────────────────
    fun refreshDayPicker(
        dp: NumberPicker,
        bsYear: Int,
        bsMonth: Int
    ) {
        val maxDay = NepaliDateConverter.getDaysInBsMonth(bsYear, bsMonth)
        dp.minValue = 1
        dp.maxValue = maxDay

        if (dp.value > maxDay) {
            dp.value = maxDay
        }
    }

    // ─────────────────────────────────────────────
    // Gregorian → BS
    // ─────────────────────────────────────────────
    fun gregStringToBs(gregDdMmYyyy: String?): IntArray? {
        if (gregDdMmYyyy.isNullOrEmpty()) return null

        return try {
            val sdf = SimpleDateFormat(GREG_FMT, Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(gregDdMmYyyy)
            NepaliDateConverter.gregorianToBs(date)
        } catch (e: Exception) {
            null
        }
    }

    // ─────────────────────────────────────────────
    // Format BS date for UI
    // ─────────────────────────────────────────────
    fun formatBsDate(y: Int, m: Int, d: Int): String {
        return String.format(
            Locale.ENGLISH,
            "%02d-%s-%d",
            d,
            BS_MONTH_NAMES[m - 1],
            y
        )
    }

    // ─────────────────────────────────────────────
    // Parse Gregorian Date + Time
    // ─────────────────────────────────────────────
    fun parseGregDateTime(
        dateStr: String?,
        timeStr: String?
    ): Date? {

        if (dateStr.isNullOrEmpty() || timeStr.isNullOrEmpty()) {
            return null
        }

        val combined = "${dateStr.trim()} ${timeStr.trim()}"

        val formats = arrayOf(
            "dd/MM/yyyy hh:mm a",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy hh:mm"
        )

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                sdf.isLenient = false
                val parsed = sdf.parse(combined)
                if (parsed != null) return parsed
            } catch (_: Exception) {
            }
        }

        Log.e("NepaliDateUtils", "Could not parse '$combined'")
        return null
    }
     fun isAfterToday(gregDateStr: String?): Boolean {
        val parsed: Date = parseGregDate(gregDateStr) ?: return false

        // Strip time from both sides — compare dates only
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val sel = Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return sel.after(today)
    }
    @Nullable
    private fun parseGregDate(dateStr: String?): Date? {
        if (dateStr == null || dateStr.isEmpty()) return null
        return try {
            val sdf = SimpleDateFormat(GREG_FMT, Locale.ENGLISH)
            sdf.isLenient = false
            sdf.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
    @JvmStatic
    fun toGregFmt(date: Date): String {
        val sdf = SimpleDateFormat(GREG_FMT, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }
}
