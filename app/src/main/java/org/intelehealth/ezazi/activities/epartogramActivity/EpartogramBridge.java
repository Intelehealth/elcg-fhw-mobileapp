package org.intelehealth.ezazi.activities.epartogramActivity;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.google.gson.Gson;

/**
 * JavaScript bridge exposed to the offline ePartogram WebView as {@code window.AndroidBridge}.
 *
 * Online path: the bridge is still attached but the remote Angular app does not call into it.
 * Offline path: the local Angular bundle (or the placeholder index.html shipped in
 * app/src/main/assets/epartogram/) calls {@link #getEpartogramData(String)} synchronously
 * and renders from the returned JSON.
 */
public class EpartogramBridge {

    public static final String NAME = "AndroidBridge";

    private static final String TAG = "EpartogramBridge";

    private final EpartogramRepository repository;
    private final Gson gson = new Gson();

    public EpartogramBridge(EpartogramRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the snapshot JSON for the given visit. Called synchronously from JS.
     * Any exception is logged and serialised as {@code {"error": "..."}} so the
     * WebView can surface a readable failure instead of a silent null.
     */
    @JavascriptInterface
    public String getEpartogramData(String visitUuid) {
        try {
            EpartogramSnapshot snapshot = repository.buildSnapshot(visitUuid);
            return gson.toJson(snapshot);
        } catch (Throwable t) {
            Log.e(TAG, "getEpartogramData failed for visit " + visitUuid, t);
            return "{\"error\":\"" + escape(t.getMessage()) + "\"}";
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
