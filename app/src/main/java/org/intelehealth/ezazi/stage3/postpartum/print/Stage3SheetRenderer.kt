package org.intelehealth.ezazi.stage3.postpartum.print

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import org.intelehealth.ezazi.activities.epartogramActivity.print.PageSpec
import org.intelehealth.ezazi.utilities.NepaliDateConverter
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Draws the Delivery Outcome Report straight onto PDF pages from the JSON that
 * Stage3DataTransformer produces — no WebView, no HTML, no JavaScript.
 *
 * Everything lands on one sheet: delivery outcome, maternal vitals after
 * delivery, and newborn condition, across the transformer's eight fixed time
 * slots. Where a cell's text will not fit, it is shortened on the grid and
 * listed in full underneath, so the sheet count stays predictable rather than
 * varying with how much a midwife wrote.
 *
 * Colours follow the web report: values in blue, observations flagged in the
 * data in red on a light red fill, assessment and plan on pale blue.
 *
 * Dates arrive as ISO instants in UTC and are shown in the device's timezone
 * with Bikram Sambat dates, matching what staff read on screen.
 */
object Stage3SheetRenderer {

    private const val INK = 0xFF14110F.toInt()
    private const val VALUE_BLUE = 0xFF0B62D0.toInt()
    private const val ALERT_RED = 0xFFC5221F.toInt()
    private const val ALERT_FILL = 0xFFFCE8E6.toInt()
    private const val NARRATIVE_FILL = 0xFFE8F0FE.toInt()
    private const val GRID = 0xFF8D8681.toInt()
    private const val MUTED = 0xFF5D5751.toInt()

    /** One time slot: the transformer's label plus the encounter instant. */
    private class Slot(val label: String, val time: Date?)

    /** One grid row, with its eight cells already reduced to display text. */
    private class Row(
        val name: String,
        val cells: List<Cell>
    )

    private class Cell(
        val text: String,
        val alert: Boolean,
        val narrative: Boolean
    ) {
        val isEmpty: Boolean get() = text.isEmpty()
    }

    /** A cell whose text had to be shortened, listed in full below the grid. */
    private class Overflow(val section: String, val slot: String, val time: Date?, val text: String)

    private class Field(val label: String, val value: String, val span: Int = 1)

    fun renderTo(
        document: PdfDocument,
        data: JSONObject,
        page: PageSpec,
        startNumber: Int = 1
    ): Int {
        val g = Stage3SheetGeometry
        val slots = buildSlots(data)
        val maternal = buildRows(data.optJSONArray("maternalParams"))
        val newborn = buildRows(data.optJSONArray("newbornParams"))
        val overflow = mutableListOf<Overflow>()

        val pdfPage = document.startPage(pageInfo(page, startNumber))
        val canvas = pdfPage.canvas
        val left = g.margin(page)
        val right = page.widthPt - g.margin(page)
        var y = g.margin(page)

        y = drawTitle(canvas, data, page, left, right, y)
        y = drawDeliveryOutcome(canvas, data, page, left, right, y)
        y = drawBand(canvas, page, "MATERNAL VITALS AFTER DELIVERY", left, right, y)
        y = drawTable(canvas, page, slots, maternal, left, y, overflow, showSlots = true)
        y = drawBand(canvas, page, "NEWBORN CONDITION", left, right, y)
        drawTable(canvas, page, slots, newborn, left, y, overflow, showSlots = true)

        // The footer sits at a fixed height, so the shortened text cannot share
        // this page — it gets its own, as the Labour Care Guide's notes do.
        drawFooter(canvas, page, left, right, continues = overflow.isNotEmpty())
        document.finishPage(pdfPage)

        var written = 1
        if (overflow.isNotEmpty()) {
            written += renderOverflowPages(
                document, data.optJSONObject("pinfo") ?: JSONObject(),
                page, startNumber + written, overflow
            )
        }
        return written
    }

    private fun pageInfo(page: PageSpec, number: Int) = PdfDocument.PageInfo
        .Builder(page.widthPt.toInt(), page.heightPt.toInt(), number)
        .create()

    // region ---- Model ----

    private fun buildSlots(data: JSONObject): List<Slot> {
        val labels = data.optJSONArray("colLabels")
        val times = data.optJSONArray("colTimes")
        return (0 until Stage3SheetGeometry.COLUMNS).map { i ->
            Slot(
                labels?.optString(i, "").orEmpty(),
                parseInstant(times?.optString(i, null))
            )
        }
    }

    /**
     * Flattens the transformer's parameter array into display rows. A cell is
     * either absent, a single observation object, or a list of them for the
     * free-text rows — all three collapse to one string here.
     */
    private fun buildRows(params: JSONArray?): List<Row> {
        if (params == null) return emptyList()
        val rows = mutableListOf<Row>()
        for (i in 0 until params.length()) {
            val p = params.optJSONObject(i) ?: continue
            val name = p.optString("name")
            if (name.isEmpty()) continue
            val narrative = p.optBoolean("isTextarea", false)
            val values = p.optJSONArray("values")
            val cells = (0 until Stage3SheetGeometry.COLUMNS).map { c ->
                readCell(values?.opt(c), narrative, name)
            }
            rows.add(Row(displayName(name), cells))
        }
        return rows
    }

    private fun readCell(raw: Any?, narrative: Boolean, paramName: String): Cell = when (raw) {
        null, JSONObject.NULL -> Cell("", alert = false, narrative = narrative)
        is JSONObject -> raw.optString("value").trim().let { text ->
            Cell(text, alert = isAlert(paramName, text), narrative = narrative)
        }
        is JSONArray -> {
            val parts = (0 until raw.length()).mapNotNull { i ->
                val entry = raw.optJSONObject(i) ?: return@mapNotNull null
                val text = entry.optString("value").trim()
                if (text.isEmpty()) return@mapNotNull null
                val who = entry.optString("provider").trim()
                val at = clockAmPm(entry.optString("obsDatetime"))
                val credit = listOf(who, at).filter { it.isNotEmpty() && it != "-" }
                    .joinToString(" ")
                if (credit.isEmpty()) text else "$text ($credit)"
            }
            Cell(parts.joinToString(" · "), alert = false, narrative = narrative)
        }
        else -> Cell(raw.toString().trim(), alert = false, narrative = narrative)
    }

    /**
     * The abnormal-value rules, ported from isAlert() in stage3.html so the
     * printed sheet marks exactly what the report on screen marks. Keyed on the
     * transformer's own parameter name, before any display renaming.
     *
     * Deliberately not thresholds of my own invention, and deliberately not the
     * observation comment column — that carries R/G flags for some rows only,
     * and treating any comment as abnormal reddened normal observations.
     */
    private fun isAlert(paramName: String, value: String): Boolean {
        val v = value.trim().lowercase(Locale.US)
        if (v.isEmpty() || v == "-") return false
        val num = v.toFloatOrNull()

        return when (paramName) {
            "Pulse" -> num != null && (num < 60f || num >= 120f)
            "BP" -> {
                val parts = value.split("/")
                val systolic = parts.getOrNull(0)?.trim()?.toFloatOrNull()
                val diastolic = parts.getOrNull(1)?.trim()?.toFloatOrNull()
                (systolic != null && (systolic < 80f || systolic >= 140f)) ||
                        (diastolic != null && diastolic >= 90f)
            }
            "Temperature" -> num != null && (num < 95f || num >= 99.5f)
            "Respiratory Rate" -> num != null && num > 30f
            "Blood Loss" -> num != null && num >= 500f
            "Uterus Contracted", "Urine Passed", "Sucking / Feeding",
            "Feet (warm)", "Feet Temperature" -> v == "n" || v == "no"
            "Hematoma", "Grunting", "Chest Indrawing", "Fast Breathing",
            "Skin Color", "Umbilical Cord Oozing" -> v == "y" || v == "yes"
            "Complication" -> v != "no" && v != "n"
            else -> false
        }
    }

    /** Row labels the report spells differently from the concept names. */
    private fun displayName(name: String): String = when (name) {
        "BP" -> "BP (systolic/diastolic)"
        "Temperature" -> "Temprature °f"
        else -> name
    }

    // endregion

    // region ---- Blocks ----

    private fun drawTitle(
        canvas: Canvas,
        data: JSONObject,
        page: PageSpec,
        left: Float,
        right: Float,
        top: Float
    ): Float {
        val g = Stage3SheetGeometry
        val p = data.optJSONObject("pinfo") ?: JSONObject()

        canvas.drawText(
            "DELIVERY OUTCOME REPORT", (left + right) / 2f, top + g.titleText(page),
            paint(g.titleText(page), bold = true, align = Paint.Align.CENTER)
        )

        val y = top + g.titleRow(page) + g.fieldText(page)
        drawIdentityLine(canvas, page, p, left, right, y)

        val ruleY = y + g.cellPad(page) * 2
        canvas.drawLine(left, ruleY, right, ruleY, strokePaint(0.75f, INK))
        return ruleY + g.cellPad(page) * 2
    }

    /**
     * Name at the left margin, OpenMRS ID at the right, on one line. Shared by the
     * sheet and its continuation pages so a loose page is attributable and the two
     * headers cannot drift apart.
     *
     * The ID is measured first and the name clipped to what is left: a long name
     * ellipsises rather than colliding, and the ID — the field you would look a
     * record up by — never truncates.
     */
    private fun drawIdentityLine(
        canvas: Canvas,
        page: PageSpec,
        pinfo: JSONObject,
        left: Float,
        right: Float,
        baseline: Float
    ) {
        val g = Stage3SheetGeometry
        val labelPaint = paint(g.fieldText(page), bold = true)
        val valuePaint = paint(g.fieldText(page))
        valuePaint.color = VALUE_BLUE

        val idHead = "OpenMRS ID: "
        val idValue = pinfo.optString("openMrsId").ifEmpty { "NA" }
        val idHeadWidth = labelPaint.measureText(idHead)
        val idLeft = right - idHeadWidth - valuePaint.measureText(idValue)
        canvas.drawText(idHead, idLeft, baseline, labelPaint)
        canvas.drawText(idValue, idLeft + idHeadWidth, baseline, valuePaint)

        val nameHead = "Name: "
        val nameLeft = left + labelPaint.measureText(nameHead)
        canvas.drawText(nameHead, left, baseline, labelPaint)
        canvas.drawText(
            clip(
                pinfo.optString("name").ifEmpty { "NA" },
                idLeft - nameLeft - g.cellPad(page) * 3,
                valuePaint
            ),
            nameLeft, baseline, valuePaint
        )
    }

    private fun drawDeliveryOutcome(
        canvas: Canvas,
        data: JSONObject,
        page: PageSpec,
        left: Float,
        right: Float,
        top: Float
    ): Float {
        val g = Stage3SheetGeometry
        val o = data.optJSONObject("deliveryOutcome") ?: JSONObject()
        val fields = listOf(
            Field("Delivery Date", patientDate(o.optString("deliveryDate"))),
            Field("Delivery Time", clockOnly(o.optString("deliveryTime"))),
            Field("Delivery Mode", o.optString("deliveryMode").ifEmpty { "-" }, span = 2),
            Field(
                "Placenta & Membrane Delivery",
                o.optString("placentaMembraneDelivery").ifEmpty { "-" }, span = 2
            ),
            Field("Placenta Delivery Time", clockOnly(o.optString("placentaDeliveryTime"))),
            Field(
                "Placenta/Cord Abnormality",
                o.optString("placentaCordAbnormality").ifEmpty { "-" }
            ),
            Field("Perineal Laceration", o.optString("perinealLaceration").ifEmpty { "-" }),
            Field("Degree of Tear", o.optString("degreeOfTear").ifEmpty { "-" }),
            Field("Baby Status", o.optString("babyStatus").ifEmpty { "-" }),
            Field("Baby Gender", o.optString("babyGender").ifEmpty { "-" }),
            Field(
                "Baby Weight (in grams)",
                o.optString("babyWeight").ifEmpty { "-" }, span = 2
            ),
            Field("APGAR Score", o.optString("apgarScore").ifEmpty { "-" }),
            Field("Resuscitation", o.optString("resuscitation").ifEmpty { "-" }),
            Field("Skin-to-Skin", o.optString("skinToSkin").ifEmpty { "-" }),
            Field(
                "Breast-feeding (in 1 hour)",
                o.optString("breastfeedingInOneHour").ifEmpty { "-" }, span = 2
            ),
            Field("Gestation (weeks)", o.optString("gestationWeeks").ifEmpty { "-" }, span = 2)
        )

        val colWidth = (right - left) / 4f
        val labelPaint = paint(g.fieldText(page), bold = true)
        val valuePaint = paint(g.fieldText(page))
        valuePaint.color = VALUE_BLUE

        var y = top + g.fieldText(page) + g.cellPad(page)
        var col = 0
        fields.forEach { field ->
            if (col + field.span > 4) {
                col = 0
                y += g.fieldLine(page)
            }
            val x = left + col * colWidth
            val label = "${field.label}: "
            canvas.drawText(label, x, y, labelPaint)
            val labelWidth = labelPaint.measureText(label)
            val room = colWidth * field.span - labelWidth - g.cellPad(page) * 2
            canvas.drawText(clip(field.value, room, valuePaint), x + labelWidth, y, valuePaint)
            col += field.span
            if (col >= 4) {
                col = 0
                y += g.fieldLine(page)
            }
        }
        if (col != 0) y += g.fieldLine(page)

        // the two list-valued fields get the full width so they can wrap
        listOf(
            "AMTSL Medication" to o.optString("amtslMedication"),
            "Congenital Disorders" to o.optString("congenitalDisorders")
        ).forEach { (label, raw) ->
            val value = raw.replace("\n", " · ").trim()
            if (value.isEmpty() || value == "-") return@forEach
            val head = "$label: "
            val headWidth = labelPaint.measureText(head)
            canvas.drawText(head, left, y, labelPaint)
            val lines = wrapLines(value, right - left - headWidth, right - left, valuePaint)
            lines.forEachIndexed { i, line ->
                canvas.drawText(line, if (i == 0) left + headWidth else left, y, valuePaint)
                if (i < lines.size - 1) y += g.fieldLine(page)
            }
            y += g.fieldLine(page)
        }

        return y + g.cellPad(page)
    }

    private fun drawBand(
        canvas: Canvas,
        page: PageSpec,
        title: String,
        left: Float,
        right: Float,
        top: Float
    ): Float {
        val g = Stage3SheetGeometry
        val height = g.bandRow(page) - g.cellPad(page)
        canvas.drawRect(left, top, right, top + height, fillPaint(INK))
        val text = paint(g.bandText(page), bold = true)
        text.color = Color.WHITE
        canvas.drawText(title, left + g.cellPad(page) * 2, top + height - g.cellPad(page) * 1.6f, text)
        return top + g.bandRow(page)
    }

    private fun drawTable(
        canvas: Canvas,
        page: PageSpec,
        slots: List<Slot>,
        rows: List<Row>,
        left: Float,
        top: Float,
        overflow: MutableList<Overflow>,
        showSlots: Boolean
    ): Float {
        val g = Stage3SheetGeometry
        val labelWidth = g.labelWidth(page)
        val colWidth = g.columnWidth(page)
        val dataLeft = left + labelWidth
        var y = top

        if (showSlots) y = drawSlotRow(canvas, page, slots, left, dataLeft, colWidth, y)

        val prosePaint = paint(g.proseText(page))
        val valuePaint = paint(g.valueText(page), align = Paint.Align.CENTER)
        val labelPaint = paint(g.rowLabelText(page), bold = true)
        val maxLines = g.maxProseLines(page, g.proseText(page))
        val room = colWidth - 2 * g.cellPad(page)

        rows.forEach { row ->
            val wrapped = row.cells.map { cell ->
                if (cell.isEmpty) emptyList() else wrapLines(cell.text, room, prosePaint)
            }
            val needed = wrapped.maxOfOrNull { it.size } ?: 1
            val isProse = needed > 1
            val shown = needed.coerceAtMost(maxLines)
            val height =
                if (isProse) g.proseHeight(page, shown, g.proseText(page)) else g.valueRow(page)

            cell(canvas, left, y, labelWidth, height)
            canvas.drawText(
                clip(row.name, labelWidth - 2 * g.cellPad(page), labelPaint),
                left + g.cellPad(page), y + height - g.cellPad(page) * 1.8f, labelPaint
            )

            row.cells.forEachIndexed { i, c ->
                val x = dataLeft + i * colWidth
                if (c.narrative && !c.isEmpty) {
                    canvas.drawRect(x, y, x + colWidth, y + height, fillPaint(NARRATIVE_FILL))
                } else if (c.alert && !c.isEmpty) {
                    canvas.drawRect(x, y, x + colWidth, y + height, fillPaint(ALERT_FILL))
                }
                cell(canvas, x, y, colWidth, height)
                if (c.isEmpty) return@forEachIndexed

                if (isProse) {
                    val lines = wrapped[i]
                    val kept = if (lines.size > maxLines) {
                        overflow.add(
                            Overflow(row.name, slots.getOrNull(i)?.label.orEmpty(),
                                slots.getOrNull(i)?.time, c.text)
                        )
                        lines.take(maxLines).toMutableList().also {
                            it[maxLines - 1] = clip(it[maxLines - 1] + " …", room, prosePaint)
                        }
                    } else lines
                    prosePaint.color = if (c.alert) ALERT_RED else VALUE_BLUE
                    var ly = y + g.cellPad(page) + g.proseText(page)
                    kept.forEach { line ->
                        canvas.drawText(line, x + g.cellPad(page), ly, prosePaint)
                        ly += g.lineHeight(g.proseText(page))
                    }
                } else {
                    valuePaint.color = if (c.alert) ALERT_RED else VALUE_BLUE
                    valuePaint.isFakeBoldText = c.alert
                    canvas.drawText(
                        clip(c.text, room, valuePaint),
                        x + colWidth / 2f, y + height - g.cellPad(page) * 1.8f, valuePaint
                    )
                }
            }
            y += height
        }
        return y + g.cellPad(page) * 2
    }

    private fun drawSlotRow(
        canvas: Canvas,
        page: PageSpec,
        slots: List<Slot>,
        left: Float,
        dataLeft: Float,
        colWidth: Float,
        top: Float
    ): Float {
        val g = Stage3SheetGeometry
        val height = g.slotRow(page)
        cell(canvas, left, top, g.labelWidth(page), height)
        canvas.drawText(
            "Time", left + g.cellPad(page), top + height - g.cellPad(page) * 1.8f,
            paint(g.rowLabelText(page), bold = true)
        )

        val label = paint(g.slotText(page), bold = true, align = Paint.Align.CENTER)
        val time = paint(g.slotTimeText(page), align = Paint.Align.CENTER)
        time.color = MUTED

        slots.forEachIndexed { i, slot ->
            val x = dataLeft + i * colWidth
            cell(canvas, x, top, colWidth, height)
            val cx = x + colWidth / 2f
            canvas.drawText(slot.label, cx, top + g.slotText(page) + g.cellPad(page), label)
            slot.time?.let {
                canvas.drawText(localTime(it), cx, top + height - g.cellPad(page) * 1.6f, time)
            }
        }
        return top + height
    }

    /**
     * The text that would not fit its cell, grouped by row and slot. Same
     * bargain as the Labour Care Guide: the grid stays one predictable sheet
     * and nothing a clinician wrote is lost.
     */
    /**
     * The text that would not fit its cell, on pages of its own, grouped by the
     * row it came from and in slot order. Paginates, and repeats the patient
     * identity so a loose sheet is still attributable.
     */
    private fun renderOverflowPages(
        document: PdfDocument,
        pinfo: JSONObject,
        page: PageSpec,
        startNumber: Int,
        overflow: List<Overflow>
    ): Int {
        val g = Stage3SheetGeometry
        val left = g.margin(page)
        val right = page.widthPt - g.margin(page)
        val bottom = page.heightPt - g.margin(page) - g.footerRow(page)

        var written = 0
        var pdfPage = document.startPage(pageInfo(page, startNumber))
        var canvas = pdfPage.canvas
        var y = drawOverflowHeader(canvas, pinfo, page, left, right, written + 1)

        val head = paint(g.proseText(page), bold = true)
        val body = paint(g.proseText(page))
        val sectionPaint = paint(g.fieldText(page), bold = true)
        val indent = left + g.cellPad(page) * 6

        fun newPageIfNeeded(space: Float) {
            if (y + space <= bottom) return
            drawFooter(canvas, page, left, right, continues = true)
            document.finishPage(pdfPage)
            written++
            pdfPage = document.startPage(pageInfo(page, startNumber + written))
            canvas = pdfPage.canvas
            y = drawOverflowHeader(canvas, pinfo, page, left, right, written + 1)
        }

        overflow.groupBy { it.section }.forEach { (section, entries) ->
            newPageIfNeeded(g.fieldLine(page) * 2)
            canvas.drawText(section, left, y, sectionPaint)
            y += g.fieldLine(page)

            entries.sortedBy { it.time?.time ?: 0L }.forEach { entry ->
                val prefix = "${entry.slot}  ${bsDateTime(entry.time)} - "
                val prefixWidth = head.measureText(prefix)
                val lines = wrapLines(
                    entry.text, right - indent - prefixWidth, right - indent, body
                )
                newPageIfNeeded(lines.size * g.lineHeight(g.proseText(page)) + g.cellPad(page) * 2)
                canvas.drawText("•", left + g.cellPad(page) * 3, y, body)
                canvas.drawText(prefix, indent, y, head)
                lines.forEachIndexed { i, line ->
                    canvas.drawText(line, if (i == 0) indent + prefixWidth else indent, y, body)
                    y += g.lineHeight(g.proseText(page))
                }
                y += g.cellPad(page) * 2
            }
            y += g.cellPad(page) * 2
        }

        drawFooter(canvas, page, left, right, continues = false)
        document.finishPage(pdfPage)
        return written + 1
    }

    private fun drawOverflowHeader(
        canvas: Canvas,
        pinfo: JSONObject,
        page: PageSpec,
        left: Float,
        right: Float,
        pageNumber: Int
    ): Float {
        val g = Stage3SheetGeometry
        var y = g.margin(page) + g.titleText(page)

        canvas.drawText(
            "SHORTENED ON THE SHEET — FULL TEXT", left, y,
            paint(g.titleText(page), bold = true)
        )
        val meta = paint(g.metaText(page), align = Paint.Align.RIGHT)
        meta.color = MUTED
        canvas.drawText("Continuation page $pageNumber", right, y, meta)

        y += g.fieldLine(page) + g.cellPad(page) * 2
        drawIdentityLine(canvas, page, pinfo, left, right, y)

        y += g.cellPad(page) * 2
        canvas.drawLine(left, y, right, y, strokePaint(0.75f, INK))
        return y + g.fieldLine(page)
    }

    private fun drawFooter(
        canvas: Canvas,
        page: PageSpec,
        left: Float,
        right: Float,
        continues: Boolean
    ) {
        val g = Stage3SheetGeometry
        val y = page.heightPt - g.margin(page)
        val text = paint(g.footerText(page))
        text.color = MUTED
        canvas.drawText("${page.name} · Delivery Outcome Report", left, y, text)

        val end = paint(g.footerText(page), align = Paint.Align.RIGHT)
        if (continues) {
            end.color = ALERT_RED
            end.isFakeBoldText = true
            canvas.drawText("Shortened entries continue overleaf", right, y, end)
        } else {
            end.color = MUTED
            canvas.drawText("End of report", right, y, end)
        }
    }

    // endregion

    // region ---- Primitives ----

    private fun cell(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.drawRect(x, y, x + w, y + h, strokePaint(0.5f, GRID))
    }

    private fun wrapLines(text: String, maxWidth: Float, paint: Paint): List<String> =
        wrapLines(text, maxWidth, maxWidth, paint)

    private fun wrapLines(
        text: String,
        firstWidth: Float,
        restWidth: Float,
        paint: Paint
    ): List<String> {
        if (text.isEmpty()) return emptyList()
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

    private fun clip(text: String, maxWidth: Float, paint: Paint): String {
        if (maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return if (end <= 0) "" else text.substring(0, end) + "…"
    }

    private fun paint(
        size: Float,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        textAlign = align
        color = INK
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun strokePaint(width: Float, color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            this.color = color
        }

    private fun fillPaint(color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

    // endregion

    // region ---- Dates ----

    /** A trailing Z means UTC; a bare timestamp is read as local, as the web view does. */
    private fun parseInstant(raw: String?): Date? {
        if (raw.isNullOrEmpty() || raw == "null") return null
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

    private fun localTime(date: Date?): String =
        if (date == null) "" else SimpleDateFormat("HH:mm", Locale.US).format(date)

    /**
     * Bikram Sambat for the local calendar day. The converter counts whole days
     * from UTC midnight, so the instant is reduced to its local date first —
     * otherwise anything after 18:15 UTC prints the previous day in Nepal.
     */
    private fun bsDisplay(date: Date): String {
        val local = Calendar.getInstance().apply { time = date }
        val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH)
            )
        }
        return NepaliDateConverter.dateToBsDisplay(utcMidnight.time)
    }

    /** A date field: BS when it parses, otherwise whatever the record holds. */
    private fun patientDate(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text == "null" || text == "-") return "-"
        val date = parseInstant(text) ?: parseFlexible(text) ?: return text
        return bsDisplay(date)
    }

    /** A time-of-day field, shown as a clock reading rather than a full stamp. */
    private fun clockOnly(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text == "null" || text == "-") return "-"
        val date = parseInstant(text) ?: parseFlexible(text) ?: return text
        return localTime(date)
    }

    /** "02:51 PM" — the form the web report credits entries with. */
    private fun clockAmPm(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text == "null") return ""
        val date = parseInstant(text) ?: parseFlexible(text) ?: return ""
        return SimpleDateFormat("hh:mm a", Locale.US).format(date)
    }

    private fun bsDateTime(date: Date?): String =
        if (date == null) "" else "${bsDisplay(date)} ${localTime(date)}"

    private fun parseFlexible(raw: String): Date? {
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

    private val PATIENT_DATE_FORMATS = listOf(
        "dd/MM/yyyy hh:mm a",
        "dd/MM/yyyy HH:mm",
        "dd/MM/yyyy",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
        "hh:mm a",
        "HH:mm"
    )

    // endregion
}
