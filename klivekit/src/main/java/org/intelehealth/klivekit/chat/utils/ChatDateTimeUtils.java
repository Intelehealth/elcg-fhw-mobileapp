package org.intelehealth.klivekit.chat.utils;

import android.util.Log;

import org.intelehealth.klivekit.utils.DateTimeUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date handling for chat message timestamps only.
 *
 * <p>Kept separate from {@link DateTimeUtils} because the chat server is the only source that
 * sends two different shapes for the same field: RFC 1123 ({@link DateTimeUtils#DB_FORMAT}) on the
 * send response, and ISO 8601 ({@link #ISO_FORMAT}) over REST and the socket. The rest of the app
 * reads timestamps that have a single known format, so its behaviour is deliberately untouched.</p>
 *
 * <p>Parsing is pinned to {@code Locale.US} because the server's day and month names are English
 * and would otherwise fail to parse on a Hindi or Odia device. Display formatting is left on the
 * device locale so dates still render in the nurse's language.</p>
 *
 * <p>An unparseable timestamp yields an empty string rather than the current date. Substituting
 * "now" produced plausible-looking wrong times on every message loaded from history.</p>
 */
public final class ChatDateTimeUtils {
    private static final String TAG = "ChatDateTimeUtils";

    public static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private ChatDateTimeUtils() {
    }

    public static Date parseMessageDate(String date) {
        if (date == null) return null;

        Date parsed = tryParse(date, DateTimeUtils.DB_FORMAT);
        if (parsed == null) parsed = tryParse(date, ISO_FORMAT);
        if (parsed == null) {
            Log.w(TAG, "parseMessageDate: unparseable \"" + date + "\"");
        }
        return parsed;
    }

    public static String formatMessageDate(String date, String outFormat) {
        Date parsed = parseMessageDate(date);
        if (parsed == null) return "";
        return DateTimeUtils.formatToLocalDate(parsed, outFormat);
    }

    public static String displayDay(String date, String outFormat) {
        Date parsed = parseMessageDate(date);
        if (parsed == null) return "";
        if (DateTimeUtils.isToday(parsed)) return "Today";
        if (DateTimeUtils.isYesterday(parsed)) return "Yesterday";
        return DateTimeUtils.formatToLocalDate(parsed, outFormat);
    }

    private static Date tryParse(String date, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone(DateTimeUtils.TIME_ZONE_UTC));
            return sdf.parse(date);
        } catch (ParseException | NullPointerException e) {
            return null;
        }
    }
}
