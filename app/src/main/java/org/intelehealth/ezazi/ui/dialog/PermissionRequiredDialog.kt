package org.intelehealth.ezazi.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.databinding.LayoutPermissionDeniedDialogBinding

class PermissionRequiredDialog(
    private val dismissLabel: String?,
    private val submitLabel: String?,
    private val listener: OnActionClickListener
) : DialogFragment() {

    interface OnActionClickListener {
        fun onRetryClicked()
        fun onCloseClicked()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = LayoutPermissionDeniedDialogBinding.inflate(inflater, container, false)

        binding.dismissLabel = dismissLabel
        binding.submitLabel = submitLabel

        binding.btnDismiss.setOnClickListener {
            listener.onCloseClicked()
            dismiss()
        }

        binding.btnSubmit.setOnClickListener {
            Log.d("TAG", "onCreateView: retry")
            dismiss()
            listener.onRetryClicked()

        }

        return binding.root
    }
}