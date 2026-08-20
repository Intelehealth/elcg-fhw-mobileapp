package org.intelehealth.ezazi.activities.epartogramActivity

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.utilities.UuidDictionary
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

/**
 * Queries the local SQLite DB for a given visit and transforms the data into
 * the JSON shape expected by window.renderPartogram() in the offline epartogram HTML.
 *
 * Entry point: [transform] — a suspend function, safe to call from a coroutine.
 * All DB work runs on [Dispatchers.IO] internally; no threading setup needed by the caller.
 */
object EpartogramDataTransformer {

    private const val TAG = "EpartogramTransformer"

    // ── Concept UUID → parameter index (0-based, 29 total) ─────────────────
    // Indices must stay in sync with the parameters[] array order in the HTML.
    private val UUID_TO_PARAM_IDX = mapOf(
        // 0–3  Supportive Care
        UuidDictionary.COMPANION               to 0,
        UuidDictionary.PAIN_RELIEF             to 1,
        UuidDictionary.ORAL_FLUID              to 2,
        UuidDictionary.POSTURE                 to 3,
        // 4–9  Baby
        UuidDictionary.BASELINE_FHR            to 4,
        UuidDictionary.FHR_DECELERATION        to 5,
        UuidDictionary.AMNIOTIC_FLUID          to 6,
        UuidDictionary.FETAL_POSITION          to 7,
        UuidDictionary.CAPUT                   to 8,
        UuidDictionary.MOULDING                to 9,
        // 10–15 Women
        UuidDictionary.PULSE                   to 10,
        UuidDictionary.SYSTOLIC_BP             to 11,
        UuidDictionary.DIASTOLIC_BP            to 12,
        UuidDictionary.TEMPERATURE             to 13,
        UuidDictionary.URINE_PROTEIN           to 14,
        UuidDictionary.CONTRACTIONS_PER_10MIN  to 15,
        // 16–18 Labour Progress
        UuidDictionary.DURATION_OF_CONTRACTION to 16,
        UuidDictionary.CERVIX_PLOT_X           to 17,
        UuidDictionary.DESCENT_PLOT_0          to 18,
        // 19–21 Medication  (19 & 21 are JSON_VALUE_PARAMS; 20 is ARRAY_PARAM)
        UuidDictionary.OXYTOCIN_UL_DROPS_MIN   to 19,
        UuidDictionary.MEDICINE                to 20,
        UuidDictionary.IV_FLUIDS               to 21,
        // 22–24 Shared Decision Making  (22 & 23 are ARRAY_PARAMS)
        UuidDictionary.ASSESSMENT              to 22,
        UuidDictionary.PLAN                    to 23,
        UuidDictionary.ENCOUNTER_TYPE          to 24,
        // 25 Urine Acetone
        "968f9bc2-b33d-4daf-b59f-79d9a899e018" to 25,
        // 26–28 Prescribed  (26 is ARRAY_PARAM; 27 & 28 are ARRAY_JSON_PARAMS)
        UuidDictionary.MEDICINE_PRESCRIBED     to 26,
        UuidDictionary.OXYTOCIN_PRESCRIBED     to 27,
        UuidDictionary.IV_FLUID_PRESCRIBED     to 28
    )

    // Parameters accumulated into a JSONArray of {value, initial, obsDatetime}
    private val ARRAY_PARAMS = setOf(20, 22, 23, 26)

    // Parameters stored as a single object {value: JSONObject-or-String, initial, obsDatetime}
    private val JSON_VALUE_PARAMS = setOf(19, 21)

    // Parameters accumulated into a JSONArray of {value: JSONObject, initial, obsDatetime}
    private val ARRAY_JSON_PARAMS = setOf(27, 28)

    // pInfo keys whose raw DB values must be converted from "dd/MM/yyyy hh:mm a" to ISO 8601
    private val DATE_PINFO_KEYS = setOf("ActiveLaborDiagnosed", "MembraneRupturedTimestamp")

    // Mapping from tbl_patient_attribute_master.name → JSON key in pInfo
    private val ATTR_TO_PINFO_KEY = mapOf(
        "Parity"                            to "Parity",
        "Labor Onset"                       to "LaborOnset",
        "Active Labor Diagnosed"            to "ActiveLaborDiagnosed",
        "Membrane Ruptured Timestamp"       to "MembraneRupturedTimestamp",
        "Risk factors"                      to "Riskfactors",
        "Gravida"                           to "Gravida",
        "Last Menstrual Period (LMP)"       to "LMP",
        "Estimated Date of Delivery (EDD)"  to "EDD"
    )

    private val STAGE_HOUR_PATTERN     = Pattern.compile("Stage(\\d+)_Hour(\\d+)(?:_(\\d+))?")
    private val SOS_STAGE_HOUR_PATTERN = Pattern.compile("Stage(\\d+)_Hour(\\d+)_SOS(\\d+)")

    // Minimum sub-columns per hour guaranteed regardless of encounter count.
    // Matches the Angular component's maxSubColPerHour initialisation.
    private const val MIN_SUBCOLS_STAGE1 = 2
    private const val MIN_SUBCOLS_STAGE2 = 4

    // ── Internal data class ─────────────────────────────────────────────────

    private data class EncounterRecord(
        val uuid: String,
        val typeUuid: String,
        val typeName: String,
        val encounterTime: String,
        val providerUuid: String,
        var stage: Int  = 0,   // 1 or 2 (0 = unresolved)
        var hour: Int   = 0,   // 1-based
        var subCol: Int = 1    // 1-based sub-column within this hour
    ) {
        fun isSos() = typeUuid == UuidDictionary.LCG_SOS
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Transforms all DB data for the given visit UUID into the JSON string
     * expected by window.renderPartogram() in the standalone epartogram HTML.
     *
     * @param visitUuid the visit UUID whose data should be rendered
     * @return JSON string, or null on fatal error
     */
    suspend fun transform(visitUuid: String): String? = withContext(Dispatchers.IO) {
        try {
            val db   = AppConstants.inteleHealthDatabaseHelper.writeDb
            val root = JSONObject()

            val patientUuid = queryPatientUuid(db, visitUuid) ?: ""
            root.put("pInfo", buildPinfo(db, patientUuid))
            buildStageData(db, visitUuid, root)
            buildVisitCompleteData(db, visitUuid, root)

            root.toString()
        } catch (e: Exception) {
            Log.e(TAG, "transform failed", e)
            null
        }
    }

    // ── Patient info ────────────────────────────────────────────────────────

    @SuppressLint("Range")
    private fun queryPatientUuid(db: SQLiteDatabase, visitUuid: String): String? {
        return db.rawQuery("SELECT patientuuid FROM tbl_visit WHERE uuid = ?",
            arrayOf(visitUuid)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    @SuppressLint("Range")
    private fun buildPinfo(db: SQLiteDatabase, patientUuid: String): JSONObject {
        val pInfo = JSONObject()

        db.rawQuery(
            "SELECT first_name, middle_name, last_name, date_of_birth, openmrs_id " +
            "FROM tbl_patient WHERE uuid = ?",
            arrayOf(patientUuid)
        ).use { c ->
            if (c.moveToFirst()) {
                val fn   = c.getString(c.getColumnIndex("first_name")).orEmpty()
                val mn   = c.getString(c.getColumnIndex("middle_name")).orEmpty()
                val ln   = c.getString(c.getColumnIndex("last_name")).orEmpty()
                val name = "$fn $mn $ln".trim().replace(Regex("\\s+"), " ")
                pInfo.put("patientName", name)
                pInfo.put("dob",       c.getString(c.getColumnIndex("date_of_birth")).orEmpty())
                pInfo.put("openmrsId", c.getString(c.getColumnIndex("openmrs_id")).orEmpty())
            }
        }

        db.rawQuery(
            "SELECT pa.value, COALESCE(m.name, '') AS attr_name " +
            "FROM tbl_patient_attribute pa " +
            "LEFT JOIN tbl_patient_attribute_master m ON m.uuid = pa.person_attribute_type_uuid " +
            "WHERE pa.patientuuid = ?",
            arrayOf(patientUuid)
        ).use { c ->
            while (c.moveToNext()) {
                val attrName = c.getString(c.getColumnIndex("attr_name")).orEmpty()
                val attrVal  = c.getString(c.getColumnIndex("value")).orEmpty()
                ATTR_TO_PINFO_KEY[attrName]?.let { key ->
                    // Date fields stored as "dd/MM/yyyy hh:mm a" → convert to ISO 8601
                    // so the HTML fmtDate() can parse them correctly via new Date().
                    val finalVal = if (key in DATE_PINFO_KEYS) convertPatientDate(attrVal) else attrVal
                    pInfo.put(key, finalVal)
                }
            }
        }

        return pInfo
    }

    /**
     * Converts encounter_time stored in the DB as "yyyy-MM-dd HH:mm:ss" (UTC) to ISO 8601 UTC.
     * JS new Date() treats a space-separated string as local time, causing a 5.5 h IST shift;
     * appending T…Z forces unambiguous UTC parsing.
     */
    private fun convertEncounterTime(raw: String): String {
        if (raw.isEmpty()) return raw
        if (raw.contains('T') || raw.endsWith('Z')) return raw  // already ISO
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            inFmt.timeZone = TimeZone.getTimeZone("UTC")
            val date = inFmt.parse(raw) ?: return raw
            val outFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
            outFmt.timeZone = TimeZone.getTimeZone("UTC")
            outFmt.format(date)
        } catch (_: Exception) { raw }
    }

    /**
     * Converts a date string stored in the DB as "dd/MM/yyyy hh:mm a" to ISO 8601.
     * Returns the raw value unchanged if it is "U" (unknown) or cannot be parsed.
     */
    private fun convertPatientDate(raw: String): String {
        if (raw.equals("U", ignoreCase = true)) return raw
        return try {
            val inFmt  = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
            inFmt.isLenient = false
            val date   = inFmt.parse(raw) ?: return raw
            val outFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
            outFmt.timeZone = TimeZone.getTimeZone("UTC")
            outFmt.format(date)
        } catch (_: Exception) { raw }
    }

    // ── Stage data (main partogram grid) ────────────────────────────────────

    @SuppressLint("Range")
    private fun buildStageData(db: SQLiteDatabase, visitUuid: String, root: JSONObject) {
        val encounters    = fetchEncounters(db, visitUuid)
        val initialsCache = mutableMapOf<String, String>()

        encounters.forEach { resolveStageHour(it, db) }

        // ── First pass: compute effective sub-column count per (stage, hour) ─
        // maxSubCol tracks the highest subCol seen for each hour.
        // The effective count is max(maxSubCol, MIN_SUBCOLS) to match the Angular
        // component's behaviour of initialising each hour with a minimum width.
        val maxSubCol = mutableMapOf<String, Int>()
        val sosUuidSet = mutableSetOf<String>()

        for (enc in encounters) {
            if (enc.stage == 0) continue
            val key = "s${enc.stage}_h${enc.hour}"
            if (enc.subCol > (maxSubCol[key] ?: 0)) maxSubCol[key] = enc.subCol
            if (enc.isSos()) sosUuidSet.add(enc.uuid)
        }

        val s1ColTotal = countCols(1, 15, maxSubCol)
        val s2ColTotal = countCols(2, 5,  maxSubCol)

        // Flat int arrays — what the HTML renderer reads directly
        root.put("subColsPerHourStage1", buildFlatColArray(1, 15, maxSubCol))
        root.put("subColsPerHourStage2", buildFlatColArray(2, 5,  maxSubCol))
        root.put("totalStage1Cols", s1ColTotal)
        root.put("totalStage2Cols", s2ColTotal)

        // SOS encounter UUIDs (used by the HTML to apply SOS styling)
        val sosJsonArray = JSONArray()
        sosUuidSet.forEach { sosJsonArray.put(it) }
        root.put("sosEncounterUUIDs", sosJsonArray)

        // Pre-compute hour start indices for O(1) column lookup
        val s1HourStarts = hourStartCols(1, 15, maxSubCol)
        val s2HourStarts = hourStartCols(2, 5,  maxSubCol)

        // Observation value grids (29 params × total columns)
        val s1Values = Array(29) { arrayOfNulls<JSONObject>(s1ColTotal) }
        val s2Values = Array(29) { arrayOfNulls<JSONObject>(s2ColTotal) }

        // Time & encounter info per sub-column (drives the Time header row in the HTML)
        val timeFullS1   = arrayOfNulls<String>(s1ColTotal)
        val timeFullS2   = arrayOfNulls<String>(s2ColTotal)
        val encuuid1Full = arrayOfNulls<JSONObject>(s1ColTotal)
        val encuuid2Full = arrayOfNulls<JSONObject>(s2ColTotal)

        // First encounter UUID + provider initials per hour (drives the Initials row)
        val encuuid1    = arrayOfNulls<String>(15)
        val encuuid2    = arrayOfNulls<String>(5)
        val initialsS1  = arrayOfNulls<String>(15)
        val initialsS2  = arrayOfNulls<String>(5)

        // ── Second pass: fill observations + time/encounter tracking ─────────
        for (enc in encounters) {
            if (enc.stage == 0) continue
            val hourIdx    = enc.hour - 1
            val subIdx     = enc.subCol - 1
            val hourStarts = if (enc.stage == 1) s1HourStarts else s2HourStarts
            val maxHours   = if (enc.stage == 1) 15 else 5
            val values     = if (enc.stage == 1) s1Values else s2Values

            if (hourIdx < 0 || hourIdx >= maxHours) continue
            val colIdx = hourStarts[hourIdx] + subIdx
            if (colIdx >= values[0].size) continue

            val initials = getProviderInitials(db, enc.providerUuid, initialsCache)
            fillObservations(db, enc.uuid, enc.encounterTime, initials, colIdx, values)

            // Track time and encounter metadata for each occupied sub-column slot
            val encInfo = JSONObject().apply {
                put("enc_time",       enc.encounterTime)
                put("enc_uuid",       enc.uuid)
                put("stageNo",        enc.stage)
                put("stageHourNo",    enc.hour)
                put("stageHourSecNo", enc.subCol)
            }
            if (enc.stage == 1) {
                timeFullS1[colIdx]   = enc.encounterTime
                encuuid1Full[colIdx] = encInfo
                // First non-SOS encounter per hour drives the initials
                if (!enc.isSos() && encuuid1[hourIdx] == null) {
                    encuuid1[hourIdx]  = enc.uuid
                    initialsS1[hourIdx] = initials
                }
            } else {
                timeFullS2[colIdx]   = enc.encounterTime
                encuuid2Full[colIdx] = encInfo
                if (!enc.isSos() && encuuid2[hourIdx] == null) {
                    encuuid2[hourIdx]  = enc.uuid
                    initialsS2[hourIdx] = initials
                }
            }
        }

        // Emit time / encounter arrays
        root.put("timeFullStage1",  nullableStringArrayToJson(timeFullS1))
        root.put("timeFullStage2",  nullableStringArrayToJson(timeFullS2))
        root.put("encuuid1Full",    encInfoArrayToJson(encuuid1Full))
        root.put("encuuid2Full",    encInfoArrayToJson(encuuid2Full))
        root.put("encuuid1",        nullableStringArrayToJson(encuuid1))
        root.put("encuuid2",        nullableStringArrayToJson(encuuid2))
        root.put("initialsStage1",  nullableStringArrayToJson(initialsS1))
        root.put("initialsStage2",  nullableStringArrayToJson(initialsS2))

        root.put("parameters", buildParametersJson(s1Values, s2Values, s1ColTotal, s2ColTotal))
        buildHistoryLists(root, s1Values, s2Values)
    }

    /**
     * Effective sub-column count for a given (stage, hour), matching the Angular
     * component's behaviour: at least MIN_SUBCOLS, growing if SOS encounters exist.
     */
    private fun effectiveSubs(stage: Int, hour: Int, maxSubCol: Map<String, Int>): Int {
        val min = if (stage == 1) MIN_SUBCOLS_STAGE1 else MIN_SUBCOLS_STAGE2
        return maxOf(maxSubCol["s${stage}_h${hour}"] ?: 0, min)
    }

    private fun countCols(stage: Int, maxHour: Int, maxSubCol: Map<String, Int>): Int =
        (1..maxHour).sumOf { h -> effectiveSubs(stage, h, maxSubCol) }

    private fun hourStartCols(stage: Int, maxHour: Int, maxSubCol: Map<String, Int>): IntArray {
        val starts  = IntArray(maxHour)
        var running = 0
        for (h in 1..maxHour) {
            starts[h - 1] = running
            running += effectiveSubs(stage, h, maxSubCol)
        }
        return starts
    }

    /** Flat JSONArray of ints — one per hour — that the HTML renderer reads directly. */
    private fun buildFlatColArray(stage: Int, maxHour: Int, maxSubCol: Map<String, Int>): JSONArray {
        val out = JSONArray()
        for (h in 1..maxHour) out.put(effectiveSubs(stage, h, maxSubCol))
        return out
    }

    // ── Array helpers ───────────────────────────────────────────────────────

    private fun nullableStringArrayToJson(arr: Array<String?>): JSONArray {
        val out = JSONArray()
        for (v in arr) if (v != null) out.put(v) else out.put(JSONObject.NULL)
        return out
    }

    private fun encInfoArrayToJson(arr: Array<JSONObject?>): JSONArray {
        val out = JSONArray()
        for (v in arr) if (v != null) out.put(v) else out.put(JSONObject.NULL)
        return out
    }

    // ── Encounter fetch ──────────────────────────────────────────────────────

    @SuppressLint("Range")
    private fun fetchEncounters(db: SQLiteDatabase, visitUuid: String): List<EncounterRecord> {
        val list = mutableListOf<EncounterRecord>()
        db.rawQuery(
            "SELECT e.uuid, e.encounter_type_uuid, e.encounter_time, e.provider_uuid, " +
            "       COALESCE(u.name, '') AS enc_type_name " +
            "FROM tbl_encounter e " +
            "LEFT JOIN tbl_uuid_dictionary u ON u.uuid = e.encounter_type_uuid " +
            "WHERE e.visituuid = ? " +
            "  AND (e.voided = '0' OR e.voided = 'false' OR e.voided = 'FALSE') " +
            "  AND e.encounter_type_uuid != '${UuidDictionary.ENCOUNTER_VISIT_COMPLETE}' " +
            "ORDER BY e.encounter_time ASC",
            arrayOf(visitUuid)
        ).use { c ->
            while (c.moveToNext()) {
                list += EncounterRecord(
                    uuid          = c.getString(c.getColumnIndex("uuid")).orEmpty(),
                    typeUuid      = c.getString(c.getColumnIndex("encounter_type_uuid")).orEmpty(),
                    typeName      = c.getString(c.getColumnIndex("enc_type_name")).orEmpty(),
                    encounterTime = convertEncounterTime(c.getString(c.getColumnIndex("encounter_time")).orEmpty()),
                    providerUuid  = c.getString(c.getColumnIndex("provider_uuid")).orEmpty()
                )
            }
        }
        return list
    }

    // ── Stage/hour resolution ────────────────────────────────────────────────

    @SuppressLint("Range")
    private fun resolveStageHour(enc: EncounterRecord, db: SQLiteDatabase) {
        if (enc.isSos()) {
            db.rawQuery(
                "SELECT value FROM tbl_obs " +
                "WHERE encounteruuid = ? AND conceptuuid = ? " +
                "  AND (voided = '0' OR voided = 'false') LIMIT 1",
                arrayOf(enc.uuid, UuidDictionary.SOS_STAGE_HOUR)
            ).use { c ->
                if (c.moveToFirst()) {
                    val m = SOS_STAGE_HOUR_PATTERN.matcher(c.getString(0).orEmpty())
                    if (m.find()) {
                        enc.stage  = m.group(1)!!.toInt()
                        enc.hour   = m.group(2)!!.toInt()
                        enc.subCol = m.group(3)!!.toInt()
                        return
                    }
                }
            }
        }

        val m = STAGE_HOUR_PATTERN.matcher(enc.typeName)
        if (m.find()) {
            enc.stage  = m.group(1)!!.toInt()
            enc.hour   = m.group(2)!!.toInt()
            enc.subCol = m.group(3)?.takeIf { it.isNotEmpty() }?.toInt() ?: 1
        }
    }

    // ── Observation filling ─────────────────────────────────────────────────

    @SuppressLint("Range")
    private fun fillObservations(db: SQLiteDatabase, encounterUuid: String,
                                  encounterTime: String, initials: String,
                                  colIdx: Int, values: Array<Array<JSONObject?>>) {
        db.rawQuery(
            "SELECT conceptuuid, value, comment FROM tbl_obs " +
            "WHERE encounteruuid = ? " +
            "  AND (voided = '0' OR voided = 'false' OR voided = 'FALSE')",
            arrayOf(encounterUuid)
        ).use { c ->
            while (c.moveToNext()) {
                val conceptUuid = c.getString(c.getColumnIndex("conceptuuid")).orEmpty()
                val value       = c.getString(c.getColumnIndex("value")).orEmpty()
                val comment     = c.getString(c.getColumnIndex("comment")).orEmpty()

                val paramIdx = UUID_TO_PARAM_IDX[conceptUuid] ?: continue
                if (paramIdx >= values.size || colIdx >= values[paramIdx].size) continue

                try {
                    values[paramIdx][colIdx] = buildObsValue(
                        paramIdx, value, comment, encounterTime, initials,
                        values[paramIdx][colIdx]
                    )
                } catch (e: JSONException) {
                    Log.w(TAG, "buildObsValue failed for param $paramIdx", e)
                }
            }
        }
    }

    private fun buildObsValue(paramIdx: Int, value: String, comment: String,
                               obsDatetime: String, initials: String,
                               existing: JSONObject?): JSONObject {
        return when {
            paramIdx in ARRAY_PARAMS -> {
                val arr    = existing?.optJSONArray("arr") ?: JSONArray()
                val holder = existing ?: JSONObject().also { it.put("arr", arr) }
                arr.put(JSONObject().apply {
                    put("value", value)
                    put("initial", initials)
                    put("obsDatetime", obsDatetime)
                    put("comment", comment)
                })
                holder
            }
            paramIdx in ARRAY_JSON_PARAMS -> {
                val arr    = existing?.optJSONArray("arr") ?: JSONArray()
                val holder = existing ?: JSONObject().also { it.put("arr", arr) }
                arr.put(JSONObject().apply {
                    put("value", parseJsonOrString(value))
                    put("initial", initials)
                    put("obsDatetime", obsDatetime)
                })
                holder
            }
            paramIdx in JSON_VALUE_PARAMS -> JSONObject().apply {
                put("value", parseJsonOrString(value))
                put("initial", initials)
                put("obsDatetime", obsDatetime)
            }
            else -> JSONObject().apply {
                put("value", value)
                put("comment", comment)
            }
        }
    }

    // ── Parameters JSON array (29 entries) ─────────────────────────────────

    private fun buildParametersJson(s1Values: Array<Array<JSONObject?>>,
                                     s2Values: Array<Array<JSONObject?>>,
                                     s1Cols: Int, s2Cols: Int): JSONArray {
        val params = JSONArray()
        for (i in 0 until 29) {
            params.put(JSONObject().apply {
                put("stage1", colArrayToJson(s1Values[i], i, s1Cols))
                put("stage2", colArrayToJson(s2Values[i], i, s2Cols))
            })
        }
        return params
    }

    private fun colArrayToJson(cols: Array<JSONObject?>, paramIdx: Int,
                                totalCols: Int): JSONArray {
        val out         = JSONArray()
        val isArrayType = paramIdx in ARRAY_PARAMS || paramIdx in ARRAY_JSON_PARAMS
        for (c in 0 until totalCols) {
            val cell = cols.getOrNull(c)
            when {
                cell == null -> out.put(JSONObject.NULL)
                isArrayType  -> out.put(cell.optJSONArray("arr") ?: JSONObject.NULL)
                else         -> out.put(cell)
            }
        }
        return out
    }

    // ── History lists ────────────────────────────────────────────────────────
    // Keys match what the HTML's normalizeData() reads directly.

    private fun buildHistoryLists(root: JSONObject,
                                   s1Values: Array<Array<JSONObject?>>,
                                   s2Values: Array<Array<JSONObject?>>) {
        root.put("assessmentHistory",          flattenArrayParam(22, s1Values, s2Values))
        root.put("planHistory",                flattenArrayParam(23, s1Values, s2Values))
        root.put("medicationPrescribedHistory",mergeArrayParams(intArrayOf(20, 26), s1Values, s2Values))
        root.put("oxytocinPrescribedHistory",  mergeArrayParams(intArrayOf(19, 27), s1Values, s2Values))
        root.put("ivPrescribedHistory",        mergeArrayParams(intArrayOf(21, 28), s1Values, s2Values))
    }

    private fun flattenArrayParam(paramIdx: Int,
                                   s1Values: Array<Array<JSONObject?>>,
                                   s2Values: Array<Array<JSONObject?>>): JSONArray {
        val result = JSONArray()
        appendFrom(result, s1Values[paramIdx])
        appendFrom(result, s2Values[paramIdx])
        return result
    }

    private fun mergeArrayParams(paramIdxs: IntArray,
                                  s1Values: Array<Array<JSONObject?>>,
                                  s2Values: Array<Array<JSONObject?>>): JSONArray {
        val result = JSONArray()
        for (idx in paramIdxs) {
            appendFrom(result, s1Values[idx])
            appendFrom(result, s2Values[idx])
        }
        return result
    }

    private fun appendFrom(target: JSONArray, cols: Array<JSONObject?>) {
        for (cell in cols) {
            if (cell == null) continue
            val arr = cell.optJSONArray("arr")
            if (arr != null) {
                for (i in 0 until arr.length()) target.put(arr.get(i))
            } else if (cell.has("value")) {
                target.put(cell)
            }
        }
    }

    // ── Visit complete card ──────────────────────────────────────────────────
    // Emits flat fields at the root level so the HTML normalizeData() can read
    // them directly (visitCompleted, visitCompleteReason, birthOutcome, etc.).

    @SuppressLint("Range")
    private fun buildVisitCompleteData(db: SQLiteDatabase, visitUuid: String,
                                        root: JSONObject) {
        var vcEncounterUuid: String? = null

        db.rawQuery(
            "SELECT uuid FROM tbl_encounter " +
            "WHERE visituuid = ? AND encounter_type_uuid = ? " +
            "  AND (voided = '0' OR voided = 'false') LIMIT 1",
            arrayOf(visitUuid, UuidDictionary.ENCOUNTER_VISIT_COMPLETE)
        ).use { c ->
            if (c.moveToFirst()) vcEncounterUuid = c.getString(0)
        }

        if (vcEncounterUuid == null) {
            root.put("visitCompleted", false)
            return
        }

        root.put("visitCompleted", true)

        // Collect all obs for the visit-complete encounter
        val obs = mutableMapOf<String, String>()
        db.rawQuery(
            "SELECT conceptuuid, value FROM tbl_obs " +
            "WHERE encounteruuid = ? AND (voided = '0' OR voided = 'false')",
            arrayOf(vcEncounterUuid)
        ).use { c ->
            while (c.moveToNext()) {
                val uuid  = c.getString(c.getColumnIndex("conceptuuid")).orEmpty()
                val value = c.getString(c.getColumnIndex("value")).orEmpty()
                obs[uuid] = value
            }
        }

        // ── visitCompleteReason ─────────────────────────────────────────────
        // Priority: OUT_OF_TIME > REFER_TYPE > "Newborn" (delivery completed)
        val outOfTimeVal = obs[UuidDictionary.OUT_OF_TIME]
        val referTypeVal = obs[UuidDictionary.REFER_TYPE]
        when {
            !outOfTimeVal.isNullOrEmpty() -> {
                root.put("visitCompleteReason", "Out Of Time")
                root.put("outOfTimeReason", outOfTimeVal)
            }
            !referTypeVal.isNullOrEmpty() -> {
                root.put("visitCompleteReason", referTypeVal)
                // LABOUR_OTHER carries the free-text reason when referType == "Other"
                val otherReason = obs[UuidDictionary.LABOUR_OTHER].orEmpty()
                if (otherReason.isNotEmpty()) root.put("referTypeOtherReason", otherReason)
            }
            else -> root.put("visitCompleteReason", "Newborn")
        }

        // ── Birth / baby fields ─────────────────────────────────────────────
        root.put("birthOutcome",      obs[UuidDictionary.BIRTH_OUTCOME]        ?: "")
        root.put("birthOutcomeOther", obs[UuidDictionary.END_2ND_STAGE_OTHER]  ?: "")
        root.put("birthWeight",       obs[UuidDictionary.BIRTH_WEIGHT]         ?: "")
        root.put("apgar1",            obs[UuidDictionary.APGAR_1_MIN]          ?: "")
        root.put("apgar5",            obs[UuidDictionary.APGAR_5_MIN]          ?: "")
        root.put("babyGender",        obs[UuidDictionary.SEX]                  ?: "")
        root.put("babyStatus",        obs[UuidDictionary.BABY_STATUS]          ?: "")

        // ── Mother fields ───────────────────────────────────────────────────
        val motherDecVal = obs[UuidDictionary.MOTHER_DECEASED].orEmpty()
        root.put("motherDeceased",       if (motherDecVal.equals("YES", ignoreCase = true)) "YES" else "")
        root.put("motherDeceasedReason", obs[UuidDictionary.MOTHER_DECEASED_FLAG] ?: "")
    }

    // ── Provider initials ────────────────────────────────────────────────────

    @SuppressLint("Range")
    /**
     * tbl_encounter.provider_uuid is a provider uuid, so it matches
     * tbl_provider.uuid — not tbl_provider.useruuid, which is the linked user
     * and matches tbl_obs.creatoruuid instead. Joining on useruuid here
     * returned nothing for every encounter, which left the INITIALS row of the
     * Labour Care Guide blank in both the printed sheet and the offline view.
     */
    private fun getProviderInitials(db: SQLiteDatabase, providerUuid: String,
                                     cache: MutableMap<String, String>): String {
        if (providerUuid.isEmpty()) return ""
        cache[providerUuid]?.let { return it }

        var initials = ""
        db.rawQuery(
            "SELECT given_name, family_name FROM tbl_provider WHERE uuid = ? LIMIT 1",
            arrayOf(providerUuid)
        ).use { c ->
            if (c.moveToFirst()) {
                val given  = c.getString(c.getColumnIndex("given_name")).orEmpty()
                val family = c.getString(c.getColumnIndex("family_name")).orEmpty()
                if (given.isNotEmpty())  initials += given[0]
                if (family.isNotEmpty()) initials += family[0]
                initials = initials.uppercase()
            }
        }

        cache[providerUuid] = initials
        return initials
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private fun parseJsonOrString(value: String): Any {
        if (value.startsWith("{")) try { return JSONObject(value) } catch (_: JSONException) {}
        if (value.startsWith("[")) try { return JSONArray(value)  } catch (_: JSONException) {}
        return value
    }
}
