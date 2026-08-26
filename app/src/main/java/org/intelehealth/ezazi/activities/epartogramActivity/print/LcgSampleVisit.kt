package org.intelehealth.ezazi.activities.epartogramActivity.print

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * A fully populated fake visit using EpartogramDataTransformer's own key
 * names — pInfo, patientName, stage1/stage2 — so the renderer is exercised against the
 * real contract rather than a convenient invention. Swapping this for a live
 * visit is a one-line change at the call site.
 *
 * Every observation slot carries a time, which is the worst case for width:
 * 30 columns in first stage (two per hour) and 20 in second (four per hour).
 */
object LcgSampleVisit {

    private const val STAGE1_HOURS = 15
    private const val STAGE2_HOURS = 5
    private const val STAGE1_SUBS = 2
    private const val STAGE2_SUBS = 4

    private const val PARAM_ASSESSMENT = 22
    private const val PARAM_PLAN = 23

    private const val BASE_MILLIS = 1705293000000L
    private const val MINUTE = 60000L

    /**
     * Matches the transformer exactly: ISO 8601 in UTC with a trailing Z. The
     * suffix matters — a fixture without it hides timezone bugs that only
     * appear against real data.
     */
    private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun at(minutes: Long): String = ISO.format(Date(BASE_MILLIS + minutes * MINUTE))

    fun build(): JSONObject {
        val total1 = STAGE1_HOURS * STAGE1_SUBS
        val total2 = STAGE2_HOURS * STAGE2_SUBS

        val root = JSONObject()
        root.put("pInfo", buildPinfo())
        root.put("subColsPerHourStage1", intArray(STAGE1_HOURS, STAGE1_SUBS))
        root.put("subColsPerHourStage2", intArray(STAGE2_HOURS, STAGE2_SUBS))
        root.put("totalStage1Cols", total1)
        root.put("totalStage2Cols", total2)

        val times1 = JSONArray()
        for (i in 0 until total1) times1.put(at(i * 30L))
        val times2 = JSONArray()
        for (i in 0 until total2) times2.put(at(STAGE1_HOURS * 60L + i * 15L))
        root.put("timeFullStage1", times1)
        root.put("timeFullStage2", times2)

        root.put("initialsStage1", stringArray(STAGE1_HOURS) { if (it < 8) "JD" else "AS" })
        root.put("initialsStage2", stringArray(STAGE2_HOURS) { "AS" })

        root.put("sosEncounterUUIDs", JSONArray().put("enc1-18"))
        root.put("encuuid1Full", encArray(total1, "enc1"))
        root.put("encuuid2Full", encArray(total2, "enc2"))

        root.put("parameters", buildParameters(total1, total2))
        root.put("visitCompleted", false)

        root.put("assessmentHistory", historyOf(ASSESSMENTS))
        root.put("planHistory", historyOf(PLANS))
        root.put("medicationPrescribedHistory", historyOf(MEDICINES))
        return root
    }

    private fun buildPinfo() = JSONObject().apply {
        put("patientName", "सुनिता तामाङ / Sunita Tamang")
        put("openmrsId", "NEP-2083-0412")
        put("Parity", "P1")
        put("Gravida", "G2")
        put("LaborOnset", "Spontaneous")
        put("ActiveLaborDiagnosed", at(-120))
        put("MembraneRupturedTimestamp", at(-45))
        put("Riskfactors", "None")
        put("LMP", "15/04/2023")
        put("EDD", "20/01/2024")
    }

    private fun intArray(count: Int, value: Int) = JSONArray().apply {
        repeat(count) { put(value) }
    }

    private fun stringArray(count: Int, value: (Int) -> String) = JSONArray().apply {
        for (i in 0 until count) put(value(i))
    }

    private fun encArray(count: Int, prefix: String) = JSONArray().apply {
        for (i in 0 until count) put(JSONObject().put("enc_uuid", "$prefix-$i"))
    }

    private fun historyOf(values: List<String>) = JSONArray().apply {
        values.forEachIndexed { i, text ->
            put(
                JSONObject()
                    .put("value", text)
                    .put("initial", if (i % 2 == 0) "JD" else "AS")
                    .put("obsDatetime", at(i * 180L))
            )
        }
    }

    private val ASSESSMENTS = listOf(
        "Labour progressing normally, cervix 5 cm, contractions regular.",
        "FHR briefly elevated to 168 after position change, settled within 10 minutes.",
        "Cervix 8 cm, descent 2/5, membranes ruptured, clear liquor.",
        "Active second stage, maternal effort good, head visible on contraction."
    )

    private val PLANS = listOf(
        "Continue half-hourly observations, encourage mobility and oral fluids.",
        "Left lateral position, repeat FHR in 15 minutes, inform senior midwife.",
        "Prepare delivery trolley, continue monitoring, anticipate delivery within the hour.",
        "Prepare for delivery, paediatric team on standby."
    )

    private val MEDICINES = listOf(
        "Paracetamol 500 mg oral",
        "Oxytocin 10 U/L at 20 drops per minute"
    )

    private fun buildParameters(total1: Int, total2: Int): JSONArray {
        val params = JSONArray()
        for (idx in 0 until 29) {
            params.put(
                JSONObject()
                    .put("stage1", valuesFor(idx, 1, total1))
                    .put("stage2", valuesFor(idx, 2, total2))
            )
        }
        return params
    }

    private fun valuesFor(paramIdx: Int, stage: Int, total: Int): JSONArray {
        val arr = JSONArray()
        for (col in 0 until total) {
            val cell = sampleCell(paramIdx, stage, col)
            if (cell == null) arr.put(JSONObject.NULL) else arr.put(cell)
        }
        return arr
    }

    private fun obs(value: String, comment: String? = null) = JSONObject().apply {
        put("value", value)
        if (comment != null) put("comment", comment)
    }

    private fun sampleCell(paramIdx: Int, stage: Int, col: Int): JSONObject? {
        val hourly = col % 2 == 0
        return when (paramIdx) {
            0 -> if (hourly) obs("Y") else null
            1 -> if (hourly) obs(if (col % 4 == 0) "N" else "D") else null
            2 -> if (hourly) obs("Y") else null
            3 -> if (hourly) obs(if (col % 4 == 0) "MO" else "SP") else null
            4 -> {
                val fhr = 132 + (col * 7) % 34
                obs(fhr.toString(), if (fhr >= 160) "R" else if (fhr >= 155) "Y" else null)
            }
            5 -> if (col % 3 == 0) obs(if (col % 6 == 0) "E" else "L", if (col % 6 == 3) "R" else null) else null
            6 -> if (hourly) obs(if (col < 6) "I" else "C") else null
            7 -> if (hourly) obs(if (stage == 2) "A" else "P") else null
            8 -> if (hourly) obs(if (col > 16) "++" else "+") else null
            9 -> if (hourly) obs("+") else null
            10 -> if (hourly) obs((84 + col % 14).toString()) else null
            11 -> if (hourly) obs((112 + col % 16).toString()) else null
            12 -> if (hourly) obs((72 + col % 12).toString()) else null
            13 -> if (hourly) obs(if (col % 6 == 0) "37.1" else "36.8") else null
            14 -> if (hourly) obs("Nil") else null
            15 -> obs((3 + col % 3).toString())
            16 -> obs((30 + (col % 4) * 10).toString())
            17 -> cervixCell(stage, col)
            18 -> descentCell(stage, col)
            19 -> if (col % 8 == 6) oxytocin(col) else null
            20 -> if (col % 6 == 4) medicineList(col) else null
            21 -> if (col % 8 == 2) ivFluid(col) else null
            25 -> if (hourly) obs("Nil") else null
            PARAM_ASSESSMENT -> narrative(ASSESSMENTS, stage, col)
            PARAM_PLAN -> narrative(PLANS, stage, col)
            else -> null
        }
    }

    /**
     * Shared-decision entries land on the grid, one every three hours, so the
     * sideways wrapping in the cells is exercised rather than only the notes
     * page.
     */
    private fun narrative(source: List<String>, stage: Int, col: Int): JSONObject? {
        val slot = if (stage == 1) col / 6 else col / 8
        if (stage == 1 && col % 6 != 0) return null
        if (stage == 2 && col % 8 != 0) return null
        val text = source.getOrNull(slot) ?: return null
        return JSONObject().apply {
            put("value", text)
            put("initial", if (slot % 2 == 0) "JD" else "AS")
            put("obsDatetime", at(slot * 180L))
        }
    }

    private fun cervixCell(stage: Int, col: Int): JSONObject? {
        if (stage == 2) return if (col == 0) obs("P") else null
        if (col % 4 != 0) return null
        val level = 5 + col / 4
        return if (level in 5..10) obs(level.toString()) else null
    }

    private fun descentCell(stage: Int, col: Int): JSONObject? {
        if (stage == 2) return if (col == 0) obs("0") else null
        if (col % 5 != 0) return null
        val level = 5 - col / 5
        return if (level in 0..5) obs(level.toString()) else null
    }

    private fun oxytocin(col: Int) = JSONObject().apply {
        put(
            "value", JSONObject()
                .put("strength", "10")
                .put("infusionRate", (20 + col).toString())
                .put("infusionStatus", if (col > 12) "Continued" else "Started")
        )
        put("initial", "JD")
        put("obsDatetime", at(col * 30L))
    }

    private fun ivFluid(col: Int) = JSONObject().apply {
        val types = listOf("Normal Saline", "Ringer Lactate", "Dextrose 5% (D5)")
        put(
            "value", JSONObject()
                .put("type", types[(col / 8) % types.size])
                .put("infusionRate", (45 + col).toString())
                .put("infusionStatus", if (col > 10) "Continued" else "Started")
        )
        put("initial", "MM")
        put("obsDatetime", at(col * 30L))
    }

    private fun medicineList(col: Int) = JSONObject().apply {
        val drugs = listOf(
            "Paracetamol 650 mg::1 Tab Oral",
            "Metronidazole 400 mg::1 Tab Oral",
            "Paracetamol 500 mg::2 Tab Oral"
        )
        put("value", drugs[(col / 6) % drugs.size])
        put("initial", "MM")
        put("obsDatetime", at(col * 30L))
    }

}
