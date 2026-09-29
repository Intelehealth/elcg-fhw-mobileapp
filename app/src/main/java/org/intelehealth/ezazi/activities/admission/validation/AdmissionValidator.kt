package org.intelehealth.ezazi.activities.admission.validation

import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.utilities.GregorianDateUtils.GREG_FMT
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The Save-time rules, ported from PatientOtherInfoFragment.areValidFields() with the rule order and
 * the accumulating (non-short-circuiting) behaviour preserved. Returns what failed; painting is the
 * caller's job.
 */
object AdmissionValidator {

    private const val ADMISSION_FLOOR_SUFFIX = " (max 10 days ago)"

    fun validate(form: AdmissionForm): List<Failure> {
        val failures = mutableListOf<Failure>()

        if (form.admissionDate.isEmpty()) {
            failures += Failure(AdmissionField.ADMISSION_DATE, R.string.select_admission_date)
        } else {
            val admDate = parseGregDate(form.admissionDate)
            if (admDate == null || isAfterToday(form.admissionDate)) {
                failures += Failure(AdmissionField.ADMISSION_DATE, R.string.select_admission_date)
            } else {
                val minAdm = startOfDay(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -10) })
                if (admDate.before(minAdm.time)) {
                    failures += Failure(
                        AdmissionField.ADMISSION_DATE,
                        R.string.select_admission_date,
                        ADMISSION_FLOOR_SUFFIX
                    )
                }
            }
        }

        if (form.admissionTime.isBlank()) {
            failures += Failure(AdmissionField.ADMISSION_TIME, R.string.select_admission_time)
        } else if (form.admissionDate.isNotEmpty()) {
            val admDt = parseGregDateTime(form.admissionDate, form.admissionTime)
            if (admDt != null && admDt.after(Date())) {
                failures += Failure(AdmissionField.ADMISSION_TIME, R.string.select_admission_time)
            }
        }

        if (form.selectedRuptureMembrane.equals("Known", ignoreCase = true)) {
            if (form.membraneRupturedDate.isEmpty()) {
                failures += Failure(AdmissionField.SAC_RUPTURED_DATE, R.string.select_sac_ruptured_date)
            } else if (isAfterToday(form.membraneRupturedDate)) {
                failures += Failure(
                    AdmissionField.SAC_RUPTURED_DATE,
                    R.string.sac_ruptured_future_not_allowed
                )
            }
            if (form.membraneRupturedTime.isBlank()) {
                failures += Failure(AdmissionField.SAC_RUPTURED_TIME, R.string.select_sac_ruptured_time)
            } else {
                val rupDt = parseGregDateTime(form.membraneRupturedDate, form.membraneRupturedTime)
                if (rupDt != null && rupDt.after(Date())) {
                    failures += Failure(AdmissionField.SAC_RUPTURED_TIME, R.string.select_sac_ruptured_time)
                }
            }
        }

        if (form.totalBirth.isEmpty()) {
            failures += Failure(AdmissionField.TOTAL_BIRTH, R.string.total_birth_count_val_txt)
        } else if (parseSafe(form.totalBirth) > 15) {
            failures += Failure(AdmissionField.TOTAL_BIRTH, R.string.total_birth_count_limit)
        }

        if (form.totalMiscarriage.isEmpty()) {
            failures += Failure(AdmissionField.TOTAL_MISCARRIAGE, R.string.total_miscarriage_count_val_txt)
        } else if (parseSafe(form.totalMiscarriage) > 8) {
            failures += Failure(AdmissionField.TOTAL_MISCARRIAGE, R.string.miscarriage_count_limit)
        }

        if (form.labourOnset.isEmpty()) {
            failures += Failure(AdmissionField.LABOUR_ONSET, R.string.labor_onset_val_txt)
        }

        if (form.activeLabourDiagnosedDate.isEmpty() || isAfterToday(form.activeLabourDiagnosedDate)) {
            failures += Failure(
                AdmissionField.LABOUR_DIAGNOSED_DATE,
                R.string.active_labor_diagnosed_date_val_txt
            )
        }

        if (form.activeLabourDiagnosedTime.isBlank()) {
            failures += Failure(
                AdmissionField.LABOUR_DIAGNOSED_TIME,
                R.string.active_labor_diagnosed_time_val_txt
            )
        } else if (form.activeLabourDiagnosedDate.isNotEmpty()) {
            val labDt = parseGregDateTime(form.activeLabourDiagnosedDate, form.activeLabourDiagnosedTime)
            if (labDt != null) {
                val min15h = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -15) }
                if (labDt.after(Date()) || labDt.before(min15h.time)) {
                    failures += Failure(
                        AdmissionField.LABOUR_DIAGNOSED_TIME,
                        R.string.active_labour_diagnosis
                    )
                }
            }
        }

        if (form.riskFactorsText.isEmpty()) {
            failures += Failure(AdmissionField.RISK_FACTORS, R.string.please_select_risk_factor)
        } else if (form.isOtherRiskFactorVisible && form.otherRiskFactorText.isEmpty()) {
            failures += Failure(AdmissionField.OTHER_RISK_FACTOR, R.string.error_other_risk)
        }

        if (form.hospitalMaternity.isEmpty()) {
            failures += Failure(AdmissionField.HOSPITAL_MATERNITY, R.string.hospital_matermnity_val_txt)
        } else if (!form.hospitalMaternity.equals("hospital", ignoreCase = true) &&
            !form.hospitalMaternity.equals("maternity", ignoreCase = true)
        ) {
            if (form.hospitalOtherText.isEmpty()) {
                failures += Failure(AdmissionField.HOSPITAL_OTHER, R.string.enter_hospital_other_error)
            }
        }

        if (form.lmpDate.isNotEmpty()) {
            val lmp = parseGregDate(form.lmpDate)
            if (lmp != null) {
                if (isAfterToday(form.lmpDate)) {
                    failures += Failure(AdmissionField.LMP, R.string.lmp_future_not_allowed)
                } else {
                    val min44 = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -44) }
                    if (lmp.before(min44.time)) {
                        failures += Failure(AdmissionField.LMP, R.string.lmp_range_invalid)
                    }
                }
            }
        } else {
            failures += Failure(AdmissionField.LMP, R.string.select_lmp_date)
        }

        if (form.primaryDoctorText.isEmpty()) {
            failures += Failure(AdmissionField.PRIMARY_DOCTOR, R.string.select_primary_doctor)
        }

        if (form.ruptureMembraneText.isEmpty()) {
            failures += Failure(AdmissionField.RUPTURE_MEMBRANE, R.string.select_rupture_membrane)
        }

        return failures
    }

    /** Runs only after [validate] passes, and only when the parity-against-age warning did not fire. */
    fun validateGravida(form: AdmissionForm): Failure? {
        if (form.gravida.isEmpty()) {
            return Failure(AdmissionField.GRAVIDA, R.string.error_gravida_required)
        }
        val gravida = parseSafe(form.gravida)
        if (gravida < 0) return Failure(AdmissionField.GRAVIDA, R.string.error_gravida_negative)
        if (gravida > 20) return Failure(AdmissionField.GRAVIDA, R.string.error_gravida_max_limit)
        return null
    }

    private fun parseSafe(value: String): Int = value.toIntOrNull() ?: 0

    private fun startOfDay(cal: Calendar): Calendar = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /**
     * Device-local and lenient=false, matching the Java. Deliberately not GregorianDateUtils, which is
     * UTC-pinned: these comparisons run against a local Calendar and a UTC parse would shift the boundary.
     */
    private fun parseGregDate(dateStr: String?): Date? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            SimpleDateFormat(GREG_FMT, Locale.ENGLISH).apply { isLenient = false }.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun isAfterToday(dateStr: String?): Boolean {
        val parsed = parseGregDate(dateStr) ?: return false
        val today = startOfDay(Calendar.getInstance())
        val sel = startOfDay(Calendar.getInstance().apply { time = parsed })
        return sel.after(today)
    }

    private fun parseGregDateTime(dateStr: String?, timeStr: String?): Date? {
        if (dateStr.isNullOrEmpty() || timeStr.isNullOrEmpty()) return null
        val combined = dateStr.trim() + " " + timeStr.trim()
        for (fmt in arrayOf("dd/MM/yyyy hh:mm a", "dd/MM/yyyy HH:mm", "dd/MM/yyyy hh:mm")) {
            try {
                SimpleDateFormat(fmt, Locale.ENGLISH).apply { isLenient = false }.parse(combined)
                    ?.let { return it }
            } catch (ignored: Exception) {
            }
        }
        return null
    }
}
