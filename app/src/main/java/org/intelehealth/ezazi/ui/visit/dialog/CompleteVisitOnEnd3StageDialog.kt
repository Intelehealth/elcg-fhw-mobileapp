package org.intelehealth.ezazi.ui.visit.dialog

import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.databinding.LayoutEndStage3Binding
import org.intelehealth.ezazi.utilities.Utils
import org.intelehealth.ezazi.utilities.UuidDictionary
import org.intelehealth.ezazi.utilities.exception.DAOException

class CompleteVisitOnEnd3StageDialog(
    context: Context,
    visitUuid: String,
    private val listener: OnVisitCompleteListener
) : ReferTypeHelper(context, visitUuid) {

    private lateinit var binding: LayoutEndStage3Binding

    private var womenOption: String = ""
    private var newbornOption: String = ""

    companion object {
        private const val TAG = "CompleteVisitDialog"
    }

    interface OnVisitCompleteListener {
        fun onVisitCompleted()
    }

    fun buildDialog() {
        Log.e(TAG, "buildDialog: visitId =>$visitId")

        binding = LayoutEndStage3Binding.inflate(inflater, null, true)

        setupWomenOptions()
        setupNewbornOptions()

        showCustomViewDialog(
            R.string.complete_case,
            R.string.save_button,
            R.string.cancel,
            binding.root
        ) { saveStage3Outcome() }
    }

    private fun saveStage3Outcome() {

        if (womenOption.isEmpty() && newbornOption.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.please_select_an_option),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val obsDAO = ObsDAO()
            val encounterUuid = insertVisitCompleteEncounter()

            if (!encounterUuid.isNullOrEmpty()) {

                // ---------------- Mother Save ----------------
                if (womenOption.isNotEmpty()) {

                    val conceptId =
                        if (womenOption == "Maternal death") {
                            UuidDictionary.MOTHER_DECEASED   // separate concept
                        } else {
                            UuidDictionary.REFER_TYPE   // common concept
                        }

                    obsDAO.insert_Obs(
                        encounterUuid,
                        sessionManager.creatorID,
                        womenOption,
                        conceptId
                    )
                }

                // ---------------- Newborn Save ----------------
                if (newbornOption.isNotEmpty()) {

                    val conceptId =
                        if (newbornOption == "Neonatal death") {
                            UuidDictionary.NEONATAL_DEATH   // separate concept
                        } else {
                            UuidDictionary.NEWBORN_DISCHARGE_TYPE  // common concept
                        }

                    obsDAO.insert_Obs(
                        encounterUuid,
                        sessionManager.creatorID,
                        newbornOption,
                        conceptId
                    )
                }

                listener.onVisitCompleted()
            }

        } catch (e: DAOException) {
            throw RuntimeException(e)
        }
    }

    private fun setupWomenOptions() {

        val womenOptions =
            context.resources.getStringArray(R.array.mother_status_options)

        val adapter = ArrayAdapter(
            context,
            R.layout.spinner_textview,
            womenOptions
        )

        binding.autotvWomenEndStage3Options.setDropDownBackgroundResource(
            R.drawable.rounded_corner_white_with_gray_stroke
        )

        binding.autotvWomenEndStage3Options.setAdapter(adapter)

        binding.autotvWomenEndStage3Options.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            womenOption = parent.getItemAtPosition(position).toString()
        }
    }

    private fun setupNewbornOptions() {

        val newbornOptions =
            context.resources.getStringArray(R.array.newborn_status_options)

        val adapter = ArrayAdapter(
            context,
            R.layout.spinner_textview,
            newbornOptions
        )

        binding.autotvNewbornEndStage3Options.setDropDownBackgroundResource(
            R.drawable.rounded_corner_white_with_gray_stroke
        )

        binding.autotvNewbornEndStage3Options.setAdapter(adapter)

        binding.autotvNewbornEndStage3Options.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            newbornOption = parent.getItemAtPosition(position).toString()
        }
    }
}