package org.intelehealth.ezazi.utilities

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.WebView
import androidx.core.content.FileProvider
import org.intelehealth.ezazi.BuildConfig
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Exports a WebView's FULL document (including content hidden inside nested
 * horizontally/vertically scrolling containers) as a single-page PDF saved to
 * public Downloads/eZazi.
 *
 * Also exposes [expandAndMeasure] / [restorePageStyles] so print flows can
 * reuse the same in-page expansion logic (see EpartogramViewActivity).
 *
 * Requirements:
 *  - WebView.enableSlowWholeDocumentDraw() must be called in the Application
 *    class before any WebView is created (already done in IntelehealthApplication).
 *  - Pre-Android 10: caller must hold WRITE_EXTERNAL_STORAGE before calling.
 *
 * Used by: EpartogramViewActivity (download + print), ViewPostPartumReportActivity (download).
 */
object WebViewPdfExporter {

    private const val TAG = "WebViewPdfExporter"
    private const val RELAYOUT_DELAY_MS = 300L
    private const val WIDTH_BUFFER = 1.03

    interface Callback {
        fun onSuccess(uri: Uri, displayName: String)
        fun onFailure(message: String?)
    }

    /** Java-friendly callback for [expandAndMeasure]. */
    fun interface WidthCallback {
        fun onMeasured(cssWidth: Int)
    }

    /**
     * Expands any scrolling containers so hidden columns get painted, and
     * returns the true full content width. Original styles are stashed on the
     * page (window.__ezSaved) so they can be restored after capture.
     */
    private const val JS_EXPAND_AND_MEASURE =
        "(function(){" +
                "  window.__ezSaved = [];" +
                "  function save(el){ window.__ezSaved.push([el, el.style.cssText]); }" +
                "  var all = document.querySelectorAll('*');" +
                "  for (var i = 0; i < all.length; i++) {" +
                "    var el = all[i];" +
                "    if (el.scrollWidth > el.clientWidth + 1) {" +
                "      save(el);" +
                "      el.style.overflow = 'visible';" +
                "      el.style.width = 'max-content';" +
                "      el.style.maxWidth = 'none';" +
                "    }" +
                "  }" +
                "  save(document.documentElement);" +
                "  save(document.body);" +
                "  document.documentElement.style.width = 'max-content';" +
                "  document.body.style.width = 'max-content';" +
                "  return Math.ceil(Math.max(document.documentElement.scrollWidth," +
                "                            document.body.scrollWidth));" +
                "})()"

    /** Puts every modified element back exactly as it was. */
    private const val JS_RESTORE_STYLES =
        "(function(){" +
                "  if (window.__ezSaved) {" +
                "    for (var i = 0; i < window.__ezSaved.length; i++) {" +
                "      window.__ezSaved[i][0].style.cssText = window.__ezSaved[i][1];" +
                "    }" +
                "    window.__ezSaved = null;" +
                "  }" +
                "})()"

    // region ---- Public API ----

    /**
     * Full export: expand -> capture -> save to Downloads -> restore.
     * Must be called on the main thread with a fully loaded page.
     * The callback is invoked on the main thread.
     */
    @JvmStatic
    fun export(activity: Activity, webView: WebView, displayName: String, callback: Callback) {
        expandAndMeasure(webView) { cssWidth ->
            capture(activity, webView, cssWidth, displayName, callback)
        }
    }

    /**
     * Expands the page's scroll containers and reports the true full content
     * width (CSS px) after the page has re-laid out. Callers using this
     * directly (e.g. print flows) MUST call [restorePageStyles] afterwards.
     */
    @JvmStatic
    fun expandAndMeasure(webView: WebView, callback: WidthCallback) {
        webView.evaluateJavascript(JS_EXPAND_AND_MEASURE) { value ->
            val cssWidth = value?.replace("\"", "")?.trim()?.toIntOrNull() ?: 0
            // Give the page a moment to re-layout after the style changes
            webView.postDelayed({ callback.onMeasured(cssWidth) }, RELAYOUT_DELAY_MS)
        }
    }

    /** Undoes [expandAndMeasure]'s style changes (and any body zoom applied after it). */
    @JvmStatic
    fun restorePageStyles(webView: WebView) {
        webView.evaluateJavascript(JS_RESTORE_STYLES, null)
        webView.invalidate()
    }

    // endregion

    // region ---- Capture ----

    @Suppress("DEPRECATION") // webView.scale is the honest CSS-px -> device-px factor
    private fun capture(
        activity: Activity,
        webView: WebView,
        cssWidth: Int,
        displayName: String,
        callback: Callback
    ) {
        val tempFile = File(activity.cacheDir, displayName)

        val originalWidth = webView.width
        val originalHeight = webView.height
        val originalScrollX = webView.scrollX
        val originalScrollY = webView.scrollY
        val originalLayerType = webView.layerType

        fun restore() = restoreWebView(
            webView, originalWidth, originalHeight,
            originalScrollX, originalScrollY, originalLayerType
        )

        try {
            // CSS px -> device px; never narrower than the screen,
            // small buffer for absolutely-positioned elements at the right edge
            val contentWidthPx = Math.ceil(cssWidth * webView.scale.toDouble()).toInt()
            val captureWidth =
                (maxOf(originalWidth, contentWidthPx) * WIDTH_BUFFER).toInt()

            // Capture must start from the top-left of the document
            webView.scrollTo(0, 0)

            // Software rendering is required for full-document capture
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            // Lay the WebView out at FULL content width and height
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(captureWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val contentWidth = webView.measuredWidth
            val contentHeight = webView.measuredHeight
            webView.layout(0, 0, contentWidth, contentHeight)

            // Draw the entire document onto ONE page sized to the content
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo
                .Builder(contentWidth, contentHeight, 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawColor(Color.WHITE)
            webView.draw(page.canvas)
            document.finishPage(page)

            try {
                FileOutputStream(tempFile).use { document.writeTo(it) }
            } finally {
                document.close()
            }

            // Restore the WebView to its normal on-screen state
            restore()

            // Move to public Downloads and report back
            val savedUri = savePdfToDownloads(activity, tempFile, displayName)
            tempFile.delete()
            callback.onSuccess(savedUri, displayName)

        } catch (oom: OutOfMemoryError) {
            Timber.tag(TAG).e("PDF generation OOM — record too long for device memory")
            restore()
            tempFile.delete()
            callback.onFailure("Out of memory")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Single-page PDF generation failed")
            restore()
            tempFile.delete()
            callback.onFailure(e.message)
        }
    }

    /** Returns the WebView (and the page's CSS) to normal after a capture. */
    private fun restoreWebView(
        webView: WebView,
        width: Int, height: Int,
        scrollX: Int, scrollY: Int,
        layerType: Int
    ) {
        restorePageStyles(webView)
        webView.setLayerType(layerType, null)
        webView.layout(0, 0, width, height)
        webView.scrollTo(scrollX, scrollY)
        webView.invalidate()
        webView.requestLayout()
    }

    // endregion

    // region ---- Save to Downloads ----

    /**
     * Copies the generated PDF into public Downloads/eZazi.
     * Android 10+ uses MediaStore (no permission needed); older versions
     * write directly to external storage (permission is the caller's job).
     */
    @Throws(IOException::class)
    private fun savePdfToDownloads(context: Context, pdfFile: File, displayName: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + File.separator + "eZazi"
                )
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("MediaStore insert returned null")

            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(pdfFile).use { input -> copyStream(input, out) }
            } ?: throw IOException("Could not open output stream")
            uri
        } else {
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "eZazi"
            )
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                throw IOException("Could not create Downloads/eZazi directory")
            }
            val dest = File(downloadsDir, displayName)
            FileInputStream(pdfFile).use { input ->
                FileOutputStream(dest).use { out -> copyStream(input, out) }
            }
            // NOTE: authority must match the <provider> entry in AndroidManifest.xml
            FileProvider.getUriForFile(
                context, BuildConfig.APPLICATION_ID + ".provider", dest
            )
        }
    }

    private fun copyStream(input: InputStream, out: OutputStream) {
        val buffer = ByteArray(8192)
        var length: Int
        while (input.read(buffer).also { length = it } > 0) {
            out.write(buffer, 0, length)
        }
        out.flush()
    }

    // endregion
}