package org.intelehealth.ezazi.stage3.postpartum.print

import android.app.Activity
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.intelehealth.ezazi.activities.epartogramActivity.print.PageSpec
import org.intelehealth.ezazi.stage3.postpartum.Stage3DataTransformer
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
 * Builds the Delivery Outcome Report PDF for a real visit and saves it to
 * Downloads/eZazi.
 *
 * Data comes from [Stage3DataTransformer], which reads the phone's own
 * database — so this works offline and needs neither the remote page nor a
 * WebView. The renderer draws the sheet directly, so nothing downstream gets to
 * scale, fit or re-flow it.
 */
object Stage3PdfExport {

    private const val TAG = "Stage3PdfExport"

    /** Default sheet. Print flows should pass the size the user actually chose. */
    @JvmStatic
    fun defaultSheet(): PageSpec = PageSpec.A4.portrait()

    interface Callback {
        fun onSuccess(uri: Uri, displayName: String)
        fun onFailure(message: String?)
    }

    /**
     * Renders the report and reports back on the main thread. Database work and
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
                val json = runBlocking { Stage3DataTransformer.transform(visitUuid) }
                    ?: throw IOException("No delivery outcome data for this visit")

                val data = JSONObject(json)
                Timber.tag(TAG).d(
                    "Rendering delivery outcome for %s on %s: maternal=%d newborn=%d slots=%d",
                    visitUuid, sheet.name,
                    data.optJSONArray("maternalParams")?.length() ?: 0,
                    data.optJSONArray("newbornParams")?.length() ?: 0,
                    data.optJSONArray("colTimes")?.length() ?: 0
                )

                val displayName = "DeliveryOutcome_${visitUuid}_${timestamp()}.pdf"
                temp = File(activity.cacheDir, displayName)

                val document = PdfDocument()
                try {
                    Stage3SheetRenderer.renderTo(document, data, sheet)
                    FileOutputStream(temp).use { document.writeTo(it) }
                } finally {
                    document.close()
                }

                val uri = WebViewPdfExporter.publishToDownloads(activity, temp, displayName)
                temp.delete()
                temp = null
                post(activity) { callback.onSuccess(uri, displayName) }
            } catch (oom: OutOfMemoryError) {
                Timber.tag(TAG).e("Delivery outcome render ran out of memory")
                temp?.delete()
                post(activity) { callback.onFailure("Out of memory") }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Delivery outcome render failed")
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
