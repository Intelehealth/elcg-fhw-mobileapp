package org.intelehealth.ezazi.activities.epartogramActivity.print

import kotlin.math.floor

/**
 * A sheet of paper, in PostScript points (1 pt = 1/72 inch) — the unit
 * PdfDocument and the print framework both work in.
 */
data class PageSpec(val name: String, val widthPt: Float, val heightPt: Float) {

    val isLandscape: Boolean get() = widthPt >= heightPt

    fun landscape(): PageSpec =
        if (isLandscape) this else PageSpec("$name landscape", heightPt, widthPt)

    fun portrait(): PageSpec =
        if (!isLandscape) this else PageSpec("$name portrait", heightPt, widthPt)

    companion object {
        val A4 = PageSpec("A4", 595f, 842f)
        val A3 = PageSpec("A3", 842f, 1191f)
        val A2 = PageSpec("A2", 1191f, 1684f)
        val A1 = PageSpec("A1", 1684f, 2384f)
        val A0 = PageSpec("A0", 2384f, 3370f)
        val LETTER = PageSpec("Letter", 612f, 792f)

        /** Media sizes arrive from the print dialog in mils (1/1000 inch). */
        fun fromMils(name: String, widthMils: Int, heightMils: Int) =
            PageSpec(name, widthMils * 0.072f, heightMils * 0.072f)
    }
}

/**
 * Fits the Labour Care Guide grid to an arbitrary sheet.
 *
 * Nothing here touches Android, so the arithmetic is unit-testable on the JVM.
 *
 * Two rules govern the layout, both set by the programme team:
 *
 *  - The chart is always **two sheets**. Sheet 1 carries first-stage hours 1
 *    to [SHEET1_LAST_STAGE1_HOUR]; sheet 2 carries the remaining first-stage
 *    hours followed by the whole second stage. The split never moves, whatever
 *    paper is used, so staff see the same structure at every site.
 *  - Row heights are fixed. Paper size changes how wide the columns are, not
 *    how tall the bands are — so a larger sheet buys roomier cells and larger
 *    cell text via [cellTextSize], never a different layout.
 */
object LcgSheetGeometry {

    const val MARGIN = 20f

    const val SECTION_COL = 15f
    const val NAME_COL = 88f
    const val ALERT_COL = 52f
    const val LABEL_WIDTH = SECTION_COL + NAME_COL + ALERT_COL

    /** Last first-stage hour on sheet 1. Everything after it moves to sheet 2. */
    const val SHEET1_LAST_STAGE1_HOUR = 12
    const val SHEET_COUNT = 2

    /** Comfortable column width. Narrower still renders — [cellTextSize] adapts. */
    const val MIN_COLUMN = 24f

    const val TEXT_LABEL = 8f
    const val TEXT_TIME = 7.5f
    const val TEXT_HEADER = 9f
    const val TEXT_TITLE = 12f
    const val TEXT_FOOTER = 6.5f
    const val TEXT_NOTE = 6f

    const val ROW_HEADER_BLOCK = 46f
    const val ROW_TIME = 24f
    const val ROW_HOURS = 12f
    const val ROW_ALERT = 14f
    const val ROW_DATA = 12f
    const val ROW_PLOT = 10f
    /**
     * Medication rows carry the drug, dose, route, rate and status as sideways
     * text, the same as the web view — so they need real height, not a single
     * line. Sized to what an A4 portrait sheet can spare once the fixed rows
     * and the protected shared-decision band are taken out.
     */
    const val ROW_MEDICATION = 46f

    /**
     * Height of one shared-decision row. Sized from the printed WHO form, which
     * fits roughly three wrapped lines of sideways text per hour. Deliberately
     * NOT trimmed to a character budget — no entry-length limit is confirmed,
     * so the band keeps the room the reference form gives it.
     */
    const val ROW_SHARED_DECISION = 65f

    const val ROW_INITIALS = 13f
    /**
     * The footer block: the instructions, the abbreviation legend and the cervix
     * plotting note, each wrapped to the page width — six lines on A4 portrait, with
     * room for a seventh if a paragraph wraps wider.
     *
     * This does NOT leave room for the delivery block underneath. A4 portrait gives
     * 802pt usable, the grid above the footer is 713pt, so a footer this tall leaves
     * 13pt and the block needs 56pt. [fitsWithDeliveryBlock] is what decides; when it
     * says no, the block is drawn on its own page instead of running off the paper.
     */
    const val ROW_FOOTER = 76f

    /** Delivery details block, drawn under the grid on sheet 2. */
    const val DELIVERY_BLOCK = 50f

    private const val DATA_ROWS = 4 + 6 + 6 + 2
    private const val PLOT_ROWS = 12
    const val MEDICATION_ROWS = 3
    const val SHARED_DECISION_ROWS = 2

    /** Everything whose height never varies with the content. */
    fun fixedRowsHeight(): Float =
        ROW_HEADER_BLOCK + ROW_TIME + ROW_HOURS + ROW_ALERT +
                DATA_ROWS * ROW_DATA +
                PLOT_ROWS * ROW_PLOT +
                ROW_INITIALS + ROW_FOOTER

    /** Minimum grid height: the fixed rows plus the smallest allowed tall bands. */
    fun gridHeight(): Float =
        fixedRowsHeight() +
                MEDICATION_ROWS * ROW_MEDICATION +
                SHARED_DECISION_ROWS * ROW_SHARED_DECISION


    fun usableWidth(page: PageSpec): Float = page.widthPt - 2 * MARGIN

    fun usableHeight(page: PageSpec): Float = page.heightPt - 2 * MARGIN

    fun dataWidth(page: PageSpec): Float = usableWidth(page) - LABEL_WIDTH

    /** Whether the row set clears the sheet's height. Grid rows never shrink. */
    fun fitsVertically(page: PageSpec): Boolean = gridHeight() <= usableHeight(page)

    /** Whether sheet 2's delivery block also clears the height. */
    /**
     * Whether the delivery-outcome block fits beneath the grid on one sheet.
     *
     * Load-bearing, not diagnostic: [gridHeight] plus the block must clear the page,
     * and on A4 portrait it does not. Drawing it anyway put the Apgar scores, baby
     * sex and status, and the visit-complete reason past the bottom edge, where a PDF
     * silently discards them.
     */
    fun fitsWithDeliveryBlock(page: PageSpec): Boolean =
        gridHeight() + DELIVERY_BLOCK <= usableHeight(page)

    /** Which sheet a column belongs on: 0 for sheet 1, 1 for sheet 2. */
    fun sheetOf(stage: Int, hour: Int): Int =
        if (stage == 1 && hour <= SHEET1_LAST_STAGE1_HOUR) 0 else 1

    /** Columns stretch to fill the data area, so the grid always reaches the margin. */
    fun columnWidth(page: PageSpec, columnsOnSheet: Int): Float =
        if (columnsOnSheet <= 0) MIN_COLUMN else dataWidth(page) / columnsOnSheet

    /**
     * Cell text scaled so a three-character observation fits its column with
     * padding. This is what makes larger paper produce larger print instead of
     * a different layout, and it absorbs hours that carry extra sub-columns.
     */
    fun cellTextSize(columnWidth: Float): Float =
        ((columnWidth - 4f) / (3f * 0.56f)).coerceIn(5.5f, 9.5f)

    /** Comfortable column count, for logging when a sheet is tighter than ideal. */
    fun comfortableColumns(page: PageSpec): Int =
        floor(dataWidth(page) / MIN_COLUMN).toInt().coerceAtLeast(1)
}
