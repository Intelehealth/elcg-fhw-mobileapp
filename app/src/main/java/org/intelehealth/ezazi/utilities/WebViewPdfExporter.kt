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
import kotlin.math.abs

/**
 * Exports a WebView's FULL document (including content hidden inside nested
 * horizontally/vertically scrolling containers) as a single-page PDF saved to
 * public Downloads/eZazi.
 *
 * Units — this is what governs the exported font size:
 * PdfDocument page dimensions are PostScript points (1/72 inch), NOT device
 * pixels. The page is therefore sized from the document's CSS width/height at
 * [PT_PER_CSS_PX] points per CSS pixel, and the device-pixel rendering is
 * mapped onto it through a scaled canvas. At 1 pt per CSS px a 12px CSS font
 * lands as real 12pt text, so the PDF at 100% zoom matches the page on screen.
 * Feeding device pixels straight into PageInfo (CSS px x display density)
 * yields a page roughly 3x too large in physical units, which every viewer's
 * fit-to-page then shrinks to a couple of points — and makes the output vary
 * with the device's screen density.
 *
 * All geometry is derived from [WebView.getScale] (page scale x device scale),
 * both for the layout width and for the canvas scale, so the two cancel: the
 * exported font size no longer depends on the user's current pinch-zoom level.
 * A best-effort zoom reset still runs first so the capture is laid out at the
 * document's natural width.
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

    /** PDF points emitted per CSS pixel. 1.0 keeps the page's own font sizes (12px -> 12pt). */
    private const val PT_PER_CSS_PX = 1.0

    /** Maximum page side the PDF format allows, in points (200 inches). */
    private const val PDF_MAX_SIDE_PT = 14400

    /** Insurance against clipping right-edge absolutely-positioned content. */
    private const val WIDTH_BUFFER = 1.01

    private const val PAGE_SCALE_TOLERANCE = 0.01f

    interface Callback {
        fun onSuccess(uri: Uri, displayName: String)
        fun onFailure(message: String?)
    }

    /** Java-friendly callback for [expandAndMeasure]. */
    fun interface WidthCallback {
        fun onMeasured(cssWidth: Int)
    }

    /**
     * Expands any scrolling containers so hidden columns get painted, then
     * pins the document to the measured width in CSS pixels and reports
     * `[width, height]`. The explicit pixel width matters: `max-content` on
     * the document root stops wrapping text (the LCG's instructions/
     * abbreviations footer) from wrapping at all, which inflates the measured
     * width and shrinks everything else to compensate. Original styles are
     * stashed on the page (window.__ezSaved) so they can be restored after
     * capture.
     */
    private const val JS_EXPAND_AND_MEASURE =
        "(function(){" +
                "  window.__ezSaved = [];" +
                "  function save(el){ window.__ezSaved.push([el, el.style.cssText]); }" +
                "  function fullWidth(){" +
                "    return Math.ceil(Math.max(document.documentElement.scrollWidth," +
                "                              document.body.scrollWidth));" +
                "  }" +
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
                "  var w = fullWidth();" +
                "  save(document.documentElement);" +
                "  save(document.body);" +
                "  document.documentElement.style.width = w + 'px';" +
                "  document.body.style.width = w + 'px';" +
                "  w = Math.max(w, fullWidth());" +
                "  var h = Math.ceil(Math.max(document.documentElement.scrollHeight," +
                "                             document.body.scrollHeight));" +
                "  return [w, h];" +
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
     * Full export: reset zoom -> expand -> capture -> save to Downloads -> restore.
     * Must be called on the main thread with a fully loaded page.
     * The callback is invoked on the main thread.
     */
    @JvmStatic
    fun export(activity: Activity, webView: WebView, displayName: String, callback: Callback) {
        resetPageZoom(webView) {
            expandAndMeasureContent(webView) { cssWidth, cssHeight ->
                capture(activity, webView, cssWidth, cssHeight, displayName, callback)
            }
        }
    }

    /**
     * Expands the page's scroll containers and reports the true full content
     * width (CSS px) after the page has re-laid out. Callers using this
     * directly (e.g. print flows) MUST call [restorePageStyles] afterwards.
     */
    @JvmStatic
    fun expandAndMeasure(webView: WebView, callback: WidthCallback) {
        expandAndMeasureContent(webView) { cssWidth, _ -> callback.onMeasured(cssWidth) }
    }

    /**
     * Publishes an already-written PDF into Downloads/eZazi, for producers
     * that build their own document rather than capturing a WebView.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun publishToDownloads(context: Context, pdfFile: File, displayName: String): Uri =
        savePdfToDownloads(context, pdfFile, displayName)

    /** Undoes [expandAndMeasure]'s style changes (and any body zoom applied after it). */
    @JvmStatic
    fun restorePageStyles(webView: WebView) {
        webView.evaluateJavascript(JS_RESTORE_STYLES, null)
        webView.invalidate()
    }

    // endregion

    // region ---- Measure ----

    private fun expandAndMeasureContent(webView: WebView, onMeasured: (Int, Int) -> Unit) {
        webView.evaluateJavascript(JS_EXPAND_AND_MEASURE) { value ->
            val (cssWidth, cssHeight) = parseSize(value)
            // Give the page a moment to re-layout after the style changes
            webView.postDelayed({ onMeasured(cssWidth, cssHeight) }, RELAYOUT_DELAY_MS)
        }
    }

    private fun parseSize(value: String?): Pair<Int, Int> {
        val parts = value?.trim()
            ?.removeSurrounding("\"")
            ?.trim('[', ']')
            ?.split(',')
            ?: return 0 to 0
        if (parts.size < 2) return 0 to 0
        val width = parts[0].trim().toDoubleOrNull() ?: return 0 to 0
        val height = parts[1].trim().toDoubleOrNull() ?: return 0 to 0
        return Math.ceil(width).toInt() to Math.ceil(height).toInt()
    }

    /**
     * Best-effort return to 100% page scale, so the capture is laid out at the
     * document's natural width rather than at whatever the user last pinched to.
     */
    @Suppress("DEPRECATION")
    private fun resetPageZoom(webView: WebView, onReady: () -> Unit) {
        val density = webView.resources.displayMetrics.density
        val pageScale = if (density > 0f) webView.scale / density else 1f
        if (!webView.settings.supportZoom() || pageScale <= 0f ||
            abs(pageScale - 1f) <= PAGE_SCALE_TOLERANCE
        ) {
            onReady()
            return
        }
        webView.zoomBy((1f / pageScale).coerceIn(0.01f, 100f))
        webView.postDelayed({ onReady() }, RELAYOUT_DELAY_MS)
    }

    // endregion

    // region ---- Capture ----

    /**
     * Lays the WebView out at the document's full size and draws it onto one
     * PDF page sized in points (see the unit note on [WebViewPdfExporter]).
     *
     * The layout pass and the draw are deliberately split across a frame:
     * Chromium re-lays-out asynchronously when the view width changes, so
     * drawing in the same call stack can capture the pre-resize layout.
     */
    @Suppress("DEPRECATION") // webView.scale is the honest CSS-px -> device-px factor
    private fun capture(
        activity: Activity,
        webView: WebView,
        cssWidth: Int,
        cssHeight: Int,
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

        val drawScale = webView.scale.toDouble()
            .takeIf { it > 0.0 }
            ?: webView.resources.displayMetrics.density.toDouble()

        val contentCssWidth = if (cssWidth > 0) {
            cssWidth
        } else {
            Math.ceil(originalWidth / drawScale).toInt()
        }

        val layoutWidthPx = maxOf(
            originalWidth,
            Math.ceil(contentCssWidth * drawScale * WIDTH_BUFFER).toInt()
        )
        val widthSpec = View.MeasureSpec.makeMeasureSpec(layoutWidthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        fun writePage() {
            try {
                // Re-measure now that Chromium has re-laid out at the full width
                webView.measure(widthSpec, heightSpec)
                val contentWidth = maxOf(webView.measuredWidth, layoutWidthPx)
                val contentHeight = maxOf(
                    webView.measuredHeight,
                    Math.ceil(cssHeight * drawScale).toInt(),
                    originalHeight
                )
                webView.layout(0, 0, contentWidth, contentHeight)

                var canvasScale = PT_PER_CSS_PX / drawScale
                val rawWidthPt = contentWidth * canvasScale
                val rawHeightPt = contentHeight * canvasScale
                val fit = minOf(
                    1.0,
                    PDF_MAX_SIDE_PT / rawWidthPt,
                    PDF_MAX_SIDE_PT / rawHeightPt
                )
                if (fit < 1.0) {
                    Timber.tag(TAG).w(
                        "Content exceeds the %d pt PDF page limit — scaling by %.3f",
                        PDF_MAX_SIDE_PT, fit
                    )
                    canvasScale *= fit
                }
                val pageWidthPt = Math.ceil(rawWidthPt * fit).toInt().coerceAtLeast(1)
                val pageHeightPt = Math.ceil(rawHeightPt * fit).toInt().coerceAtLeast(1)

                Timber.tag(TAG).d(
                    "Export: css=%dx%d scale=%.2f layout=%dx%d page=%dx%d pt",
                    contentCssWidth, cssHeight, drawScale,
                    contentWidth, contentHeight, pageWidthPt, pageHeightPt
                )

                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo
                    .Builder(pageWidthPt, pageHeightPt, 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                page.canvas.scale(canvasScale.toFloat(), canvasScale.toFloat())
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

        try {
            // Capture must start from the top-left of the document
            webView.scrollTo(0, 0)

            // Software rendering is required for full-document capture
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            webView.measure(widthSpec, heightSpec)
            webView.layout(
                0, 0,
                maxOf(webView.measuredWidth, layoutWidthPx),
                maxOf(webView.measuredHeight, originalHeight)
            )

            webView.postDelayed({ writePage() }, RELAYOUT_DELAY_MS)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "PDF layout pass failed")
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
