package org.intelehealth.ezazi.utilities;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Converts between Bikram Sambat (BS) and Gregorian dates.
 *
 * ── FIX (Bug #1 – "30 Chaitra 2082 shows as 1 Baisakh 2083") ───────────────
 * Root cause: both bsToGregorian() and gregorianToBs() used
 * Calendar.getInstance() which inherits the device's LOCAL timezone.
 * In Nepal (UTC+5:45), DST-unaware integer division of diffMs by 86_400_000
 * could truncate one day on certain dates, making the round-trip lossy:
 *
 *   bsToGregorian(2082, 12, 30)  →  Gregorian D  (correct)
 *   gregorianToBs(D)             →  2083-01-01   (off by one ← BUG)
 *
 * Fix: pin ALL calendar operations to UTC so there are no timezone offsets
 * or DST transitions to corrupt the millisecond arithmetic.
 * ────────────────────────────────────────────────────────────────────────────
 */
public class NepaliDateConverter {

    // ── Lookup table: BS 2000 → 2086 (87 rows; derive the bound, never hardcode it) ────────
    // Each row: { totalDaysInYear, daysInMonth1 … daysInMonth12 }
    private static final int[][] BS_MONTHS_DATA = {
            {365, 30,32,31,32,31,30,30,30,29,30,29,31}, // 2000
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2001
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2002
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2003
            {365, 30,32,31,32,31,30,30,30,29,30,29,31}, // 2004
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2005
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2006
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2007
            {365, 31,31,31,32,31,31,29,30,30,29,29,31}, // 2008
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2009
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2010
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2011
            {365, 31,31,31,32,31,31,29,30,30,29,30,30}, // 2012
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2013
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2014
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2015
            {365, 31,31,31,32,31,31,29,30,30,29,30,30}, // 2016
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2017
            {365, 31,32,31,32,31,30,30,29,30,29,30,30}, // 2018
            {366, 31,32,31,32,31,30,30,30,29,30,29,31}, // 2019
            {365, 31,31,31,32,31,31,30,29,30,29,30,30}, // 2020
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2021
            {365, 31,32,31,32,31,30,30,30,29,29,30,30}, // 2022
            {366, 31,32,31,32,31,30,30,30,29,30,29,31}, // 2023
            {365, 31,31,31,32,31,31,30,29,30,29,30,30}, // 2024
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2025
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2026
            {365, 30,32,31,32,31,30,30,30,29,30,29,31}, // 2027
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2028
            {365, 31,31,32,31,32,30,30,29,30,29,30,30}, // 2029
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2030
            {365, 30,32,31,32,31,30,30,30,29,30,29,31}, // 2031
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2032
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2033
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2034
            {365, 30,32,31,32,31,31,29,30,30,29,29,31}, // 2035
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2036
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2037
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2038
            {365, 31,31,31,32,31,31,29,30,30,29,30,30}, // 2039
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2040
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2041
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2042
            {365, 31,31,31,32,31,31,29,30,30,29,30,30}, // 2043
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2044
            {365, 31,32,31,32,31,30,30,29,30,29,30,30}, // 2045
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2046
            {365, 31,31,31,32,31,31,30,29,30,29,30,30}, // 2047
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2048
            {365, 31,32,31,32,31,30,30,30,29,29,30,30}, // 2049
            {366, 31,32,31,32,31,30,30,30,29,30,29,31}, // 2050
            {365, 31,31,31,32,31,31,30,29,30,29,30,30}, // 2051
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2052
            {365, 31,32,31,32,31,30,30,30,29,29,30,30}, // 2053
            {366, 31,32,31,32,31,30,30,30,29,30,29,31}, // 2054
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2055
            {365, 31,31,32,31,32,30,30,29,30,29,30,30}, // 2056
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2057
            {365, 30,32,31,32,31,30,30,30,29,30,29,31}, // 2058
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2059
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2060
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2061
            {365, 30,32,31,32,31,31,29,30,29,30,29,31}, // 2062
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2063
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2064
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2065
            {365, 31,31,31,32,31,31,29,30,30,29,29,31}, // 2066
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2067
            {365, 31,31,32,32,31,30,30,29,30,29,30,30}, // 2068
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2069
            {365, 31,31,31,32,31,31,29,30,30,29,30,30}, // 2070
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2071
            {365, 31,32,31,32,31,30,30,29,30,29,30,30}, // 2072
            {366, 31,32,31,32,31,30,30,30,29,29,30,31}, // 2073
            {365, 31,31,31,32,31,31,30,29,30,29,30,30}, // 2074
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2075
            {365, 31,32,31,32,31,30,30,30,29,29,30,30}, // 2076
            {366, 31,32,31,32,31,30,30,30,29,30,29,31}, // 2077
            {365, 31,31,31,32,31,31,30,29,30,29,30,30}, // 2078
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2079
            {365, 31,32,31,32,31,30,30,30,29,29,30,30}, // 2080
            {366, 31,32,31,32,31,30,30,30,29,30,29,31}, // 2081
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2082
            {365, 31,31,32,31,31,31,30,29,30,29,30,30}, // 2083
            {365, 31,31,32,31,31,30,30,30,29,30,30,30}, // 2084
            {366, 31,32,31,32,30,31,30,30,29,30,30,30}, // 2085
            {365, 30,32,31,32,31,30,30,30,29,30,30,30}, // 2086
    };

    private static final int BS_YEAR_START = 2000;

    /**
     * The last BS year this converter can represent, derived from the lookup table so it can never
     * drift from it. Any UI offering a year beyond this would index past the table.
     */
    public static int getMaxSupportedBsYear() {
        return BS_YEAR_START + BS_MONTHS_DATA.length - 1;
    }

    public static int getMinSupportedBsYear() {
        return BS_YEAR_START;
    }

    // Reference epoch: BS 2000 Baisakh 1 = AD 1943 April 14
    private static final int REF_AD_YEAR  = 1943;
    private static final int REF_AD_MONTH = 4;  // April (1-based)
    private static final int REF_AD_DAY   = 14;

    /**
     * The Gregorian rendering used by every region that does not use Bikram Sambat. Deliberately a
     * single constant so all three display entry points agree, and pinned to ENGLISH so a device
     * locale with a non-Latin numbering system cannot emit non-ASCII digits.
     */
    private static final String GREGORIAN_DISPLAY_FORMAT = "dd MMM yyyy";

    private static final String[] BS_DISPLAY_MONTHS = {
            "Baisakh", "Jestha", "Asar", "Shrawan",
            "Bhadra", "Ashwin", "Kartik", "Mangsir",
            "Poush", "Magh", "Falgun", "Chaitra"
    };

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a UTC-pinned Calendar set to midnight of the given date components.
     * Pinning to UTC eliminates DST offsets that corrupt day-count arithmetic.
     */
    private static Calendar utcMidnight(int year, int month0, int day) {
        // month0 is 0-based (Calendar convention)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month0, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /** Strips time component from any Date, returning UTC midnight of that day. */
    private static Calendar utcMidnightOf(Date date) {
        Calendar tmp = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        tmp.setTime(date);
        tmp.set(Calendar.HOUR_OF_DAY, 0);
        tmp.set(Calendar.MINUTE,      0);
        tmp.set(Calendar.SECOND,      0);
        tmp.set(Calendar.MILLISECOND, 0);
        return tmp;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Converts a BS date to a Gregorian {@link Date} (UTC midnight).
     *
     * FIXED: reference Calendar and all arithmetic now use UTC,
     * preventing DST-driven off-by-one errors on devices in non-UTC zones
     * (e.g. Nepal UTC+5:45).
     */
    /**
     * Converts a BS date to Gregorian, or returns null when the year falls outside the lookup table.
     *
     * <p>Returning null rather than throwing matters: the table has a hard end, and every caller
     * already has to cope with an unconvertible date. Indexing past it previously raised an
     * uncaught ArrayIndexOutOfBoundsException from inside a picker callback.
     */
    public static Date bsToGregorian(int bsYear, int bsMonth, int bsDay) {
        long totalDays = 0;
        int yearIdx = bsYear - BS_YEAR_START;
        if (yearIdx < 0 || yearIdx >= BS_MONTHS_DATA.length || bsMonth < 1 || bsMonth > 12) {
            return null;
        }

        for (int y = 0; y < yearIdx; y++) {
            totalDays += BS_MONTHS_DATA[y][0];
        }
        for (int m = 1; m < bsMonth; m++) {
            totalDays += BS_MONTHS_DATA[yearIdx][m];
        }
        totalDays += (bsDay - 1);

        // ── FIX: use UTC reference so timezone/DST never shifts the day ──────
        Calendar cal = utcMidnight(REF_AD_YEAR, REF_AD_MONTH - 1, REF_AD_DAY);
        cal.add(Calendar.DAY_OF_MONTH, (int) totalDays); // DAY_OF_MONTH is unambiguous
        return cal.getTime();
    }

    /**
     * Returns the number of days in a given BS year/month.
     */
    public static int getDaysInBsMonth(int bsYear, int bsMonth) {
        int idx = bsYear - BS_YEAR_START;
        if (idx < 0 || idx >= BS_MONTHS_DATA.length) return 30;
        return BS_MONTHS_DATA[idx][bsMonth];
    }

    /** Returns today's date in BS as int[]{year, month, day}. */
    public static int[] getCurrentBsDate() {
        return gregorianToBs(new Date());
    }

    /**
     * Converts a Gregorian {@link Date} to BS int[]{year, month, day}.
     *
     * FIXED: both the reference and the target are normalised to UTC midnight
     * before the millisecond difference is computed. This guarantees the
     * integer day-count is always exact, even on devices where the local
     * timezone (e.g. Nepal UTC+5:45) would otherwise cause a truncation error.
     */
    public static int[] gregorianToBs(Date gregorianDate) {
        // ── FIX: pin both reference and target to UTC midnight ────────────────
        Calendar ref    = utcMidnight(REF_AD_YEAR, REF_AD_MONTH - 1, REF_AD_DAY);
        Calendar target = utcMidnightOf(gregorianDate);

        long diffMs   = target.getTimeInMillis() - ref.getTimeInMillis();
        long diffDays = diffMs / (1000L * 60 * 60 * 24); // exact because both are UTC midnight

        int bsYear  = BS_YEAR_START;
        int bsMonth = 1;
        int bsDay   = 1;
        long remaining = diffDays;
        int yIdx = 0;

        while (yIdx < BS_MONTHS_DATA.length && remaining >= BS_MONTHS_DATA[yIdx][0]) {
            remaining -= BS_MONTHS_DATA[yIdx][0];
            bsYear++;
            yIdx++;
        }

        // The table ends at BS 2086 (AD 2030-04-13). Past that yIdx is out of range and
        // the month loop below would throw. Returning null lets callers fall back to a
        // Gregorian date; without this one out-of-range LMP or EDD took down every
        // report on all four surfaces, because the conversion happens inside buildPinfo.
        if (yIdx >= BS_MONTHS_DATA.length) return null;

        for (int m = 1; m <= 12; m++) {
            int daysInMonth = BS_MONTHS_DATA[yIdx][m];
            if (remaining < daysInMonth) {
                bsMonth = m;
                bsDay   = (int) remaining + 1;
                break;
            }
            remaining -= daysInMonth;
        }

        return new int[]{bsYear, bsMonth, bsDay};
    }

    /**
     * Converts a {@link Date} (any timezone — normalised to UTC midnight internally)
     * to a Nepali BS display string: "DD MonthName YYYY BS"
     * e.g. {@code Date} representing 2024-06-28 → "15 Asar 2081 BS"
     *
     * Use this wherever a {@code java.util.Date} is already available
     * (e.g. after {@code DateTimeUtils.utcToLocalDate()}).
     *
     * @param date Gregorian date to convert; returns empty string if null.
     */
    /**
     * Renders a date that has ALREADY been reduced to the intended calendar day, expressed as UTC
     * midnight. This is the contract both PDF renderers honour, and the one
     * {@link #localDayToBsDisplay(String)} produces.
     *
     * <p>For a raw instant that has not been reduced, call {@link #instantToBsDisplay(Date)} instead.
     * Passing a raw instant here reads the UTC calendar day, which is the previous day for any
     * local time before the UTC offset on a positive-offset device such as Nepal (UTC+5:45).
     *
     * <p>Outside Nepal this formats in UTC deliberately: the argument is UTC midnight, so a
     * device-local formatter would shift the rendered day backwards on any negative-offset device.
     */
    public static String dateToBsDisplay(Date date) {
        if (date == null) return "";
        if (!AppRegion.usesBikramSambat()) {
            SimpleDateFormat sdf = new SimpleDateFormat(GREGORIAN_DISPLAY_FORMAT, Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format(date);
        }
        int[] bs = gregorianToBs(date);
        if (bs == null) {
            SimpleDateFormat sdf = new SimpleDateFormat(GREGORIAN_DISPLAY_FORMAT, Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format(date);
        }
        return bs[2] + " " + BS_DISPLAY_MONTHS[bs[1] - 1] + " " + bs[0] + " BS";
    }

    /**
     * Renders a raw instant, reducing it to the calendar day the device is currently in before
     * converting. Use this whenever the argument is a timestamp rather than an already-reduced day.
     */
    public static String instantToBsDisplay(Date instant) {
        if (instant == null) return "";
        Calendar local = Calendar.getInstance();
        local.setTime(instant);
        Calendar day = utcMidnight(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH));
        return dateToBsDisplay(day.getTime());
    }

    /**
     * Every Gregorian shape this project's transformers emit, with the timezone the
     * shape implies. Instants carry an explicit UTC marker or come from the DB in
     * UTC; bare calendar dates are already local wall-clock dates.
     */
    private static final String[][] ANY_GREGORIAN_FORMATS = {
            {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC"},
            {"yyyy-MM-dd'T'HH:mm:ss'Z'",     "UTC"},
            {"yyyy-MM-dd HH:mm:ss",          "UTC"},
            {"dd/MM/yyyy hh:mm a",           "LOCAL"},
            {"dd/MM/yyyy HH:mm",             "LOCAL"},
            {"dd/MM/yyyy",                   "LOCAL"},
            {"yyyy-MM-dd",                   "LOCAL"},
    };

    /**
     * Parses any of {@link #ANY_GREGORIAN_FORMATS}, requiring the WHOLE string to be
     * consumed.
     *
     * The full-consumption check is the point. SimpleDateFormat.parse(String) stops as
     * soon as its pattern is satisfied and ignores the rest, so "dd/MM/yyyy" happily
     * eats "28/06/2024 14:30" and silently discards the clock. Insisting the parse
     * position reach the end makes the order of this table irrelevant and stops a
     * loose pattern claiming a string that belongs to a stricter one.
     *
     * @return the parsed instant, or null if nothing matched exactly.
     */
    private static Date parseAnyGregorian(String raw) {
        for (String[] entry : ANY_GREGORIAN_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(entry[0], Locale.ENGLISH);
                sdf.setLenient(false);
                if ("UTC".equals(entry[1])) {
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                ParsePosition pos = new ParsePosition(0);
                Date parsed = sdf.parse(raw, pos);
                if (parsed != null && pos.getIndex() == raw.length()) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * BS display date for the LOCAL calendar day of a Gregorian timestamp string,
     * e.g. "2024-06-28T09:15:00.000Z" or "28/06/2024" to "15 Asar 2081 BS".
     *
     * This exists because the offline WebView screens cannot do the conversion
     * themselves: epartogram.html carries no Bikram Sambat code at all, and the table
     * stage3.html shipped both ran out in April 2025 and disagreed with the table here
     * on most of its rows. Rather than maintain a second calendar in JavaScript, the
     * transformers call this and inject the finished string alongside the raw value —
     * the asset renders this on its date line and keeps using the raw instant for its
     * clock line, which is why the two are emitted as separate keys.
     *
     * Reducing to the local day first is required: {@link #gregorianToBs} counts whole
     * days from UTC midnight, so anything after 18:15 UTC would otherwise resolve to
     * the previous day in Nepal.
     *
     * @param raw a Gregorian date or timestamp in any supported shape.
     * @return the BS display string, or "" when raw is empty or unparseable.
     */
    public static String localDayToBsDisplay(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        Date instant = parseAnyGregorian(raw.trim());
        if (instant == null) return "";

        Calendar local = Calendar.getInstance();
        local.setTime(instant);
        Calendar day = utcMidnight(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH));
        return dateToBsDisplay(day.getTime());
    }

    /**
     * Converts a Gregorian date <em>string</em> in any of the common formats used
     * in this project to a Nepali BS display string: "DD MonthName YYYY BS"
     *
     * Supported input formats (tried in order):
     * <ul>
     *   <li>{@code yyyy-MM-dd}          — DB storage format for DOB / admission date</li>
     *   <li>{@code dd/MM/yyyy}          — PatientOtherInfoFragment storage format</li>
     *   <li>{@code dd MMM yyyy}         — Formatted display strings (e.g. from DateAndTimeUtils)</li>
     *   <li>{@code yyyy-MM-dd HH:mm:ss} — Full datetime strings from tbl_visit.startdate</li>
     * </ul>
     *
     * Returns an empty string if the input is null/empty or cannot be parsed.
     *
     * @param gregorianDateStr Gregorian date string in one of the formats above.
     */
    public static String gregStringToBsDisplay(String gregorianDateStr) {
        if (gregorianDateStr == null || gregorianDateStr.trim().isEmpty()) return "";
        if (!AppRegion.usesBikramSambat()) return gregorianDateStr;

        // Each entry: { parse-format, time-output-format-or-null }
        // time-output-format is non-null only for formats that contain a time component.
        //
        // ORDER IS LOAD-BEARING. SimpleDateFormat.parse(String) stops as soon as the
        // pattern is satisfied and ignores whatever trails it, so a date-only pattern
        // happily consumes "28 Jun 2024 14:30" and silently drops the clock. Every
        // time-bearing format must therefore be tried before the date-only ones; a
        // date-only input simply misses them and falls through.
        String[][] formats = {
                {"yyyy-MM-dd'T'HH:mm:ss.SSSZ",  "HH:mm:ss"},
                {"yyyy-MM-dd HH:mm:ss",         "HH:mm:ss"},
                {"dd MMM, yyyy hh:mm a",        "hh:mm a"},   // "15 Apr, 2026 06:21 PM"
                {"dd MMM yyyy hh:mm a",         "hh:mm a"},   // "15 Apr 2026 06:21 PM"
                {"dd MMM yyyy HH:mm",           "HH:mm"},     // "28 Jun 2024 14:30"
                {"yyyy-MM-dd",                  null},
                {"dd/MM/yyyy",                  null},
                {"dd MMM yyyy",                 null},
                {"dd MMM, yyyy",                null},
        };

        for (String[] entry : formats) {
            String parseFmt   = entry[0];
            String timeFmt    = entry[1]; // null = date-only format, no time to preserve
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(parseFmt, Locale.ENGLISH);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                sdf.setLenient(false);
                Date parsed = sdf.parse(gregorianDateStr.trim());
                if (parsed == null) continue;

                // Convert date portion to BS
                String bsDate = dateToBsDisplay(parsed);

                // If this format has a time component, extract and append it
                if (timeFmt != null) {
                    SimpleDateFormat timeSdf = new SimpleDateFormat(timeFmt, Locale.ENGLISH);
                    timeSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String timeStr = timeSdf.format(parsed);
                    return bsDate + " " + timeStr;
                }

                return bsDate;
            } catch (Exception ignored) {}
        }

        // Graceful fallback: return original string rather than crashing
        return gregorianDateStr;
    }
}