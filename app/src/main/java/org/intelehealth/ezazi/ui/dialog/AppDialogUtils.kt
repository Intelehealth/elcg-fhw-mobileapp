package org.intelehealth.ezazi.ui.dialog

import androidx.fragment.app.FragmentActivity

object AppDialogUtils {

    @JvmStatic
    fun showSingleButtonDialog(
        activity: FragmentActivity,
        title: String,
        message: String,
        positiveText: String,
        onPositiveClick: (() -> Unit)? = null
    ) {
        val dialog = ConfirmationDialogFragment.Builder(activity)
            .title(title)
            .content(message)
            .positiveButtonLabel(positiveText)
            .hideNegativeButton(true)
            .build()

        dialog.setListener(object :
            ConfirmationDialogFragment.OnConfirmationActionListener {

            override fun onAccept() {
                onPositiveClick?.invoke()
            }
        })

        dialog.show(
            activity.supportFragmentManager,
            dialog::class.java.name
        )
    }

    @JvmStatic
    fun showTwoButtonDialog(
        activity: FragmentActivity,
        title: String,
        message: String,
        positiveText: String,
        negativeText: String,
        onPositiveClick: (() -> Unit)? = null,
        onNegativeClick: (() -> Unit)? = null
    ) {
        val dialog = ConfirmationDialogFragment.Builder(activity)
            .title(title)
            .content(message)
            .positiveButtonLabel(positiveText)
            .negativeButtonLabel(negativeText)
            .build()

        dialog.setListener(object :
            ConfirmationDialogFragment.OnConfirmationActionListener {

            override fun onAccept() {
                onPositiveClick?.invoke()
            }

            override fun onDecline() {
                onNegativeClick?.invoke()
            }
        })

        dialog.show(
            activity.supportFragmentManager,
            dialog::class.java.name
        )
    }
}
