package org.intelehealth.ezazi.stage3.postpartum

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.app.IntelehealthApplication
import org.intelehealth.ezazi.partogram.PartogramConstants
import org.intelehealth.ezazi.stage3.Utils.DeliveryDetailsConcept
import org.intelehealth.ezazi.utilities.SessionManager
import org.intelehealth.ezazi.utilities.UuidDictionary
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds the JSON payload consumed by window.renderStage3() in
 * stage3.html (the offline Early-PostPartum report).
 *
 * Mirrors the data-shaping logic of the web Angular stage3.component.ts —
 * the resulting JSON's field names line up 1:1 with what the HTML
 * normalizeData() reads.
 *
 * Entry point: [transform] (suspend; runs DB work on [Dispatchers.IO]).
 */
object Stage3DataTransformer {

    private const val TAG = "Stage3Transformer"

    /**
     * Runtime check that mirrors the web Angular component's isNepalClient
     * logic (stage3.component.ts:31–33). Drives BS vs Gregorian formatting
     * inside stage3.html. Returns true if the configured OpenMRS server URL
     * contains "nepal" (case-insensitive) — same heuristic the web app uses
     * on globalThis.location.hostname.
     */
    private fun isNepalRegion(): Boolean {
        return SessionManager(IntelehealthApplication.getAppContext())
            .serverUrl.orEmpty().lowercase().contains("nepal")
    }

    // 8 fixed sub-columns matching stage3.component.ts COL_OFFSETS / COL_LABELS.
    private const val NUM_COLS = 8
    private val COL_LABELS = listOf(
        "0 min", "+15m", "+30m", "+45m", "+1h 15m", "+1h 45m", "+2h 45m", "+3h 45m"
    )

    // Encounter-type UUIDs for the 8 Stage 3 hourly slots (Stage3_Hour1_1 … Hour4_1).
    // Seeded in InteleHealthDatabaseHelper.uuidInsert() — but onUpgrade() does NOT
    // re-run that seed, so devices upgraded from older builds may have an empty
    // tbl_uuid_dictionary for these rows. Matching by UUID directly bypasses
    // that JOIN-based lookup entirely.
    private val STAGE3_HOUR_TYPE_UUIDS = listOf(
        "e58b22d1-b09e-4cda-b05c-a45d1e63d390", // Stage3_Hour1_1
        "2c112cc2-3922-400e-9dd9-11b27fc76404", // Stage3_Hour1_2
        "83ddb485-4d99-4efa-b7c9-8a2aab14c868", // Stage3_Hour1_3
        "45d3680e-b368-4a05-8ae5-e0baa31b1973", // Stage3_Hour1_4
        "3d71ad42-7b6e-4687-b096-f0dd89e84647", // Stage3_Hour2_1
        "3b3960f4-c9d2-4d41-aed9-f376312fc33e", // Stage3_Hour2_2
        "0ca40a73-3581-47ae-9cb2-0c6cde17158e", // Stage3_Hour3_1
        "746e8cc9-e8e8-4055-87a2-2687888cf7f8"  // Stage3_Hour4_1
    )

    // Concept-UUID → param-index for maternalParams (order MUST match the
    // maternalParams[] array in stage3.html).
    private val MATERNAL_UUID_TO_IDX: Map<String, Int> = mapOf(
        PartogramConstants.Params.PULSE.conceptId                       to 0,  // Pulse
        PartogramConstants.Params.SYSTOLIC_BP.conceptId                 to 1,  // BP (systolic half)
        PartogramConstants.Params.DIASTOLIC_BP.conceptId                to 1,  // BP (diastolic half)
        PartogramConstants.Params.RESPIRATORY_RATE_MOTHER.conceptId     to 2,  // Respiratory Rate
        PartogramConstants.Params.TEMPERATURE.conceptId                 to 3,  // Temperature
        PartogramConstants.Params.BLOOD_LOSS_MOTHER.conceptId           to 4,
        PartogramConstants.Params.UTERUS_CONTRACTED_MOTHER.conceptId    to 5,
        PartogramConstants.Params.URINE_PASSED_MOTHER.conceptId         to 6,
        PartogramConstants.Params.HEMATOMA_MOTHER.conceptId             to 7,
        PartogramConstants.Params.ONGOING_COMPLICATIONS_MOTHER.conceptId to 8,
        PartogramConstants.Params.ASSESSMENT_MOTHER.conceptId           to 9,
        PartogramConstants.Params.PLAN_MOTHER.conceptId                 to 10
    )

    private val NEWBORN_UUID_TO_IDX: Map<String, Int> = mapOf(
        PartogramConstants.Params.RESPIRATORY_RATE_NEWBORN.conceptId    to 0,
        PartogramConstants.Params.SPO2_NEWBORN.conceptId                to 1,
        PartogramConstants.Params.GRUNTING_NEWBORN.conceptId            to 2,
        PartogramConstants.Params.CHEST_INDRAWING_NEWBORN.conceptId     to 3,
        PartogramConstants.Params.FAST_BREATHING_NEWBORN.conceptId      to 4,
        PartogramConstants.Params.FEET_WARM_NEWBORN.conceptId           to 5,
        PartogramConstants.Params.SKIN_COLOR_NEWBORN.conceptId          to 6,
        PartogramConstants.Params.UC_OOZING_NEWBORN.conceptId           to 7,
        PartogramConstants.Params.SUCKING_FEEDING_NEWBORN.conceptId     to 8,
        PartogramConstants.Params.ONGOING_COMPLICATIONS_NEWBORN.conceptId to 9,
        PartogramConstants.Params.ASSESSMENT_NEWBORN.conceptId          to 10,
        PartogramConstants.Params.PLAN_NEWBORN.conceptId                to 11
    )

    private val MATERNAL_PARAM_NAMES = listOf(
        "Pulse", "BP", "Respiratory Rate", "Temperature", "Blood Loss",
        "Uterus Contracted", "Urine Passed", "Hematoma", "Complication",
        "Assessment (Mother)", "Plan (Mother)"
    )
    private val MATERNAL_TEXTAREA_IDX = setOf(9, 10)

    private val NEWBORN_PARAM_NAMES = listOf(
        "Respiratory Rate", "SPO2", "Grunting", "Chest Indrawing", "Fast Breathing",
        "Feet (warm)", "Skin Color", "Umbilical Cord Oozing", "Sucking / Feeding",
        "Complications", "Assessment (Newborn)", "Plan (Newborn)"
    )
    private val NEWBORN_TEXTAREA_IDX = setOf(10, 11)

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Returns the JSON string consumed by window.renderStage3() in stage3.html.
     * Returns null on fatal error so the activity can show an error dialog.
     */
    suspend fun transform(visitUuid: String): String? = withContext(Dispatchers.IO) {
        try {
            val db = AppConstants.inteleHealthDatabaseHelper.writeDb
            val root = JSONObject()
            root.put("isNepalClient", isNepalRegion())

            val patientUuid = queryPatientUuid(db, visitUuid) ?: ""
            root.put("pinfo", buildPinfo(db, patientUuid))
            root.put("deliveryOutcome", buildDeliveryOutcome(db, visitUuid))

            root.put("colLabels", JSONArray(COL_LABELS))
            buildStageData(db, visitUuid, root)

            root.toString()
        } catch (e: Exception) {
            Log.e(TAG, "transform failed", e)
            null
        }
    }

    // ── Patient lookup ──────────────────────────────────────────────────────

    @SuppressLint("Range")
    private fun queryPatientUuid(db: SQLiteDatabase, visitUuid: String): String? {
        return db.rawQuery(
            "SELECT patientuuid FROM tbl_visit WHERE uuid = ?",
            arrayOf(visitUuid)
        ).use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }

    @SuppressLint("Range")
    private fun buildPinfo(db: SQLiteDatabase, patientUuid: String): JSONObject {
        val out = JSONObject()
        db.rawQuery(
            "SELECT first_name, middle_name, last_name, openmrs_id FROM tbl_patient WHERE uuid = ?",
            arrayOf(patientUuid)
        ).use { c ->
            if (c.moveToFirst()) {
                val fn = c.getString(c.getColumnIndex("first_name")).orEmpty()
                val mn = c.getString(c.getColumnIndex("middle_name")).orEmpty()
                val ln = c.getString(c.getColumnIndex("last_name")).orEmpty()
                val name = "$fn $mn $ln".trim().replace(Regex("\\s+"), " ")
                out.put("name", name)
                out.put("openMrsId", c.getString(c.getColumnIndex("openmrs_id")).orEmpty())
            }
        }
        return out
    }

    // ── Delivery outcome ─────────────────────────────────────────────────────
    // Pulls obs from the DELIVERY_OUTCOME_STAGE3 encounter (or falls back to
    // the Visit Complete encounter, mirroring stage3.component.ts:readDeliveryOutcome).

    @SuppressLint("Range")
    private fun buildDeliveryOutcome(db: SQLiteDatabase, visitUuid: String): JSONObject {
        val out = JSONObject()
        val (encounterUuid, encounterTime) = findDeliveryOutcomeEncounter(db, visitUuid)
            ?: return out

        val obs = readObs(db, encounterUuid)

        out.put("deliveryDate", toIsoOrRaw(obs[DeliveryDetailsConcept.DATE_OF_DELIVERY.uuid]
            ?: encounterTime))
        out.put("deliveryTime", toIsoOrRaw(obs[DeliveryDetailsConcept.TIME_OF_DELIVERY.uuid]
            ?: encounterTime))
        out.put("deliveryMode", textOrDash(obs[DeliveryDetailsConcept.MODE_OF_DELIVERY.uuid]))
        out.put("placentaMembraneDelivery", textOrDash(obs[DeliveryDetailsConcept.PLACENTA_MEMBRANE_STATUS.uuid]))
        out.put("placentaDeliveryTime", toIsoOrRaw(obs[DeliveryDetailsConcept.PLACENTA_DELIVERY_TIME.uuid].orEmpty()))
        out.put("placentaCordAbnormality", textOrDash(obs[DeliveryDetailsConcept.PLACENTA_CORD_ABNORMALITY.uuid]))
        out.put("perinealLaceration", textOrDash(obs[DeliveryDetailsConcept.PERINEAL_TEAR.uuid]))
        out.put("degreeOfTear", textOrDash(obs[DeliveryDetailsConcept.DEGREE_OF_PERINEAL_TEAR.uuid]))
        out.put("amtslMedication", formatAmtslMedication(obs[DeliveryDetailsConcept.MEDICATIONS_AMTSL.uuid]))
        out.put("babyStatus", textOrDash(obs[DeliveryDetailsConcept.BIRTH_TYPE.uuid]))
        out.put("babyGender", textOrDash(obs[DeliveryDetailsConcept.BABY_GENDER.uuid]))
        out.put("babyWeight", textOrDash(obs[DeliveryDetailsConcept.BIRTH_WEIGHT.uuid]))

        val a1 = obs[DeliveryDetailsConcept.APGAR_SCORE_1_MIN.uuid].orEmpty()
        val a5 = obs[DeliveryDetailsConcept.APGAR_SCORE_5_MIN.uuid].orEmpty()
        out.put("apgarScore", if (a1.isEmpty() && a5.isEmpty()) "-" else "${a1.ifEmpty { "-" }}/${a5.ifEmpty { "-" }}")

        out.put("resuscitation", textOrDash(obs[DeliveryDetailsConcept.RESUSCITATION.uuid]))
        out.put("skinToSkin", textOrDash(obs[DeliveryDetailsConcept.SKIN_CONTACT.uuid]))
        out.put("breastfeedingInOneHour", textOrDash(obs[DeliveryDetailsConcept.BREASTFED_FIRSTHOUR.uuid]))
        out.put("congenitalDisorders", formatCongenitalDisorders(obs[DeliveryDetailsConcept.CONGENITAL_ANOMALY.uuid]))

        return out
    }

    /**
     * Returns (encounterUuid, encounterTime) of the DELIVERY_OUTCOME_STAGE3
     * encounter, falling back to the Visit Complete encounter.
     */
    @SuppressLint("Range")
    private fun findDeliveryOutcomeEncounter(db: SQLiteDatabase, visitUuid: String): Pair<String, String>? {
        val candidates = listOf(
            UuidDictionary.DELIVERY_OUTCOME_STAGE3,
            UuidDictionary.ENCOUNTER_VISIT_COMPLETE
        )
        for (typeUuid in candidates) {
            db.rawQuery(
                "SELECT uuid, encounter_time FROM tbl_encounter " +
                "WHERE visituuid = ? AND encounter_type_uuid = ? " +
                "  AND (voided = '0' OR voided = 'false' OR voided = 'FALSE') LIMIT 1",
                arrayOf(visitUuid, typeUuid)
            ).use { c ->
                if (c.moveToFirst()) {
                    return c.getString(c.getColumnIndex("uuid")).orEmpty() to
                            c.getString(c.getColumnIndex("encounter_time")).orEmpty()
                }
            }
        }
        return null
    }

    /** Returns conceptUuid → value for all non-voided obs on the encounter. */
    @SuppressLint("Range")
    private fun readObs(db: SQLiteDatabase, encounterUuid: String): Map<String, String> {
        val map = HashMap<String, String>()
        db.rawQuery(
            "SELECT conceptuuid, value FROM tbl_obs " +
            "WHERE encounteruuid = ? AND (voided = '0' OR voided = 'false' OR voided = 'FALSE')",
            arrayOf(encounterUuid)
        ).use { c ->
            while (c.moveToNext()) {
                val k = c.getString(c.getColumnIndex("conceptuuid")).orEmpty()
                val v = c.getString(c.getColumnIndex("value")).orEmpty()
                if (k.isNotEmpty()) map[k] = v
            }
        }
        return map
    }

    // ── Stage 3 hourly grid ──────────────────────────────────────────────────

    @SuppressLint("Range")
    private fun buildStageData(db: SQLiteDatabase, visitUuid: String, root: JSONObject) {
        val maternalValues = Array(MATERNAL_PARAM_NAMES.size) { idx ->
            if (idx in MATERNAL_TEXTAREA_IDX) Array<Any?>(NUM_COLS) { JSONArray() }
            else arrayOfNulls<Any?>(NUM_COLS)
        }
        val newbornValues = Array(NEWBORN_PARAM_NAMES.size) { idx ->
            if (idx in NEWBORN_TEXTAREA_IDX) Array<Any?>(NUM_COLS) { JSONArray() }
            else arrayOfNulls<Any?>(NUM_COLS)
        }
        val colTimes = arrayOfNulls<String>(NUM_COLS)

        val encounters = fetchStage3Encounters(db, visitUuid)
        for ((idx, enc) in encounters.withIndex()) {
            if (idx >= NUM_COLS) break
            colTimes[idx] = enc.second
            fillObsForEncounter(db, enc.first, idx, maternalValues, newbornValues)
        }

        root.put("colTimes", stringArrayToJson(colTimes))
        root.put("maternalParams", buildParamsJson(MATERNAL_PARAM_NAMES, MATERNAL_TEXTAREA_IDX, maternalValues))
        root.put("newbornParams",  buildParamsJson(NEWBORN_PARAM_NAMES,  NEWBORN_TEXTAREA_IDX,  newbornValues))
    }

    /** Returns ordered list of (encounterUuid, isoEncounterTime) for Stage3_Hour* encounters. */
    @SuppressLint("Range")
    private fun fetchStage3Encounters(db: SQLiteDatabase, visitUuid: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val placeholders = STAGE3_HOUR_TYPE_UUIDS.joinToString(",") { "?" }
        val args = arrayOf(visitUuid) + STAGE3_HOUR_TYPE_UUIDS.toTypedArray()
        db.rawQuery(
            "SELECT uuid, encounter_time FROM tbl_encounter " +
            "WHERE visituuid = ? " +
            "  AND encounter_type_uuid IN ($placeholders) " +
            "  AND (voided = '0' OR voided = 'false' OR voided = 'FALSE') " +
            "ORDER BY encounter_time ASC",
            args
        ).use { c ->
            while (c.moveToNext()) {
                out += c.getString(c.getColumnIndex("uuid")).orEmpty() to
                        toIsoOrRaw(c.getString(c.getColumnIndex("encounter_time")).orEmpty())
            }
        }
        return out
    }

    @SuppressLint("Range")
    private fun fillObsForEncounter(
        db: SQLiteDatabase, encounterUuid: String, colIdx: Int,
        maternalValues: Array<Array<Any?>>, newbornValues: Array<Array<Any?>>
    ) {
        db.rawQuery(
            "SELECT conceptuuid, value FROM tbl_obs " +
            "WHERE encounteruuid = ? AND (voided = '0' OR voided = 'false' OR voided = 'FALSE')",
            arrayOf(encounterUuid)
        ).use { c ->
            while (c.moveToNext()) {
                val conceptUuid = c.getString(c.getColumnIndex("conceptuuid")).orEmpty()
                val value = c.getString(c.getColumnIndex("value")).orEmpty()

                MATERNAL_UUID_TO_IDX[conceptUuid]?.let { paramIdx ->
                    assignMaternalObs(paramIdx, conceptUuid, value, colIdx, maternalValues)
                    return@let
                }
                NEWBORN_UUID_TO_IDX[conceptUuid]?.let { paramIdx ->
                    assignObs(paramIdx, value, colIdx, newbornValues, NEWBORN_TEXTAREA_IDX,
                              isComplication = paramIdx == 9)
                }
            }
        }
    }

    /**
     * Handles maternal BP specially: systolic + diastolic obs land on the same
     * row (idx 1) and are joined into a single "120/80" string. Everything else
     * routes through [assignObs].
     */
    private fun assignMaternalObs(
        paramIdx: Int, conceptUuid: String, value: String,
        colIdx: Int, maternalValues: Array<Array<Any?>>
    ) {
        if (paramIdx == 1) {
            val existing = (maternalValues[1][colIdx] as? JSONObject)?.optString("value", "") ?: ""
            val parts = existing.split("/").let { if (it.size < 2) listOf(it.firstOrNull().orEmpty(), "") else it }
            val sys = if (conceptUuid == PartogramConstants.Params.SYSTOLIC_BP.conceptId) value else parts[0]
            val dia = if (conceptUuid == PartogramConstants.Params.DIASTOLIC_BP.conceptId) value else parts[1]
            val combined = if (sys.isNotEmpty() && dia.isNotEmpty()) "$sys/$dia" else (sys + dia)
            maternalValues[1][colIdx] = JSONObject().put("value", combined)
            return
        }
        assignObs(paramIdx, value, colIdx, maternalValues, MATERNAL_TEXTAREA_IDX,
                  isComplication = paramIdx == 8)
    }

    private fun assignObs(
        paramIdx: Int, value: String, colIdx: Int,
        store: Array<Array<Any?>>, textareaIdxs: Set<Int>, isComplication: Boolean
    ) {
        if (paramIdx in textareaIdxs) {
            val arr = store[paramIdx][colIdx] as? JSONArray ?: JSONArray().also { store[paramIdx][colIdx] = it }
            arr.put(JSONObject().put("value", value))
        } else {
            val displayValue = if (isComplication) formatComplication(value) else value
            store[paramIdx][colIdx] = JSONObject().put("value", displayValue)
        }
    }

    private fun buildParamsJson(
        names: List<String>, textareaIdxs: Set<Int>, values: Array<Array<Any?>>
    ): JSONArray {
        val out = JSONArray()
        for (i in names.indices) {
            val isTextarea = i in textareaIdxs
            val valArr = JSONArray()
            for (c in 0 until NUM_COLS) {
                val cell = values[i][c]
                when {
                    cell == null -> valArr.put(JSONObject.NULL)
                    isTextarea -> valArr.put(cell as JSONArray)
                    else -> valArr.put(cell as JSONObject)
                }
            }
            out.put(JSONObject().apply {
                put("name", names[i])
                put("isTextarea", isTextarea)
                put("values", valArr)
            })
        }
        return out
    }

    // ── JSON-value normalisers (ports of stage3.component.ts helpers) ────────

    /**
     * AMTSL Medication may arrive as:
     *   - a comma-separated string ("Oxytocin, Misoprostol")
     *   - a JSON array (["Oxytocin", "Misoprostol"])
     *   - a JSON object ({"MEDICATIONS_AMTSL": "Oxytocin, Misoprostol"})
     * Output: newline-separated list ("Oxytocin\nMisoprostol") or "-".
     */
    private fun formatAmtslMedication(raw: String?): String {
        if (raw.isNullOrEmpty()) return "-"
        val text = extractMedicationText(raw) ?: return "-"
        return text.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .ifEmpty { "-" }
    }

    private fun extractMedicationText(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("{")) {
            try {
                val obj = JSONObject(trimmed)
                val text = obj.optString("MEDICATIONS_AMTSL").ifEmpty {
                    obj.optString("Medications_AMTSL").ifEmpty {
                        obj.optString("AMTSL Medication")
                    }
                }
                if (text.isNotEmpty()) return text
            } catch (_: JSONException) {}
        }
        if (trimmed.startsWith("[")) {
            try {
                val arr = JSONArray(trimmed)
                return (0 until arr.length()).joinToString(", ") { arr.optString(it) }
            } catch (_: JSONException) {}
        }
        return trimmed
    }

    /** "Ongoing complications" may be plain Y/N or a JSON object {complications: "..."}. */
    private fun formatComplication(raw: String?): String {
        if (raw.isNullOrEmpty()) return "-"
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) {
            try {
                val obj = JSONObject(trimmed)
                val txt = obj.optString("complications").ifEmpty { obj.optString("Complications") }
                return if (txt.isNotEmpty()) txt else "-"
            } catch (_: JSONException) {}
        }
        return trimmed
    }

    /** Congenital disorders may be plain text, a JSON array, or {CONGENITAL_ANOMALY: [...], other_text: "..."}. */
    private fun formatCongenitalDisorders(raw: String?): String {
        if (raw.isNullOrEmpty()) return "-"
        val trimmed = raw.trim()
        val items = mutableListOf<String>()
        when {
            trimmed.startsWith("{") -> try {
                val obj = JSONObject(trimmed)
                val arr = obj.optJSONArray("CONGENITAL_ANOMALY") ?: obj.optJSONArray("congenital_anomaly")
                arr?.let { (0 until it.length()).forEach { i -> items.add(it.optString(i)) } }
                obj.optString("other_text").takeIf { it.isNotEmpty() }?.let { items.add(it) }
            } catch (_: JSONException) {}
            trimmed.startsWith("[") -> try {
                val arr = JSONArray(trimmed)
                (0 until arr.length()).forEach { items.add(arr.optString(it)) }
            } catch (_: JSONException) {}
            else -> trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { items.add(it) }
        }
        val cleaned = items.filter { it.isNotBlank() }
        return if (cleaned.isEmpty()) "-" else cleaned.joinToString(", ")
    }

    // ── Utility helpers ──────────────────────────────────────────────────────

    private fun textOrDash(value: String?): String =
        if (value.isNullOrEmpty()) "-" else value

    /**
     * Tries multiple parse formats (matching stage3.component.ts:parseIncomingDate)
     * and returns an ISO 8601 UTC timestamp. If parsing fails, returns the raw
     * input — the HTML's fmtDateYMD/fmtTimeOnly/fmtDateTime helpers pass through
     * unparseable strings, so a worst-case scenario shows the raw DB value.
     */
    private fun toIsoOrRaw(raw: String): String {
        if (raw.isEmpty()) return raw
        if ((raw.contains('T') && (raw.endsWith('Z') || raw.contains('+'))) ) return raw

        val outFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val parsers = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "dd/MM/yyyy hh:mm a",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "yyyy-MM-dd"
        )
        for (pattern in parsers) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }
                val parsed: Date? = sdf.parse(raw)
                if (parsed != null) return outFmt.format(parsed)
            } catch (_: Exception) {}
        }
        return raw
    }

    private fun stringArrayToJson(arr: Array<String?>): JSONArray {
        val out = JSONArray()
        for (v in arr) if (v != null) out.put(v) else out.put(JSONObject.NULL)
        return out
    }
}
