package org.intelehealth.ezazi.stage3.postpartum.print

import org.intelehealth.ezazi.activities.epartogramActivity.print.PageSpec

/**
 * Layout for the Delivery Outcome Report.
 *
 * Nothing here touches Android, so the arithmetic is unit-testable on the JVM.
 *
 * The report is a different shape from the Labour Care Guide: eight fixed time
 * columns and about two dozen rows, which is tall and narrow rather than a wide
 * ribbon. So it needs no sideways text — the columns are wide enough for
 * ordinary wrapped text — and the whole report fits one sheet.
 *
 * Every dimension is derived from a single [scale] factor, the sheet's usable
 * width against A4 portrait's. A larger sheet is therefore a uniform
 * enlargement of the same layout: same structure, same proportion of the page
 * filled, bigger type. Nothing re-flows and nothing moves between sheets.
 */
object Stage3SheetGeometry {

    /** A4 portrait usable width, the size every other sheet is measured against. */
    private const val BASE_WIDTH = 555f

    const val MARGIN = 20f
    const val COLUMNS = 8

    private const val LABEL_COL = 95f

    private const val TEXT_TITLE = 11f
    private const val TEXT_META = 6.5f
    private const val TEXT_FIELD = 8f
    private const val TEXT_BAND = 6.5f
    private const val TEXT_SLOT = 6.5f
    private const val TEXT_SLOT_TIME = 5.5f
    private const val TEXT_ROW_LABEL = 6.8f
    private const val TEXT_VALUE = 8f
    private const val TEXT_PROSE = 7f
    private const val TEXT_FOOTER = 5.5f

    private const val ROW_TITLE = 16f
    private const val ROW_META = 18f
    private const val ROW_BAND = 15f
    private const val ROW_SLOT = 19f
    private const val ROW_VALUE = 14f
    private const val ROW_FOOTER = 16f
    private const val FIELD_LINE = 11f

    /**
     * Tallest a prose cell may grow. Past this the text is shortened on the
     * grid and listed in full underneath — the same bargain the Labour Care
     * Guide strikes, so one predictable sheet beats a varying page count.
     */
    private const val PROSE_MAX = 58f

    private const val CELL_PAD = 2.5f
    private const val LINE_RATIO = 1.22f

    fun usableWidth(page: PageSpec): Float = page.widthPt - 2 * MARGIN

    fun usableHeight(page: PageSpec): Float = page.heightPt - 2 * MARGIN

    /** Uniform enlargement factor for this sheet, never smaller than A4 portrait. */
    fun scale(page: PageSpec): Float = (usableWidth(page) / BASE_WIDTH).coerceIn(1f, 2f)

    fun labelWidth(page: PageSpec): Float = LABEL_COL * scale(page)

    fun columnWidth(page: PageSpec): Float =
        (usableWidth(page) - labelWidth(page)) / COLUMNS

    fun margin(page: PageSpec): Float = MARGIN * scale(page)

    fun titleText(page: PageSpec): Float = TEXT_TITLE * scale(page)
    fun metaText(page: PageSpec): Float = TEXT_META * scale(page)
    fun fieldText(page: PageSpec): Float = TEXT_FIELD * scale(page)
    fun bandText(page: PageSpec): Float = TEXT_BAND * scale(page)
    fun slotText(page: PageSpec): Float = TEXT_SLOT * scale(page)
    fun slotTimeText(page: PageSpec): Float = TEXT_SLOT_TIME * scale(page)
    fun rowLabelText(page: PageSpec): Float = TEXT_ROW_LABEL * scale(page)
    fun valueText(page: PageSpec): Float = TEXT_VALUE * scale(page)
    fun proseText(page: PageSpec): Float = TEXT_PROSE * scale(page)
    fun footerText(page: PageSpec): Float = TEXT_FOOTER * scale(page)

    fun titleRow(page: PageSpec): Float = ROW_TITLE * scale(page)
    fun metaRow(page: PageSpec): Float = ROW_META * scale(page)
    fun bandRow(page: PageSpec): Float = ROW_BAND * scale(page)
    fun slotRow(page: PageSpec): Float = ROW_SLOT * scale(page)
    fun valueRow(page: PageSpec): Float = ROW_VALUE * scale(page)
    fun footerRow(page: PageSpec): Float = ROW_FOOTER * scale(page)
    fun fieldLine(page: PageSpec): Float = FIELD_LINE * scale(page)
    fun proseMax(page: PageSpec): Float = PROSE_MAX * scale(page)
    fun cellPad(page: PageSpec): Float = CELL_PAD * scale(page)

    /** Baseline-to-baseline distance for wrapped text at [textSize]. */
    fun lineHeight(textSize: Float): Float = textSize * LINE_RATIO

    /** Height a prose cell needs for [lines] wrapped lines, before the cap. */
    fun proseHeight(page: PageSpec, lines: Int, textSize: Float): Float =
        (lines * lineHeight(textSize) + 2 * cellPad(page)).coerceAtLeast(valueRow(page))

    /** How many wrapped lines a prose cell can show before text is shortened. */
    fun maxProseLines(page: PageSpec, textSize: Float): Int =
        (((proseMax(page) - 2 * cellPad(page)) / lineHeight(textSize)).toInt()).coerceAtLeast(1)
}
