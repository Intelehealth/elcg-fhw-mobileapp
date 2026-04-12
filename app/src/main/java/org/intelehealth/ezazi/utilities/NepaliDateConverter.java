package org.intelehealth.ezazi.utilities;

import java.util.Calendar;
import java.util.Date;

public class NepaliDateConverter {
    // ── Lookup table: BS 2000 → 2089 ─────────────────────────────────────────
    // Each entry: { daysInYear, d1, d2, ..., d12 } (months 1-12)
    // Source: widely-used public domain BS calendar data
    private static final int[][] BS_MONTHS_DATA = {
            // Year 2000
            {365, 30,32,31,32,31,30,30,30,29,30,29,31},
            // 2001
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2002
            {365, 31,31,32,32,31,30,30,29,30,29,30,30},
            // 2003
            {366, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2004
            {365, 30,32,31,32,31,30,30,30,29,30,29,31},
            // 2005
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2006
            {365, 31,31,32,32,31,30,30,29,30,29,30,30},
            // 2007
            {366, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2008
            {365, 31,31,31,32,31,31,29,30,30,29,29,31},
            // 2009
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2010
            {365, 31,31,32,32,31,30,30,29,30,29,30,30},
            // 2011
            {366, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2012
            {365, 31,31,31,32,31,31,29,30,30,29,30,30},
            // 2013
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2014
            {365, 31,31,32,32,31,30,30,29,30,29,30,30},
            // 2015
            {366, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2016
            {365, 31,31,31,32,31,31,29,30,30,29,30,30},
            // 2017
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2018
            {365, 31,32,31,32,31,30,30,29,30,29,30,30},
            // 2019
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2020
            {365, 30,31,31,32,31,31,30,29,30,29,30,30},
            // 2021
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2022
            {365, 31,32,31,32,31,30,30,30,29,29,30,30},
            // 2023
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2024
            {365, 30,31,31,32,31,31,30,29,30,29,30,30},
            // 2025
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2026
            {365, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2027
            {366, 30,32,31,32,31,30,30,30,29,30,29,31},
            // 2028
            {365, 30,31,32,31,31,30,30,29,30,29,30,30},
            // 2029
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
            // 2030
            {365, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2031
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2032
            {365, 30,31,32,31,31,30,30,29,30,29,30,30},
            // 2033
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
            // 2034
            {365, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2035
            {366, 31,32,31,32,31,31,29,30,30,29,29,31},
            // 2036
            {365, 30,31,31,32,31,30,30,29,30,29,30,30},
            // 2037
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
            // 2038
            {365, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2039
            {366, 31,32,31,32,31,31,30,29,30,29,30,30},
            // 2040
            {365, 31,31,31,32,31,31,30,29,30,29,30,30},
            // 2041
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2042
            {365, 31,32,31,32,31,30,30,30,29,29,30,30},
            // 2043
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2044
            {365, 30,31,31,32,31,31,30,29,30,29,30,30},
            // 2045
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2046
            {365, 31,32,31,32,31,30,30,30,29,29,30,30},
            // 2047
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2048
            {365, 30,31,32,31,31,30,30,29,30,29,30,30},
            // 2049
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
            // 2050 (index 50)
            {365, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2051
            {366, 31,32,31,32,31,31,29,30,30,29,30,30},
            // 2052
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2053
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2054
            {365, 31,32,31,32,31,30,30,30,29,29,30,30},
            // 2055
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2056
            {365, 30,31,32,31,31,30,30,29,30,29,30,30},
            // 2057
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
            // 2058
            {365, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2059
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2060
            {365, 30,31,32,31,31,30,30,29,30,29,30,30},
            // 2061
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
            // 2062
            {365, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2063
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2064
            {365, 30,32,31,32,31,30,30,29,30,29,30,30},
            // 2065
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2066
            {365, 31,31,32,32,31,30,30,29,30,29,30,30},
            // 2067
            {366, 31,32,31,32,31,30,30,30,29,29,30,31},
            // 2068
            {365, 31,31,31,32,31,31,29,30,30,29,29,31},
            // 2069
            {365, 31,31,32,31,31,31,30,29,30,29,30,30},
            // 2070 (index 70) — AD 2013 Apr 14
            {365, 31,32,31,32,31,30,30,30,29,29,30,30},
            // 2071
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2072
            {365, 30,31,32,31,31,30,30,29,30,29,30,30},
            // 2073
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
            // 2074
            {365, 31,32,31,32,31,30,30,30,29,29,30,30},
            // 2075
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2076
            {365, 30,31,32,31,31,30,30,29,30,29,30,30},
            // 2077
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
            // 2078
            {365, 31,32,31,32,31,30,30,30,29,29,30,30},
            // 2079
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2080
            {365, 30,31,32,31,31,30,30,29,30,29,30,30},
            // 2081
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
            // 2082
            {365, 31,32,31,32,31,30,30,30,29,29,30,30},
            // 2083
            {366, 31,32,31,32,31,30,30,30,29,30,29,31},
            // 2084
            {365, 30,31,32,31,31,30,30,29,30,29,30,30},
            // 2085
            {365, 31,31,32,31,31,30,30,29,30,29,30,30},
    };

    /** First BS year in our table */
    private static final int BS_YEAR_START = 2000;

    /**
     * Reference point: BS 2000 Baisakh 1 = AD 1943 April 14
     * We use this to count total days from BS epoch to any BS date,
     * then add to the Gregorian reference.
     */
    private static final int REF_AD_YEAR  = 1943;
    private static final int REF_AD_MONTH = 4;   // April (1-based)
    private static final int REF_AD_DAY   = 14;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Converts a BS date to a Gregorian {@link Date} (midnight local time).
     */
    public static Date bsToGregorian(int bsYear, int bsMonth, int bsDay) {
        // Count total days from BS 2000 Baisakh 1 to the given BS date
        long totalDays = 0;
        int yearIdx = bsYear - BS_YEAR_START;

        // Full years
        for (int y = 0; y < yearIdx; y++) {
            totalDays += BS_MONTHS_DATA[y][0]; // days in that year
        }
        // Full months in the target year
        for (int m = 1; m < bsMonth; m++) {
            totalDays += BS_MONTHS_DATA[yearIdx][m]; // index 1–12
        }
        // Remaining days
        totalDays += (bsDay - 1);

        // Add totalDays to the Gregorian reference date
        Calendar cal = Calendar.getInstance();
        cal.set(REF_AD_YEAR, REF_AD_MONTH - 1, REF_AD_DAY, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, (int) totalDays);
        return cal.getTime();
    }

    /**
     * Returns the number of days in a given BS year/month.
     */
    public static int getDaysInBsMonth(int bsYear, int bsMonth) {
        int idx = bsYear - BS_YEAR_START;
        if (idx < 0 || idx >= BS_MONTHS_DATA.length) return 30; // safe fallback
        return BS_MONTHS_DATA[idx][bsMonth]; // months stored at index 1–12
    }

    /**
     * Returns the current date in BS as int[]{year, month, day}.
     * Uses a simple reverse-count from the reference point.
     */
    public static int[] getCurrentBsDate() {
        return gregorianToBs(new Date());
    }

    /**
     * Converts a Gregorian {@link Date} to BS int[]{year, month, day}.
     */
    public static int[] gregorianToBs(Date gregorianDate) {
        Calendar ref = Calendar.getInstance();
        ref.set(REF_AD_YEAR, REF_AD_MONTH - 1, REF_AD_DAY, 0, 0, 0);
        ref.set(Calendar.MILLISECOND, 0);

        Calendar target = Calendar.getInstance();
        target.setTime(gregorianDate);
        // Normalise to start of day
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        long diffMs   = target.getTimeInMillis() - ref.getTimeInMillis();
        long diffDays = diffMs / (1000L * 60 * 60 * 24);

        int bsYear  = BS_YEAR_START;
        int bsMonth = 1;
        int bsDay   = 1;

        long remaining = diffDays;
        int yIdx = 0;
        while (remaining >= BS_MONTHS_DATA[yIdx][0]) {
            remaining -= BS_MONTHS_DATA[yIdx][0];
            bsYear++;
            yIdx++;
            if (yIdx >= BS_MONTHS_DATA.length) break;
        }

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
}