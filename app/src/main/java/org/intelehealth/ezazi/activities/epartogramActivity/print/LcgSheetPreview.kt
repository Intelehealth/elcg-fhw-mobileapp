package org.intelehealth.ezazi.activities.epartogramActivity.print

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import org.intelehealth.ezazi.utilities.WebViewPdfExporter
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug-only proof of the native renderer. Draws the same visit onto several
 * different sheets in one document so the paper-independence can be checked by
 * flipping through it: the observations and the text size stay identical, only
 * the number of columns per sheet and the sheet count change.
 *
 * Nothing in the shipped flow calls this; the renderer it exercises is the
 * real one.
 */
object LcgSheetPreview {

    private val SHEETS = listOf(
        PageSpec.A4.portrait(),
        PageSpec.A3.portrait()
    )

    /** Renders [data] (sample data when null) and returns the saved file's Uri. */
    fun generate(context: Context, data: JSONObject? = null): Pair<Uri, String> {
        val visit = data ?: LcgSampleVisit.build()
        val document = PdfDocument()
        var pageNumber = 1

        try {
            SHEETS.forEach { sheet ->
                if (LcgSheetGeometry.fitsVertically(sheet)) {
                    pageNumber += LcgSheetRenderer.renderTo(document, visit, sheet, pageNumber)
                }
            }

            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val displayName = "LCG_native_preview_$stamp.pdf"
            val temp = File(context.cacheDir, displayName)
            FileOutputStream(temp).use { document.writeTo(it) }
            val uri = WebViewPdfExporter.publishToDownloads(context, temp, displayName)
            temp.delete()
            return uri to displayName
        } finally {
            document.close()
        }
    }

    /** One line per sheet describing what the geometry decided, for the log. */
    fun describe(data: JSONObject? = null): String {
        val visit = data ?: LcgSampleVisit.build()
        val columns = (visit.optInt("totalStage1Cols") + visit.optInt("totalStage2Cols"))
        return SHEETS.joinToString("\n") { sheet ->
            val comfortable = LcgSheetGeometry.comfortableColumns(sheet)
            val splitHour = LcgSheetGeometry.SHEET1_LAST_STAGE1_HOUR
            "${sheet.name}: $columns columns over ${LcgSheetGeometry.SHEET_COUNT} sheets " +
                    "(split after stage-1 hour $splitHour), comfortable width holds $comfortable, " +
                    "grid ${LcgSheetGeometry.gridHeight()}pt of ${LcgSheetGeometry.usableHeight(sheet)}pt, " +
                    "delivery fits=${LcgSheetGeometry.fitsWithDeliveryBlock(sheet)}"
        }
    }
}
