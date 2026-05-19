package org.intelehealth.ezazi.utilities

import android.content.Context
import androidx.fragment.app.FragmentActivity
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.ui.dialog.AppDialogUtils

object InternetDialogHelper {

    @JvmStatic
    fun showNoInternetDialog(context: Context) {

        val activity = context as? FragmentActivity
            ?: return

        AppDialogUtils.showSingleButtonDialog(
            activity,
            activity.getString(R.string.no_internet_timeline_screen_title),
            activity.getString(R.string.no_internet_timeline_screen_body),
            activity.getString(R.string.ok)
        ) {
            null
        }
    }

    @JvmStatic
    fun checkInternetOrShow(context: Context): Boolean {
        return if (!NetworkConnection.isOnline(context)) {
            showNoInternetDialog(context)
            false
        } else {
            true
        }
    }
}
