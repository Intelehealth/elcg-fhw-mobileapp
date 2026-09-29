package org.intelehealth.ezazi.utilities

import androidx.appcompat.app.AppCompatActivity
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.ui.dialog.ThemeTimePickerDialog
import java.util.Locale

object ObstetricTimePicker {

    private const val TAG_TIME_PICKER = "ThemeTimePickerDialog"

    /**
     * Shows the 24-hour picker and hands back the stored format. The trailing space is deliberate and
     * must not be trimmed: use24Hour makes the dialog pass an empty amPm, and every record already
     * written by the registration screen carries it. See PatientOtherInfoFragment.java:814-821.
     */
    fun show(activity: AppCompatActivity, onPicked: (String) -> Unit) {
        val dialog = ThemeTimePickerDialog.Builder(activity)
            .title(R.string.current_time)
            .positiveButtonLabel(R.string.ok)
            .use24Hour(true)
            .build()
        dialog.setListener { hours, minutes, amPm, _ ->
            onPicked(String.format(Locale.ENGLISH, "%02d:%02d %s", hours, minutes, amPm))
        }
        dialog.show(activity.supportFragmentManager, TAG_TIME_PICKER)
    }
}
