package org.intelehealth.ezazi.utilities

import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.stage3.Utils.NepaliDateUtils
import org.intelehealth.ezazi.ui.dialog.CalendarDialog

object ObstetricDatePicker {

    private const val TAG_DATE_PICKER = "date_picker"

    /**
     * Single entry point for every obstetric date field. Nepal gets the Bikram Sambat wheel picker,
     * every other region the Gregorian calendar dialog. Both hand the caller the same dd/MM/yyyy
     * Gregorian string, which is also what gets persisted.
     */
    fun show(
        activity: AppCompatActivity,
        titleRes: Int,
        currentGreg: String,
        onPicked: (String) -> Unit
    ) {
        if (AppRegion.usesBikramSambat()) showBikramSambatPicker(activity, titleRes, currentGreg, onPicked)
        else showGregorianPicker(activity, titleRes, currentGreg, onPicked)
    }

    private fun showGregorianPicker(
        activity: AppCompatActivity,
        titleRes: Int,
        currentGreg: String,
        onPicked: (String) -> Unit
    ) {
        val dialog = CalendarDialog.Builder(activity)
            .title(activity.getString(titleRes))
            .positiveButtonLabel(R.string.ok)
            .build()
        dialog.setDateFormat(GregorianDateUtils.GREG_FMT)
        GregorianDateUtils.gregStringToMillis(currentGreg)?.let { dialog.setDefaultDate(it) }
        dialog.setListener { _, _, _, value -> onPicked(value) }
        dialog.show(activity.supportFragmentManager, TAG_DATE_PICKER)
    }

    private fun showBikramSambatPicker(
        activity: AppCompatActivity,
        titleRes: Int,
        currentGreg: String,
        onPicked: (String) -> Unit
    ) {
        val stored = NepaliDateUtils.gregStringToBs(currentGreg)
        val init = if (stored != null && stored[0] > 0) stored else NepaliDateConverter.getCurrentBsDate()

        val yearPicker = NumberPicker(activity).apply {
            minValue = NepaliDateConverter.getMinSupportedBsYear()
            maxValue = NepaliDateConverter.getMaxSupportedBsYear()
            value = init[0]
        }
        val monthPicker = NumberPicker(activity).apply {
            minValue = 1
            maxValue = 12
            displayedValues = NepaliDateUtils.BS_MONTH_NAMES
            value = init[1]
        }
        val dayPicker = NumberPicker(activity)
        NepaliDateUtils.refreshDayPicker(dayPicker, init[0], init[1])
        dayPicker.value = minOf(init[2], dayPicker.maxValue)

        val onChange = NumberPicker.OnValueChangeListener { _, _, _ ->
            NepaliDateUtils.refreshDayPicker(dayPicker, yearPicker.value, monthPicker.value)
        }
        yearPicker.setOnValueChangedListener(onChange)
        monthPicker.setOnValueChangedListener(onChange)

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 24, 24, 24)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(yearPicker, lp)
            addView(monthPicker, lp)
            addView(dayPicker, lp)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(titleRes) + " (BS)")
            .setView(layout)
            .setPositiveButton(R.string.ok) { _, _ ->
                val greg = NepaliDateConverter.bsToGregorian(
                    yearPicker.value, monthPicker.value, dayPicker.value
                ) ?: return@setPositiveButton
                onPicked(NepaliDateUtils.toGregFmt(greg))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
