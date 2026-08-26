package org.intelehealth.ezazi.activities.epartogramActivity.print

import android.app.Activity
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.intelehealth.ezazi.activities.epartogramActivity.EpartogramDataTransformer
import org.intelehealth.ezazi.utilities.WebViewPdfExporter
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the Labour Care Guide PDF for a real visit and saves it to
 * Downloads/eZazi.
 *
 * The data comes from [EpartogramDataTransformer], which reads the phone's own
 * database — so this works offline and needs neither the remote page nor a
 * WebView. Nothing here scales, fits or captures anything: the renderer draws
 * the sheets directly, so the output does not depend on what a browser or a
 * print service decides to do with it.
 */
object LcgPdfExport {

    private const val TAG = "LcgPdfExport"

    /** Default sheet. Print flows should pass the size the user actually chose. */
    @JvmStatic
    fun defaultSheet(): PageSpec = PageSpec.A4.portrait()

    interface Callback {
        fun onSuccess(uri: Uri, displayName: String)
        fun onFailure(message: String?)
    }

    /**
     * Renders the visit and reports back on the main thread. Database work and
     * file I/O both happen off it.
     */
    @JvmStatic
    fun export(
        activity: Activity,
        visitUuid: String,
        sheet: PageSpec,
        callback: Callback
    ) {
        Thread {
            var temp: File? = null
            try {
                val json = runBlocking { EpartogramDataTransformer.transform(visitUuid) }
                    ?: throw IOException("No labour care guide data for this visit")

                val data = JSONObject(json)
                Timber.tag(TAG).d(
                    "Rendering LCG for %s on %s: %s",
                    visitUuid, sheet.name, LcgSheetPreview.describe(data)
                )

                val displayName = "LCG_${visitUuid}_${timestamp()}.pdf"
                temp = File(activity.cacheDir, displayName)

                val document = PdfDocument()
                try {
                    LcgSheetRenderer.renderTo(document, data, sheet)
                    FileOutputStream(temp).use { document.writeTo(it) }
                } finally {
                    document.close()
                }

                val uri = WebViewPdfExporter.publishToDownloads(activity, temp, displayName)
                temp.delete()
                temp = null
                post(activity) { callback.onSuccess(uri, displayName) }
            } catch (oom: OutOfMemoryError) {
                Timber.tag(TAG).e("LCG render ran out of memory")
                temp?.delete()
                post(activity) { callback.onFailure("Out of memory") }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "LCG render failed")
                temp?.delete()
                post(activity) { callback.onFailure(e.message) }
            }
        }.start()
    }

    private fun post(activity: Activity, action: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread(action)
    }

    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
}
