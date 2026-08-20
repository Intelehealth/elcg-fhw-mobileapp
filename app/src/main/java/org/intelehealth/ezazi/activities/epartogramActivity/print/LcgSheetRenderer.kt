package org.intelehealth.ezazi.activities.epartogramActivity.print

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import org.intelehealth.ezazi.utilities.NepaliDateConverter
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import kotlin.math.floor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Draws the Labour Care Guide straight onto a PDF page from the JSON that
 * EpartogramDataTransformer produces — no WebView, no HTML, no JavaScript.
 *
 * The observation format is untouched: every column the transformer emits is
 * drawn, at the transformer's own resolution, both stages, nothing merged.
 * What varies with the sheet is how many of those columns fit on one page —
 * cell text scales with the column via LcgSheetGeometry.cellTextSize, and
 * the grid continues onto another sheet when it runs out of room. Label
 * columns repeat on every sheet so each one stands alone.
 *
 * Dates and times: the transformer emits ISO instants in UTC with a trailing
 * Z. Those are read as UTC and presented in the device's own timezone, then
 * dates are shown in Bikram Sambat via [NepaliDateConverter], matching what
 * staff read on screen. Reading a Z-suffixed instant without its timezone
 * would shift every time on the sheet by the local offset — 5h45m in Nepal.
 */
object LcgSheetRenderer {

    private const val RED = 0xFFC62828.toInt()
    private const val AMBER = 0xFFEF6C00.toInt()
    private const val VALUE_BLUE = 0xFF0B5FA5.toInt()
    private const val GRID = 0xFF333333.toInt()
    private const val GRID_LIGHT = 0xFF9E9E9E.toInt()

    private const val TAG = "LcgSheetRenderer"

    private const val PARAM_OXYTOCIN = 19
    private const val PARAM_MEDICINE = 20
    private const val PARAM_IV_FLUIDS = 21
    private const val PARAM_ASSESSMENT = 22
    private const val PARAM_PLAN = 23

    private data class Col(val stage: Int, val index: Int, val hour: Int, val time: Date?)

    private class Row(val label: String, val alert: String, val paramIdx: Int)

    private class Section(val title: String, val rows: List<Row>, val rowHeight: Float)


    /** One grid entry whose text did not fit its cell, listed in full below. */
    private class NoteLine(
        val section: String,
        val stage: Int,
        val hour: Int,
        val time: Date?,
        val text: String
    )

    /**
     * Writes both sheets on [page], starting at [startNumber], followed by the
     * notes pages. Returns pages written.
     *
     * The split is a fixed rule, not a consequence of what fits: sheet 1 carries
     * first-stage hours 1 to 12, sheet 2 the rest of first stage plus all of
     * second stage. Paper size changes how wide the columns are, never which
     * hours land where.
     */
    fun renderTo(
        document: PdfDocument,
        data: JSONObject,
        page: PageSpec,
        startNumber: Int = 1
    ): Int {
        val g = LcgSheetGeometry
        val columns = buildColumns(data)
        val observations = countObservations(data, columns)
        if (observations == 0) {
            Timber.tag(TAG).w(
                "LCG render found 0 observations in %d columns — check the parameter key names",
                columns.size
            )
        }
        val overflow = mutableListOf<NoteLine>()
        val sheets = (0 until g.SHEET_COUNT)
            .map { index -> columns.filter { g.sheetOf(it.stage, it.hour) == index } }
            .filter { it.isNotEmpty() }
        var written = 0

        sheets.forEachIndexed { index, cols ->
            val pdfPage = document.startPage(pageInfo(page, startNumber + written))
            drawSheet(
                pdfPage.canvas, data, page, cols, overflow,
                index + 1, sheets.size,
                showDelivery = index == sheets.size - 1
            )
            document.finishPage(pdfPage)
            written++
        }

        if (overflow.isNotEmpty()) {
            written += renderNotes(document, data, page, startNumber + written, overflow)
        }
        return written
    }

    private fun pageInfo(page: PageSpec, number: Int) = PdfDocument.PageInfo
        .Builder(page.widthPt.toInt(), page.heightPt.toInt(), number)
        .create()

    // region ---- Column model ----

    private fun buildColumns(data: JSONObject): List<Col> {
        val columns = mutableListOf<Col>()
        addStage(columns, data, 1, "subColsPerHourStage1", "timeFullStage1")
        addStage(columns, data, 2, "subColsPerHourStage2", "timeFullStage2")
        return columns
    }

    private fun addStage(
        into: MutableList<Col>,
        data: JSONObject,
        stage: Int,
        subColsKey: String,
        timesKey: String
    ) {
        val subCols = data.optJSONArray(subColsKey) ?: return
        val times = data.optJSONArray(timesKey)
        var flat = 0
        for (hour in 0 until subCols.length()) {
            val count = subCols.optInt(hour, 1)
            for (sub in 0 until count) {
                into.add(Col(stage, flat, hour + 1, parseInstant(times?.optString(flat, null))))
                flat++
            }
        }
    }


    // endregion

    // region ---- Sheet ----

    private fun drawSheet(
        canvas: Canvas,
        data: JSONObject,
        page: PageSpec,
        cols: List<Col>,
        overflow: MutableList<NoteLine>,
        sheetNumber: Int,
        sheetCount: Int,
        showDelivery: Boolean
    ) {
        val g = LcgSheetGeometry
        val params = data.optJSONArray("parameters") ?: JSONArray()
        val left = g.MARGIN
        val dataLeft = left + g.LABEL_WIDTH
        val colWidth = g.columnWidth(page, cols.size)
        val right = dataLeft + colWidth * cols.size
        val cellText = g.cellTextSize(colWidth)

        var y = g.MARGIN
        y = drawHeaderBlock(canvas, data, cols, left, right, y, sheetNumber, sheetCount)
        val gridTop = y

        y = drawTimeRow(canvas, cols, data, left, dataLeft, colWidth, y)
        y = drawHoursRow(canvas, cols, left, dataLeft, colWidth, y)
        y = drawAlertRow(canvas, cols, left, dataLeft, colWidth, y)

        for (section in SECTIONS) {
            y = drawSection(canvas, section, params, cols, left, dataLeft, colWidth, y, cellText)
        }
        y = drawPlotSection(canvas, params, cols, left, dataLeft, colWidth, y, cellText)
        y = drawMedicationSection(
            canvas, params, cols, overflow, left, dataLeft, colWidth, y
        )
        y = drawSharedDecisionSection(
            canvas, params, cols, overflow, left, dataLeft, colWidth, y
        )
        y = drawInitialsRow(canvas, data, cols, left, dataLeft, colWidth, y)

        drawStageDivider(canvas, cols, dataLeft, colWidth, gridTop, y)
        y = drawFooter(canvas, left, right, y, page, sheetNumber, sheetCount)

        if (showDelivery && data.optBoolean("visitCompleted", false)) {
            drawDeliveryBlock(canvas, data, left, right, y + 6f)
        }
    }

    private fun drawHeaderBlock(
        canvas: Canvas,
        data: JSONObject,
        cols: List<Col>,
        left: Float,
        right: Float,
        top: Float,
        sheetNumber: Int,
        sheetCount: Int
    ): Float {
        val g = LcgSheetGeometry
        val p = patientInfo(data)

        val title = paint(g.TEXT_TITLE, bold = true, align = Paint.Align.CENTER)
        canvas.drawText("WHO LABOUR CARE GUIDE", (left + right) / 2f, top + 11f, title)

        val meta = paint(g.TEXT_LABEL, align = Paint.Align.RIGHT)
        meta.color = GRID
        val dates = bsRange(cols.firstOrNull { it.time != null }?.time, cols.lastOrNull { it.time != null }?.time)
        val suffix = if (dates.isEmpty()) "" else "$dates  ·  "
        canvas.drawText("${suffix}Sheet $sheetNumber of $sheetCount", right, top + 11f, meta)

        val row1 = listOf(
            "Name" to patientName(p),
            "Gravida" to p.optString("Gravida", "NA"),
            "Parity" to p.optString("Parity", "NA"),
            "Labour onset" to p.optString("LaborOnset", "NA")
        )
        val row2 = listOf(
            "Active labour" to patientDate(p.optString("ActiveLaborDiagnosed")),
            "Membranes ruptured" to patientDate(p.optString("MembraneRupturedTimestamp")),
            "Risk factors" to p.optString("Riskfactors", "NA"),
            "LMP" to patientDate(p.optString("LMP")),
            "EDD" to patientDate(p.optString("EDD"))
        )
        var baseline = drawFieldRow(canvas, row1, left, right, top + 26f)
        baseline = drawFieldRow(canvas, row2, left, right, baseline + 12f)

        val bottom = maxOf(top + g.ROW_HEADER_BLOCK, baseline + 8f)
        canvas.drawLine(left, bottom - 3f, right, bottom - 3f, strokePaint(0.8f, GRID))
        return bottom
    }

    /**
     * Lays label/value pairs out left to right, each starting where the last
     * one ended, and wraps to a new line when the row runs out of width.
     * Fixed slots let a long value — a Bikram Sambat date and time, say —
     * collide with the field after it. Returns the baseline of the last line.
     */
    private fun drawFieldRow(
        canvas: Canvas,
        fields: List<Pair<String, String>>,
        left: Float,
        right: Float,
        y: Float,
        lineHeight: Float = 12f
    ): Float {
        val g = LcgSheetGeometry
        val key = paint(g.TEXT_LABEL, bold = true)
        val value = paint(g.TEXT_LABEL)
        val gap = 14f
        var x = left
        var baseline = y
        fields.forEach { (k, v) ->
            val label = "$k: "
            val labelWidth = key.measureText(label)
            val shown = clip(v, right - left - labelWidth, value)
            val width = labelWidth + value.measureText(shown)
            if (x > left && x + width > right) {
                x = left
                baseline += lineHeight
            }
            canvas.drawText(label, x, baseline, key)
            canvas.drawText(shown, x + labelWidth, baseline, value)
            x += width + gap
        }
        return baseline
    }

    private fun drawTimeRow(
        canvas: Canvas,
        cols: List<Col>,
        data: JSONObject,
        left: Float,
        dataLeft: Float,
        colWidth: Float,
        top: Float
    ): Float {
        val g = LcgSheetGeometry
        val h = g.ROW_TIME
        drawLabelCell(canvas, "Time", "", left, top, h)

        val sos = sosSet(data)
        val timePaint = paint(g.TEXT_TIME, align = Paint.Align.CENTER)
        val flag = paint(g.TEXT_FOOTER, bold = true, align = Paint.Align.CENTER)
        flag.color = RED

        cols.forEachIndexed { i, col ->
            val x = dataLeft + i * colWidth
            cell(canvas, x, top, colWidth, h)
            val time = col.time ?: return@forEachIndexed
            val label = localTime(time)
            val cx = x + colWidth / 2f
            if (timePaint.measureText(label) <= colWidth - 2f) {
                canvas.drawText(label, cx, top + 11f, timePaint)
            } else {
                canvas.drawText(label.substringBefore(':'), cx, top + 9f, timePaint)
                canvas.drawText(label.substringAfter(':'), cx, top + 17f, timePaint)
            }
            if (sos.contains(encounterUuid(data, col))) {
                canvas.drawText("SOS", cx, top + h - 2f, flag)
            }
        }
        drawDayBreaks(canvas, cols, dataLeft, colWidth, top, h)
        return top + h
    }

    /** A heavier rule wherever the local calendar day rolls over. */
    private fun drawDayBreaks(
        canvas: Canvas,
        cols: List<Col>,
        dataLeft: Float,
        colWidth: Float,
        top: Float,
        height: Float
    ) {
        for (i in 1 until cols.size) {
            val previous = cols[i - 1].time ?: continue
            val current = cols[i].time ?: continue
            if (!sameLocalDay(previous, current)) {
                val x = dataLeft + i * colWidth
                canvas.drawLine(x, top, x, top + height, strokePaint(1.2f, GRID))
            }
        }
    }

    private fun drawHoursRow(
        canvas: Canvas,
        cols: List<Col>,
        left: Float,
        dataLeft: Float,
        colWidth: Float,
        top: Float
    ): Float {
        val g = LcgSheetGeometry
        val h = g.ROW_HOURS
        drawLabelCell(canvas, "Hours", "", left, top, h)
        val text = paint(g.TEXT_LABEL, bold = true, align = Paint.Align.CENTER)
        forEachHourRun(cols) { startIdx, endIdx, col ->
            val x = dataLeft + startIdx * colWidth
            val w = (endIdx - startIdx + 1) * colWidth
            cell(canvas, x, top, w, h)
            canvas.drawText(col.hour.toString(), x + w / 2f, top + 9f, text)
        }
        return top + h
    }

    private fun drawAlertRow(
        canvas: Canvas,
        cols: List<Col>,
        left: Float,
        dataLeft: Float,
        colWidth: Float,
        top: Float
    ): Float {
        val g = LcgSheetGeometry
        val h = g.ROW_ALERT
        drawLabelCell(canvas, "ALERT", "", left, top, h)
        val text = paint(g.TEXT_LABEL, bold = true, align = Paint.Align.CENTER)
        var i = 0
        while (i < cols.size) {
            val stage = cols[i].stage
            var j = i
            while (j + 1 < cols.size && cols[j + 1].stage == stage) j++
            val x = dataLeft + i * colWidth
            val w = (j - i + 1) * colWidth
            cell(canvas, x, top, w, h)
            val label = if (stage == 1) "ACTIVE FIRST STAGE" else "SECOND STAGE"
            canvas.drawText(label, x + w / 2f, top + 10f, text)
            i = j + 1
        }
        return top + h
    }

    private fun drawSection(
        canvas: Canvas,
        section: Section,
        params: JSONArray,
        cols: List<Col>,
        left: Float,
        dataLeft: Float,
        colWidth: Float,
        top: Float,
        cellText: Float
    ): Float {
        var y = top
        section.rows.forEach { row ->
            drawLabelCell(canvas, row.label, row.alert, left, y, section.rowHeight)
            drawValueRow(
                canvas, params, row.paramIdx, cols, dataLeft, colWidth,
                y, section.rowHeight, cellText
            )
            y += section.rowHeight
        }
        drawSectionTitle(canvas, section.title, left, top, y)
        return y
    }

    private fun drawValueRow(
        canvas: Canvas,
        params: JSONArray,
        paramIdx: Int,
        cols: List<Col>,
        dataLeft: Float,
        colWidth: Float,
        top: Float,
        height: Float,
        cellText: Float
    ) {
        val text = paint(cellText, align = Paint.Align.CENTER)
        text.color = VALUE_BLUE
        cols.forEachIndexed { i, col ->
            val x = dataLeft + i * colWidth
            cell(canvas, x, top, colWidth, height)
            val obs = cellFor(params, paramIdx, col) ?: return@forEachIndexed
            val value = primitiveValue(obs) ?: return@forEachIndexed
            val cx = x + colWidth / 2f
            val shown = clip(shortValue(value), colWidth - 2f, text)
            canvas.drawText(shown, cx, top + height - 3.5f, text)
            val width = text.measureText(shown)
            when (obs.optString("comment")) {
                "R" -> ring(canvas, cx, top + height / 2f, height, width, colWidth, RED)
                "Y" -> ring(canvas, cx, top + height / 2f, height, width, colWidth, AMBER)
            }
        }
    }

    private fun drawPlotSection(
        canvas: Canvas,
        params: JSONArray,
        cols: List<Col>,
        left: Float,
        dataLeft: Float,
        colWidth: Float,
        top: Float,
        cellText: Float
    ): Float {
        val g = LcgSheetGeometry
        var y = top
        val mark = paint(cellText, bold = true, align = Paint.Align.CENTER)
        mark.color = VALUE_BLUE

        listOf(
            Triple(17, CERVIX_LEVELS, "X"),
            Triple(18, DESCENT_LEVELS, "O")
        ).forEach { (paramIdx, levels, glyph) ->
            val sectionTop = y
            levels.forEachIndexed { rowIdx, level ->
                val label = if (rowIdx == 0) {
                    if (paramIdx == 17) "Cervix [X]" else "Descent [O]"
                } else ""
                drawLabelCell(canvas, label, level.first, left, y, g.ROW_PLOT)
                cols.forEachIndexed { i, col ->
                    val x = dataLeft + i * colWidth
                    cell(canvas, x, y, colWidth, g.ROW_PLOT)
                    val value = cellFor(params, paramIdx, col)?.let { primitiveValue(it) }
                    if (value == level.first || (paramIdx == 17 && value == "P" && rowIdx == 0)) {
                        val glyphText = if (value == "P") "P" else glyph
                        canvas.drawText(glyphText, x + colWidth / 2f, y + g.ROW_PLOT - 3f, mark)
                    }
                }
                y += g.ROW_PLOT
            }
            drawSectionTitle(
                canvas, if (paramIdx == 17) "CERVIX" else "DESCENT", left, sectionTop, y
            )
        }
        return y
    }

    /**
     * Oxytocin, Medicine and IV fluids as sideways text inside each hour's
     * cell — drug, dose, route, rate and status, the same content the web view
     * shows. Entries recorded in the same hour are joined rather than hidden.
     */
    private fun drawMedicationSection(
        canvas: Canvas,
        params: JSONArray,
        cols: List<Col>,
        overflow: MutableList<NoteLine>,
        left: Float,
        dataLeft: Float,
        colWidth: Float,
        top: Float
    ): Float {
        val g = LcgSheetGeometry
        var y = top
        val rowHeight = g.ROW_MEDICATION
        val body = paint(g.TEXT_NOTE)
        body.color = VALUE_BLUE

        val rows = listOf(
            Triple("Oxytocin", "U/L, drops", PARAM_OXYTOCIN),
            Triple("Medicine", "", PARAM_MEDICINE),
            Triple("IV fluids", "", PARAM_IV_FLUIDS)
        )
        rows.forEach { (label, alert, paramIdx) ->
            drawLabelCell(canvas, label, alert, left, y, rowHeight)
            forEachHourRun(cols) { startIdx, endIdx, _ ->
                val x = dataLeft + startIdx * colWidth
                val w = (endIdx - startIdx + 1) * colWidth
                cell(canvas, x, y, w, rowHeight)
                val text = medicationHourText(params, paramIdx, cols, startIdx, endIdx)
                if (text.isNotEmpty()) {
                    val shortened =
                        drawRotatedText(canvas, text, x, y, w, rowHeight, body)
                    if (shortened) {
                        overflow.add(
                            NoteLine(
                                label.uppercase(Locale.US),
                                cols[startIdx].stage, cols[startIdx].hour,
                                cols[startIdx].time, text
                            )
                        )
                    }
                }
            }
            y += rowHeight
        }
        drawSectionTitle(canvas, "MEDICATION", left, top, y)
        return y
    }

    /** Every medication entry recorded anywhere within one hour's sub-columns. */
    private fun medicationHourText(
        params: JSONArray,
        paramIdx: Int,
        cols: List<Col>,
        startIdx: Int,
        endIdx: Int
    ): String {
        val parts = mutableListOf<String>()
        for (i in startIdx..endIdx) {
            cellEntries(params, paramIdx, cols[i]).forEach { entry ->
                val text = medicationText(paramIdx, entry)
                if (text.isNotEmpty() && !isNothingGiven(text)) parts.add(text)
            }
        }
        return parts.joinToString(" \u00b7 ")
    }

    /**
     * Assessment and Plan as sideways text wrapped inside each hour's cell —
     * the arrangement the printed WHO form uses. Anything longer than the cell
     * holds is clipped with an ellipsis; the full text is on the notes page, so
     * nothing a clinician wrote is lost.
     */
    private fun drawSharedDecisionSection(
        canvas: Canvas,
        params: JSONArray,
        cols: List<Col>,
        overflow: MutableList<NoteLine>,
        left: Float,
        dataLeft: Float,
        colWidth: Float,
        top: Float
    ): Float {
        val g = LcgSheetGeometry
        var y = top
        val rowHeight = g.ROW_SHARED_DECISION
        val body = paint(g.TEXT_NOTE)
        body.color = VALUE_BLUE

        listOf("ASSESSMENT" to PARAM_ASSESSMENT, "PLAN" to PARAM_PLAN).forEach { (label, paramIdx) ->
            drawLabelCell(canvas, label, "", left, y, rowHeight)
            forEachHourRun(cols) { startIdx, endIdx, _ ->
                val x = dataLeft + startIdx * colWidth
                val w = (endIdx - startIdx + 1) * colWidth
                cell(canvas, x, y, w, rowHeight)
                val text = hourText(params, paramIdx, cols, startIdx, endIdx)
                if (text.isNotEmpty()) {
                    val shortened =
                        drawRotatedText(canvas, text, x, y, w, rowHeight, body)
                    if (shortened) {
                        overflow.add(
                            NoteLine(
                                label.uppercase(Locale.US),
                                cols[startIdx].stage, cols[startIdx].hour,
                                cols[startIdx].time, text
                            )
                        )
                    }
                }
            }
            y += rowHeight
        }
        drawSectionTitle(canvas, "SHARED DECISION", left, top, y)
        return y
    }

    /** Joins every entry recorded anywhere within one hour's sub-columns. */
    private fun hourText(
        params: JSONArray,
        paramIdx: Int,
        cols: List<Col>,
        startIdx: Int,
        endIdx: Int
    ): String {
        val parts = mutableListOf<String>()
        for (i in startIdx..endIdx) {
            cellEntries(params, paramIdx, cols[i]).forEach { entry ->
                val text = describeValue(entry.opt("value"))
                if (text.isNotEmpty()) parts.add(text)
            }
        }
        return parts.joinToString(" · ")
    }

    /** Delivery outcome, drawn under the grid on the last sheet. */
    private fun drawDeliveryBlock(
        canvas: Canvas,
        data: JSONObject,
        left: Float,
        right: Float,
        top: Float
    ) {
        val g = LcgSheetGeometry
        canvas.drawRect(left, top, right, top + g.DELIVERY_BLOCK, strokePaint(0.8f, GRID))
        canvas.drawText(
            "VISIT COMPLETE DETAILS", left + 4f, top + 12f,
            paint(g.TEXT_HEADER, bold = true)
        )

        val motherDied = data.optString("motherDeceased", "").equals("YES", ignoreCase = true)
        val padLeft = left + 4f
        val padRight = right - 4f

        drawFieldRow(
            canvas, listOf(
                "Visit complete" to data.optString("visitCompleteReason", "").ifEmpty { "NA" },
                "Birth outcome" to data.optString("birthOutcome", "").ifEmpty { "NA" },
                "Birth weight" to data.optString("birthWeight", "").ifEmpty { "NA" },
                "Mother status" to if (motherDied) "Death" else "Healthy"
            ), padLeft, padRight, top + 26f
        )
        drawFieldRow(
            canvas, listOf(
                "Apgar 1 min" to data.optString("apgar1", "").ifEmpty { "NA" },
                "Apgar 5 min" to data.optString("apgar5", "").ifEmpty { "NA" },
                "Baby sex" to data.optString("babyGender", "").ifEmpty { "NA" },
                "Baby status" to data.optString("babyStatus", "").ifEmpty { "NA" }
            ), padLeft, padRight, top + 38f
        )

        val notes = listOfNotNull(
            data.optString("outOfTimeReason", "").ifEmpty { null }?.let { "Out of time: $it" },
            data.optString("referTypeOtherReason", "").ifEmpty { null }?.let { "Other reason: $it" },
            data.optString("birthOutcomeOther", "").ifEmpty { null }?.let { "Other outcome: $it" },
            data.optString("motherDeceasedReason", "").ifEmpty { null }
                ?.let { "Mother death reason: $it" }
        )
        if (notes.isNotEmpty()) {
            val line = paint(g.TEXT_LABEL)
            canvas.drawText(
                clip(notes.joinToString("   ·   "), padRight - padLeft, line),
                padLeft, top + 50f, line
            )
        }
    }

    private fun drawInitialsRow(
        canvas: Canvas,
        data: JSONObject,
        cols: List<Col>,
        left: Float,
        dataLeft: Float,
        colWidth: Float,
        top: Float
    ): Float {
        val g = LcgSheetGeometry
        val h = g.ROW_INITIALS
        drawLabelCell(canvas, "INITIALS", "", left, top, h)
        val text = paint(g.TEXT_LABEL, align = Paint.Align.CENTER)
        val s1 = data.optJSONArray("initialsStage1")
        val s2 = data.optJSONArray("initialsStage2")
        forEachHourRun(cols) { startIdx, endIdx, col ->
            val x = dataLeft + startIdx * colWidth
            val w = (endIdx - startIdx + 1) * colWidth
            cell(canvas, x, top, w, h)
            val source = if (col.stage == 1) s1 else s2
            val initials = source?.optString(col.hour - 1, "").orEmpty()
                .takeUnless { it == "null" }.orEmpty()
            if (initials.isNotEmpty()) {
                canvas.drawText(initials, x + w / 2f, top + h - 4f, text)
            }
        }
        return top + h
    }

    private fun drawFooter(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        page: PageSpec,
        sheetNumber: Int,
        sheetCount: Int
    ): Float {
        val g = LcgSheetGeometry
        val text = paint(g.TEXT_FOOTER)
        text.color = GRID
        canvas.drawText(
            "INSTRUCTIONS: CIRCLE ANY OBSERVATION MEETING THE CRITERIA IN THE 'ALERT' COLUMN, " +
                    "ALERT THE SENIOR MIDWIFE OR DOCTOR AND RECORD THE ASSESSMENT AND ACTION TAKEN.",
            left, top + 9f, text
        )
        canvas.drawText(
            "Y Yes · N No · D Declined · U Unknown · SP Supine · MO Mobile · E Early · L Late · " +
                    "V Variable · I Intact · C Clear · M Meconium · B Blood · A Anterior · P Posterior · T Transverse",
            left, top + 18f, text
        )
        val corner = page.heightPt - g.MARGIN
        val paper = paint(g.TEXT_FOOTER)
        paper.color = GRID_LIGHT
        canvas.drawText(page.name, left, corner, paper)
        if (sheetNumber < sheetCount) {
            val cont = paint(g.TEXT_FOOTER, bold = true, align = Paint.Align.RIGHT)
            cont.color = RED
            canvas.drawText("continued on sheet ${sheetNumber + 1}", right, corner, cont)
        }
        return top + g.ROW_FOOTER
    }

    private fun drawStageDivider(
        canvas: Canvas,
        cols: List<Col>,
        dataLeft: Float,
        colWidth: Float,
        top: Float,
        bottom: Float
    ) {
        for (i in 1 until cols.size) {
            if (cols[i].stage != cols[i - 1].stage) {
                val x = dataLeft + i * colWidth
                canvas.drawLine(x, top, x, bottom, strokePaint(1.4f, GRID))
            }
        }
    }


    // endregion

    // region ---- Notes pages ----

    /**
     * Everything that cannot fit a grid cell: the numbered medication entries
     * the grid points at, plus assessment, plan and prescription histories.
     * Paginates, and repeats the patient header so a loose sheet is
     * identifiable.
     */
    private fun renderNotes(
        document: PdfDocument,
        data: JSONObject,
        page: PageSpec,
        startNumber: Int,
        overflow: List<NoteLine>
    ): Int {
        val g = LcgSheetGeometry
        val groups = buildNoteGroups(overflow)
        val left = g.MARGIN
        val right = page.widthPt - g.MARGIN
        val bottom = page.heightPt - g.MARGIN

        var pagesWritten = 0
        var pdfPage = document.startPage(pageInfo(page, startNumber))
        var canvas = pdfPage.canvas
        var y = drawNotesHeader(canvas, data, left, right, pagesWritten + 1)

        fun newPageIfNeeded(space: Float) {
            if (y + space <= bottom) return
            document.finishPage(pdfPage)
            pagesWritten++
            pdfPage = document.startPage(pageInfo(page, startNumber + pagesWritten))
            canvas = pdfPage.canvas
            y = drawNotesHeader(canvas, data, left, right, pagesWritten + 1)
        }

        val heading = paint(g.TEXT_HEADER, bold = true)
        val stamp = paint(8f, bold = true)
        val body = paint(8f)
        val bulletX = left + 8f
        val textX = left + 20f

        canvas.drawText("SHORTENED ON THE GRID \u2014 FULL TEXT", left, y, heading)
        y += 16f

        groups.forEach { (section, lines) ->
            newPageIfNeeded(28f)
            canvas.drawText(section, left, y, heading)
            y += 13f

            lines.forEach { line ->
                val prefix = "Stage ${line.stage}, Hour ${line.hour}: ${bsDateTime(line.time)} - "
                val prefixWidth = stamp.measureText(prefix)
                val wrapped = wrapLines(
                    line.text, right - textX - prefixWidth, right - textX, body
                )
                newPageIfNeeded(wrapped.size * 11f + 5f)
                canvas.drawText("\u2022", bulletX, y, body)
                canvas.drawText(prefix, textX, y, stamp)
                wrapped.forEachIndexed { index, text ->
                    val x = if (index == 0) textX + prefixWidth else textX
                    canvas.drawText(text, x, y, body)
                    y += 11f
                }
                if (wrapped.isEmpty()) y += 11f
                y += 3f
            }
            y += 8f
        }

        document.finishPage(pdfPage)
        return pagesWritten + 1
    }

    private fun drawNotesHeader(
        canvas: Canvas,
        data: JSONObject,
        left: Float,
        right: Float,
        pageNumber: Int
    ): Float {
        val g = LcgSheetGeometry
        val p = patientInfo(data)
        var y = g.MARGIN + 12f

        canvas.drawText(
            "LABOUR CARE GUIDE — NOTES", left, y, paint(g.TEXT_TITLE, bold = true)
        )
        val meta = paint(g.TEXT_LABEL, align = Paint.Align.RIGHT)
        meta.color = GRID
        canvas.drawText("Notes page $pageNumber", right, y, meta)
        y += 12f

        drawFieldRow(
            canvas,
            listOf(
                "Name" to patientName(p),
                "Gravida" to p.optString("Gravida", "NA"),
                "Parity" to p.optString("Parity", "NA"),
                "Active labour" to patientDate(p.optString("ActiveLaborDiagnosed"))
            ),
            left, right, y
        )
        y += 6f
        canvas.drawLine(left, y, right, y, strokePaint(0.8f, GRID))
        return y + 16f
    }

    /**
     * Groups the shortened entries by the grid band they came from, in the same
     * top-to-bottom order as the sheet, so a reader who meets an ellipsis finds
     * its section in the place they expect. Within a section, entries run in
     * stage then hour order.
     */
    private fun buildNoteGroups(
        overflow: List<NoteLine>
    ): List<Pair<String, List<NoteLine>>> = NOTE_SECTION_ORDER
        .map { section -> section to overflow.filter { it.section == section } }
        .filter { it.second.isNotEmpty() }
        .map { (section, lines) ->
            section to lines.sortedWith(compareBy({ it.stage }, { it.hour }))
        }


    private fun wrapLines(text: String, maxWidth: Float, paint: Paint): List<String> =
        wrapLines(text, maxWidth, maxWidth, paint)

    /**
     * Wraps with a shorter first line, so a bold "Stage 1, Hour 4:" prefix can
     * sit on it and the rest hangs beneath at a constant indent.
     */
    private fun wrapLines(
        text: String,
        firstWidth: Float,
        restWidth: Float,
        paint: Paint
    ): List<String> {
        val lines = mutableListOf<String>()
        var line = StringBuilder()
        text.split(" ").forEach { word ->
            val limit = if (lines.isEmpty()) firstWidth else restWidth
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > limit && line.isNotEmpty()) {
                lines.add(line.toString())
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) lines.add(line.toString())
        return lines
    }

    // endregion

    // region ---- Primitives ----

    /** Shortens text with an ellipsis until it fits [maxWidth]. */
    private fun clip(text: String, maxWidth: Float, paint: Paint): String {
        if (maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return if (end <= 0) "" else text.substring(0, end) + "…"
    }

    /**
     * Sideways text reading bottom-to-top, wrapped so each line runs along the
     * cell's height and successive lines stack across its width.
     */
    private fun drawRotatedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        paint: Paint
    ): Boolean {
        val lineLength = h - 5f
        val pitch = paint.textSize * 1.35f
        val maxLines = maxOf(1, floor((w - 3f) / pitch).toInt())
        var lines = wrapLines(text, lineLength, paint)
        val shortened = lines.size > maxLines
        if (shortened) {
            val kept = lines.take(maxLines).toMutableList()
            kept[maxLines - 1] = clip(kept[maxLines - 1] + " …", lineLength, paint)
            lines = kept
        }
        canvas.save()
        canvas.translate(x, y + h)
        canvas.rotate(-90f)
        lines.forEachIndexed { i, line ->
            canvas.drawText(line, 3f, (i + 1) * pitch - 1.5f, paint)
        }
        canvas.restore()
        return shortened
    }

    private fun drawLabelCell(
        canvas: Canvas,
        label: String,
        alert: String,
        left: Float,
        top: Float,
        height: Float
    ) {
        val g = LcgSheetGeometry
        val nameX = left + g.SECTION_COL
        val alertX = nameX + g.NAME_COL
        cell(canvas, nameX, top, g.NAME_COL, height)
        cell(canvas, alertX, top, g.ALERT_COL, height)
        if (label.isNotEmpty()) {
            canvas.drawText(label, nameX + 3f, top + height - 4f, paint(g.TEXT_LABEL, bold = true))
        }
        if (alert.isNotEmpty()) {
            canvas.drawText(
                alert, alertX + g.ALERT_COL / 2f, top + height - 4f,
                paint(g.TEXT_TIME, align = Paint.Align.CENTER)
            )
        }
    }

    private fun drawSectionTitle(
        canvas: Canvas,
        title: String,
        left: Float,
        top: Float,
        bottom: Float
    ) {
        val g = LcgSheetGeometry
        cell(canvas, left, top, g.SECTION_COL, bottom - top)
        val cx = left + g.SECTION_COL / 2f
        val cy = (top + bottom) / 2f
        canvas.save()
        canvas.rotate(-90f, cx, cy)
        canvas.drawText(
            title, cx, cy + 2.2f,
            paint(g.TEXT_FOOTER, bold = true, align = Paint.Align.CENTER)
        )
        canvas.restore()
    }

    /**
     * Walks runs of columns belonging to the same clock hour, so hour numbers
     * and initials render as one merged cell spanning that hour's columns.
     */
    private fun forEachHourRun(cols: List<Col>, action: (Int, Int, Col) -> Unit) {
        var i = 0
        while (i < cols.size) {
            val start = i
            val stage = cols[i].stage
            val hour = cols[i].hour
            while (i + 1 < cols.size && cols[i + 1].stage == stage && cols[i + 1].hour == hour) i++
            action(start, i, cols[start])
            i++
        }
    }

    private fun cell(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.drawRect(x, y, x + w, y + h, strokePaint(0.4f, GRID_LIGHT))
    }

    /**
     * Circles an observation that met an alert threshold. Sized to the text,
     * not to the row: a circle tight enough for "L" slices through "160", and
     * on this form the ring is the clinical action, so it has to enclose the
     * whole value. Clamped to the column so neighbours stay clear.
     */
    private fun ring(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        height: Float,
        textWidth: Float,
        columnWidth: Float,
        color: Int
    ) {
        val rx = minOf(textWidth / 2f + 2.4f, columnWidth / 2f - 0.8f)
        val ry = (height / 2f) - 1f
        canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, strokePaint(1f, color))
    }

    private fun paint(
        size: Float,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        textAlign = align
        color = Color.BLACK
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun strokePaint(width: Float, color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            this.color = color
        }

    // endregion

    // region ---- Data access ----

    /** Cells hold either a single observation or a list of them, depending on the parameter. */
    private fun cellEntries(params: JSONArray, paramIdx: Int, col: Col): List<JSONObject> {
        val p = params.optJSONObject(paramIdx) ?: return emptyList()
        val values = if (col.stage == 1) {
            p.optJSONArray("stage1") ?: p.optJSONArray("stage1values")
        } else {
            p.optJSONArray("stage2") ?: p.optJSONArray("stage2values")
        }
        val raw = values?.opt(col.index) ?: return emptyList()
        return when (raw) {
            is JSONObject -> listOf(raw)
            is JSONArray -> (0 until raw.length()).mapNotNull { raw.optJSONObject(it) }
            else -> emptyList()
        }
    }

    private fun cellFor(params: JSONArray, paramIdx: Int, col: Col): JSONObject? =
        cellEntries(params, paramIdx, col).firstOrNull()

    /**
     * Words the grid has no room for, in the vocabulary the form's own footer
     * already defines: "N - No". Urine arrives as "Negative", which is wider
     * than a column at any font size.
     */
    private fun shortValue(value: String): String =
        SHORT_VALUES[value.trim().uppercase(Locale.US)] ?: value

    /** Grid cells only carry short scalars; structured values are flagged elsewhere. */
    private fun primitiveValue(obs: JSONObject): String? {
        val raw = obs.opt("value") ?: return null
        if (raw is JSONObject || raw is JSONArray) return null
        val text = raw.toString()
        return if (text.isEmpty() || text == "null") null else text
    }

    /**
     * "No" recorded against a medication row means nothing was given. The web
     * view leaves those cells blank rather than printing the word, and a column
     * of "No" is noise on paper.
     */
    private fun isNothingGiven(text: String): Boolean =
        text.trim().uppercase(Locale.US) in NOTHING_GIVEN

    private fun medicationText(paramIdx: Int, obs: JSONObject): String = when (paramIdx) {
        PARAM_OXYTOCIN -> describeInfusion(obs.opt("value"), "U/L")
        PARAM_IV_FLUIDS -> describeInfusion(obs.opt("value"), null)
        else -> describeValue(obs.opt("value"))
    }

    private fun describeInfusion(raw: Any?, strengthUnit: String?): String {
        if (raw !is JSONObject) return describeValue(raw)
        val parts = mutableListOf<String>()
        val type = raw.optString("type").ifEmpty { raw.optString("otherType") }
        if (type.isNotEmpty()) parts.add(type)
        val strength = raw.optString("strength")
        if (strength.isNotEmpty()) parts.add("$strength ${strengthUnit ?: ""}".trim())
        val rate = raw.optString("infusionRate")
        if (rate.isNotEmpty()) parts.add("$rate drops/min")
        val status = raw.optString("infusionStatus")
        if (status.isNotEmpty()) parts.add(status)
        return parts.joinToString(", ")
    }

    private fun describeValue(raw: Any?): String = when (raw) {
        null -> ""
        is JSONObject -> describeInfusion(raw, "U/L")
        is JSONArray -> (0 until raw.length()).joinToString("; ") { describeValue(raw.opt(it)) }
        else -> raw.toString().replace("::", " ").let { if (it == "null") "" else it }
    }

    /**
     * The transformer and the web page disagree on several key names, and the
     * page papers over it in its own normalizeData(). Both spellings are read
     * here for the same reason: reading only one silently yields an empty
     * chart rather than an error.
     *
     * transformer -> page: pInfo/pinfo, patientName/name, stage1/stage1values.
     */
    private fun patientInfo(data: JSONObject): JSONObject =
        data.optJSONObject("pInfo") ?: data.optJSONObject("pinfo") ?: JSONObject()

    private fun patientName(p: JSONObject): String {
        val name = p.optString("patientName").ifEmpty { p.optString("name") }
        return name.ifEmpty { "NA" }
    }

    /** Populated observation cells, so an empty chart is loud in the log. */
    private fun countObservations(data: JSONObject, columns: List<Col>): Int {
        val params = data.optJSONArray("parameters") ?: return 0
        var count = 0
        for (idx in 0 until params.length()) {
            columns.forEach { col -> count += cellEntries(params, idx, col).size }
        }
        return count
    }

    private fun sosSet(data: JSONObject): Set<String> {
        val arr = data.optJSONArray("sosEncounterUUIDs") ?: return emptySet()
        return (0 until arr.length()).mapNotNull { arr.optString(it, null) }.toSet()
    }

    private fun encounterUuid(data: JSONObject, col: Col): String {
        val key = if (col.stage == 1) "encuuid1Full" else "encuuid2Full"
        return data.optJSONArray(key)?.optJSONObject(col.index)?.optString("enc_uuid").orEmpty()
    }

    // endregion

    // region ---- Dates ----

    /**
     * Reads an instant emitted by EpartogramDataTransformer. A trailing Z means
     * UTC; a bare timestamp is taken as local, matching how the web view's
     * `new Date()` reads the same string.
     */
    private fun parseInstant(raw: String?): Date? {
        if (raw.isNullOrEmpty() || raw == "null" || raw.equals("U", ignoreCase = true)) return null
        val trimmed = raw.trim()
        val isUtc = trimmed.endsWith("Z", ignoreCase = true)
        val body = trimmed.substringBefore('.').removeSuffix("Z").removeSuffix("z").take(19)
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        format.timeZone = if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
        return try {
            format.parse(body)
        } catch (e: Exception) {
            null
        }
    }

    private fun localTime(date: Date?): String {
        if (date == null) return ""
        return SimpleDateFormat("HH:mm", Locale.US).format(date)
    }

    /**
     * Bikram Sambat display for the local calendar day of [date]. The converter
     * counts whole days from UTC midnight, so the instant is first reduced to
     * its local Y/M/D and re-presented as UTC midnight — otherwise anything
     * recorded after 18:15 UTC would print the previous day in Nepal.
     */
    private fun bsDisplay(date: Date?): String {
        if (date == null) return ""
        val local = Calendar.getInstance()
        local.time = date
        val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcMidnight.clear()
        utcMidnight.set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH)
        )
        return NepaliDateConverter.dateToBsDisplay(utcMidnight.time)
    }

    /**
     * Patient fields are not reliably timestamps — membrane rupture can read
     * "Intact" or "U", and LMP already arrives converted. Anything that is not
     * an instant is printed as it stands rather than blanked to NA.
     */
    /**
     * A patient header field. Dates are converted to Bikram Sambat with no
     * time; anything that is not a date — a coded value such as "I", say — is
     * printed exactly as the database stores it, deliberately, so the sheet
     * stays faithful to the record rather than interpreting it.
     */
    private fun patientDate(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text == "null") return "NA"
        val date = parseFlexible(text) ?: return text
        return bsDisplay(date)
    }

    /** Accepts every date shape the patient attributes are stored in. */
    private fun parseFlexible(raw: String): Date? {
        parseInstant(raw)?.let { return it }
        PATIENT_DATE_FORMATS.forEach { pattern ->
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.isLenient = false
                return format.parse(raw) ?: return@forEach
            } catch (e: Exception) {
                // try the next pattern
            }
        }
        return null
    }

    private fun bsDateTime(raw: String?): String {
        val instant = parseInstant(raw)
        if (instant != null) return bsDateTime(instant)
        val text = raw?.trim().orEmpty()
        return if (text.isEmpty() || text == "null") "NA" else text
    }

    private fun bsDateTime(date: Date?): String {
        if (date == null) return "NA"
        return "${bsDisplay(date)} ${localTime(date)}".trim()
    }

    /** "19–20 Shrawan 2083 BS" when the sheet spans days, a single date otherwise. */
    private fun bsRange(first: Date?, last: Date?): String {
        val start = bsDisplay(first)
        if (start.isEmpty()) return ""
        val end = bsDisplay(last)
        if (end.isEmpty() || end == start) return start
        val startDay = start.substringBefore(' ')
        val endTail = end.substringAfter(' ')
        val startTail = start.substringAfter(' ')
        return if (startTail == endTail) "$startDay–$end" else "$start – $end"
    }

    private fun sameLocalDay(a: Date, b: Date): Boolean {
        val ca = Calendar.getInstance().apply { time = a }
        val cb = Calendar.getInstance().apply { time = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
                ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    // endregion

    private val PATIENT_DATE_FORMATS = listOf(
        "dd/MM/yyyy hh:mm a",
        "dd/MM/yyyy HH:mm",
        "dd/MM/yyyy",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )

    private val SHORT_VALUES = mapOf(
        "NEGATIVE" to "N",
        "NONE" to "N",
        "ABSENT" to "N",
        "NO" to "N",
        "NIL" to "N",
        "POSITIVE" to "P",
        "PRESENT" to "P",
        "YES" to "Y",
        "TRACE" to "T"
    )

    private val NOTHING_GIVEN = setOf("NO", "N", "NIL", "NONE", "NOT GIVEN")

    /** Notes sections in the same order as the bands they come from on the grid. */
    private val NOTE_SECTION_ORDER =
        listOf("OXYTOCIN", "MEDICINE", "IV FLUIDS", "ASSESSMENT", "PLAN")

    private val CERVIX_LEVELS = listOf(
        "10" to "", "9" to "≥2h", "8" to "≥2.5h", "7" to "≥3h", "6" to "≥5h", "5" to "≥6h"
    )

    private val DESCENT_LEVELS = listOf(
        "5" to "", "4" to "", "3" to "", "2" to "", "1" to "", "0" to ""
    )

    private val SECTIONS = listOf(
        Section(
            "SUPPORTIVE CARE", listOf(
                Row("Companion", "N", 0),
                Row("Pain relief", "N", 1),
                Row("Oral fluid", "N", 2),
                Row("Posture", "SP", 3)
            ), LcgSheetGeometry.ROW_DATA
        ),
        Section(
            "BABY", listOf(
                Row("Baseline FHR", "<110, ≥160", 4),
                Row("FHR deceleration", "L", 5),
                Row("Amniotic fluid", "M+++, B", 6),
                Row("Fetal position", "P, T", 7),
                Row("Caput", "+++", 8),
                Row("Moulding", "+++", 9)
            ), LcgSheetGeometry.ROW_DATA
        ),
        Section(
            "WOMAN", listOf(
                Row("Pulse", "<60, ≥120", 10),
                Row("Systolic BP", "<80, ≥140", 11),
                Row("Diastolic BP", "≥90", 12),
                Row("Temperature °C", "<35, ≥37.5", 13),
                Row("Urine protein", "P3+, P4+", 14),
                Row("Urine acetone", "A3+, A4+", 25)
            ), LcgSheetGeometry.ROW_DATA
        ),
        Section(
            "LABOUR", listOf(
                Row("Contractions /10min", "≤2, >5", 15),
                Row("Duration of contr.", "<20, >60", 16)
            ), LcgSheetGeometry.ROW_DATA
        )
    )
}
