package org.intelehealth.ezazi.activities.epartogramActivity;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.utilities.UuidDictionary;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Queries the local SQLite DB for a given visit and transforms the data into
 * the JSON shape expected by window.renderPartogram() in the offline epartogram HTML.
 *
 * Entry point: {@link #transform(String)}
 */
public class EpartogramDataTransformer {

    private static final String TAG = "EpartogramTransformer";

    // ── Concept UUID → parameter index (0-based, 29 total) ─────────────────
    // Indices must stay in sync with the parameters[] array order in the HTML.
    private static final Map<String, Integer> UUID_TO_PARAM_IDX = new HashMap<>();

    // Parameters accumulated into a JSONArray of {value, initial, obsDatetime}
    // Indices: Medicine(20), Assessment(22), Plan(23), Medicine Prescribed(26)
    private static final Set<Integer> ARRAY_PARAMS = new HashSet<>(Arrays.asList(20, 22, 23, 26));

    // Parameters stored as a single object {value: (JSONObject if JSON else String), initial, obsDatetime}
    // Indices: Oxytocin(19), IV Fluids(21)
    private static final Set<Integer> JSON_VALUE_PARAMS = new HashSet<>(Arrays.asList(19, 21));

    // Parameters accumulated into a JSONArray of {value: JSONObject, initial, obsDatetime}
    // Indices: Oxytocin Prescribed(27), IV Fluids Prescribed(28)
    private static final Set<Integer> ARRAY_JSON_PARAMS = new HashSet<>(Arrays.asList(27, 28));

    static {
        // 0–3  Supportive Care
        UUID_TO_PARAM_IDX.put(UuidDictionary.COMPANION,              0);
        UUID_TO_PARAM_IDX.put(UuidDictionary.PAIN_RELIEF,            1);
        UUID_TO_PARAM_IDX.put(UuidDictionary.ORAL_FLUID,             2);
        UUID_TO_PARAM_IDX.put(UuidDictionary.POSTURE,                3);
        // 4–9  Baby
        UUID_TO_PARAM_IDX.put(UuidDictionary.BASELINE_FHR,           4);
        UUID_TO_PARAM_IDX.put(UuidDictionary.FHR_DECELERATION,       5);
        UUID_TO_PARAM_IDX.put(UuidDictionary.AMNIOTIC_FLUID,         6);
        UUID_TO_PARAM_IDX.put(UuidDictionary.FETAL_POSITION,         7);
        UUID_TO_PARAM_IDX.put(UuidDictionary.CAPUT,                  8);
        UUID_TO_PARAM_IDX.put(UuidDictionary.MOULDING,               9);
        // 10–15 Women
        UUID_TO_PARAM_IDX.put(UuidDictionary.PULSE,                  10);
        UUID_TO_PARAM_IDX.put(UuidDictionary.SYSTOLIC_BP,            11);
        UUID_TO_PARAM_IDX.put(UuidDictionary.DIASTOLIC_BP,           12);
        UUID_TO_PARAM_IDX.put(UuidDictionary.TEMPERATURE,            13);
        UUID_TO_PARAM_IDX.put(UuidDictionary.URINE_PROTEIN,          14);
        UUID_TO_PARAM_IDX.put(UuidDictionary.CONTRACTIONS_PER_10MIN, 15);
        // 16–18 Labour Progress
        UUID_TO_PARAM_IDX.put(UuidDictionary.DURATION_OF_CONTRACTION, 16);
        UUID_TO_PARAM_IDX.put(UuidDictionary.CERVIX_PLOT_X,          17);
        UUID_TO_PARAM_IDX.put(UuidDictionary.DESCENT_PLOT_0,         18);
        // 19–21 Medication  (19 & 21 are JSON_VALUE_PARAMS; 20 is ARRAY_PARAM)
        UUID_TO_PARAM_IDX.put(UuidDictionary.OXYTOCIN_UL_DROPS_MIN,  19);
        UUID_TO_PARAM_IDX.put(UuidDictionary.MEDICINE,               20);
        UUID_TO_PARAM_IDX.put(UuidDictionary.IV_FLUIDS,              21);
        // 22–24 Shared Decision Making  (22 & 23 are ARRAY_PARAMS)
        UUID_TO_PARAM_IDX.put(UuidDictionary.ASSESSMENT,             22);
        UUID_TO_PARAM_IDX.put(UuidDictionary.PLAN,                   23);
        UUID_TO_PARAM_IDX.put(UuidDictionary.ENCOUNTER_TYPE,         24);
        // 25 Urine Acetone
        UUID_TO_PARAM_IDX.put("968f9bc2-b33d-4daf-b59f-79d9a899e018", 25);
        // 26–28 Prescribed  (26 is ARRAY_PARAM; 27 & 28 are ARRAY_JSON_PARAMS)
        UUID_TO_PARAM_IDX.put(UuidDictionary.MEDICINE_PRESCRIBED,    26);
        UUID_TO_PARAM_IDX.put(UuidDictionary.OXYTOCIN_PRESCRIBED,    27);
        UUID_TO_PARAM_IDX.put(UuidDictionary.IV_FLUID_PRESCRIBED,    28);
    }

    // Mapping from tbl_patient_attribute_master.name → JSON key in pInfo
    private static final Map<String, String> ATTR_TO_PINFO_KEY = new HashMap<>();
    static {
        ATTR_TO_PINFO_KEY.put("Parity",                            "Parity");
        ATTR_TO_PINFO_KEY.put("Labor Onset",                       "LaborOnset");
        ATTR_TO_PINFO_KEY.put("Active Labor Diagnosed",            "ActiveLaborDiagnosed");
        ATTR_TO_PINFO_KEY.put("Membrane Ruptured Timestamp",       "MembraneRupturedTimestamp");
        ATTR_TO_PINFO_KEY.put("Risk factors",                      "Riskfactors");
        ATTR_TO_PINFO_KEY.put("Gravida",                           "Gravida");
        ATTR_TO_PINFO_KEY.put("Last Menstrual Period (LMP)",       "LMP");
        ATTR_TO_PINFO_KEY.put("Estimated Date of Delivery (EDD)", "EDD");
    }

    private static final Pattern STAGE_HOUR_PATTERN =
            Pattern.compile("Stage(\\d+)_Hour(\\d+)(?:_(\\d+))?");
    private static final Pattern SOS_STAGE_HOUR_PATTERN =
            Pattern.compile("Stage(\\d+)_Hour(\\d+)_SOS(\\d+)");

    // ── Internal record types ───────────────────────────────────────────────

    private static class EncounterRecord {
        final String uuid;
        final String typeUuid;
        final String typeName;
        final String encounterTime;
        final String providerUuid;
        int stage = 0;   // 1 or 2 (0 = unresolved)
        int hour  = 0;   // 1-based
        int subCol = 1;  // 1-based sub-column within this hour

        EncounterRecord(String uuid, String typeUuid, String typeName,
                        String encounterTime, String providerUuid) {
            this.uuid = uuid;
            this.typeUuid = typeUuid;
            this.typeName = typeName;
            this.encounterTime = encounterTime;
            this.providerUuid = providerUuid;
        }

        boolean isSos() {
            return UuidDictionary.LCG_SOS.equals(typeUuid);
        }
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Transforms all DB data for the given visit UUID into the JSON string
     * expected by window.renderPartogram() in the standalone epartogram HTML.
     *
     * @param visitUuid the visit UUID whose data should be rendered
     * @return JSON string, or null on fatal error
     */
    @SuppressLint("Range")
    public static String transform(String visitUuid) {
        try {
            SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
            JSONObject root = new JSONObject();

            String patientUuid = queryPatientUuid(db, visitUuid);
            if (patientUuid == null) patientUuid = "";

            root.put("pInfo", buildPinfo(db, patientUuid, visitUuid));
            buildStageData(db, visitUuid, root);
            buildVisitCompleteData(db, visitUuid, root);

            return root.toString();
        } catch (Exception e) {
            Log.e(TAG, "transform failed", e);
            return null;
        }
    }

    // ── Patient info ────────────────────────────────────────────────────────

    @SuppressLint("Range")
    private static String queryPatientUuid(SQLiteDatabase db, String visitUuid) {
        Cursor c = db.rawQuery(
                "SELECT patientuuid FROM tbl_visit WHERE uuid = ?",
                new String[]{visitUuid});
        try {
            if (c.moveToFirst()) return c.getString(0);
        } finally { c.close(); }
        return null;
    }

    @SuppressLint("Range")
    private static JSONObject buildPinfo(SQLiteDatabase db, String patientUuid,
                                         String visitUuid) throws JSONException {
        JSONObject pInfo = new JSONObject();

        // Patient name & demographics from tbl_patient
        Cursor pc = db.rawQuery(
                "SELECT first_name, middle_name, last_name, date_of_birth, openmrs_id " +
                        "FROM tbl_patient WHERE uuid = ?",
                new String[]{patientUuid});
        try {
            if (pc.moveToFirst()) {
                String fn = nvl(pc.getString(pc.getColumnIndex("first_name")));
                String mn = nvl(pc.getString(pc.getColumnIndex("middle_name")));
                String ln = nvl(pc.getString(pc.getColumnIndex("last_name")));
                String name = (fn + " " + mn + " " + ln).trim().replaceAll("\\s+", " ");
                pInfo.put("patientName", name);
                pInfo.put("dob", nvl(pc.getString(pc.getColumnIndex("date_of_birth"))));
                pInfo.put("openmrsId", nvl(pc.getString(pc.getColumnIndex("openmrs_id"))));
            }
        } finally { pc.close(); }

        // Patient attributes from tbl_patient_attribute joined on tbl_patient_attribute_master
        Cursor ac = db.rawQuery(
                "SELECT pa.value, COALESCE(m.name, '') AS attr_name " +
                        "FROM tbl_patient_attribute pa " +
                        "LEFT JOIN tbl_patient_attribute_master m " +
                        "       ON m.uuid = pa.person_attribute_type_uuid " +
                        "WHERE pa.patientuuid = ?",
                new String[]{patientUuid});
        try {
            while (ac.moveToNext()) {
                String attrName = nvl(ac.getString(ac.getColumnIndex("attr_name")));
                String attrVal  = nvl(ac.getString(ac.getColumnIndex("value")));
                String jsonKey  = ATTR_TO_PINFO_KEY.get(attrName);
                if (jsonKey != null) {
                    pInfo.put(jsonKey, attrVal);
                }
            }
        } finally { ac.close(); }

        return pInfo;
    }

    // ── Stage data (main partogram grid) ────────────────────────────────────

    @SuppressLint("Range")
    private static void buildStageData(SQLiteDatabase db, String visitUuid,
                                       JSONObject root) throws JSONException {
        List<EncounterRecord> encounters = fetchEncounters(db, visitUuid);
        Map<String, String> initialsCache = new HashMap<>();

        // Resolve stage/hour for each encounter
        for (EncounterRecord enc : encounters) {
            resolveStageHour(enc, db);
        }

        // First pass: compute maxSubColPerHour for each (stage, hour)
        // Key: "s<stage>_h<hour>" → max sub-column count
        Map<String, Integer> subColCount = new HashMap<>();
        for (EncounterRecord enc : encounters) {
            if (enc.stage == 0) continue;
            String key = "s" + enc.stage + "_h" + enc.hour;
            int cur = subColCount.containsKey(key) ? subColCount.get(key) : 0;
            if (enc.subCol > cur) subColCount.put(key, enc.subCol);
        }

        // Build stage1Columns / stage2Columns arrays (column metadata)
        // stage 1: up to 15 hours; stage 2: up to 5 hours
        JSONArray stage1Cols = new JSONArray();
        JSONArray stage2Cols = new JSONArray();
        buildColumnArray(stage1Cols, 1, 15, subColCount);
        buildColumnArray(stage2Cols, 2, 5, subColCount);
        root.put("stage1Columns", stage1Cols);
        root.put("stage2Columns", stage2Cols);

        // Count total columns per stage
        int s1ColTotal = countCols(1, 15, subColCount);
        int s2ColTotal = countCols(2, 5, subColCount);

        // Prepare value matrices: parameters[29][totalCols]
        // We'll use a flat list per parameter where index = column position
        JSONObject[][] s1Values = new JSONObject[29][s1ColTotal];
        JSONObject[][] s2Values = new JSONObject[29][s2ColTotal];

        // Build (stage, hour) → starting column index map
        int[] s1HourStartCol = hourStartCols(1, 15, subColCount);
        int[] s2HourStartCol = hourStartCols(2, 5,  subColCount);

        // Second pass: fill observations into matrices
        for (EncounterRecord enc : encounters) {
            if (enc.stage == 0) continue;
            int hourIdx = enc.hour - 1; // 0-based
            int subIdx  = enc.subCol - 1; // 0-based within hour

            int[] hourStarts = enc.stage == 1 ? s1HourStartCol : s2HourStartCol;
            int maxHours     = enc.stage == 1 ? 15 : 5;
            JSONObject[][] values = enc.stage == 1 ? s1Values : s2Values;

            if (hourIdx < 0 || hourIdx >= maxHours) continue;
            if (hourStarts == null) continue;
            int colIdx = hourStarts[hourIdx] + subIdx;
            if (colIdx >= values[0].length) continue;

            String initials = getProviderInitials(db, enc.providerUuid, initialsCache);
            fillObservations(db, enc.uuid, enc.encounterTime, initials, colIdx, values);
        }

        // Build the parameters JSON array
        root.put("parameters", buildParametersJson(s1Values, s2Values, s1ColTotal, s2ColTotal));

        // Build history lists (assessment, plan, medications)
        buildHistoryLists(root, s1Values, s2Values);
    }

    private static void buildColumnArray(JSONArray out, int stage, int maxHour,
                                         Map<String, Integer> subColCount) throws JSONException {
        for (int h = 1; h <= maxHour; h++) {
            String key = "s" + stage + "_h" + h;
            int subs = subColCount.containsKey(key) ? subColCount.get(key) : 1;
            JSONObject col = new JSONObject();
            col.put("hour", h);
            col.put("subColumns", subs);
            out.put(col);
        }
    }

    private static int countCols(int stage, int maxHour, Map<String, Integer> subColCount) {
        int total = 0;
        for (int h = 1; h <= maxHour; h++) {
            String key = "s" + stage + "_h" + h;
            total += subColCount.containsKey(key) ? subColCount.get(key) : 1;
        }
        return total;
    }

    private static int[] hourStartCols(int stage, int maxHour, Map<String, Integer> subColCount) {
        int[] starts = new int[maxHour];
        int running = 0;
        for (int h = 1; h <= maxHour; h++) {
            starts[h - 1] = running;
            String key = "s" + stage + "_h" + h;
            running += subColCount.containsKey(key) ? subColCount.get(key) : 1;
        }
        return starts;
    }

    // ── Encounter fetch ──────────────────────────────────────────────────────

    @SuppressLint("Range")
    private static List<EncounterRecord> fetchEncounters(SQLiteDatabase db, String visitUuid) {
        List<EncounterRecord> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT e.uuid, e.encounter_type_uuid, e.encounter_time, e.provider_uuid, " +
                        "       COALESCE(u.name, '') AS enc_type_name " +
                        "FROM tbl_encounter e " +
                        "LEFT JOIN tbl_uuid_dictionary u ON u.uuid = e.encounter_type_uuid " +
                        "WHERE e.visituuid = ? " +
                        "  AND (e.voided = '0' OR e.voided = 'false' OR e.voided = 'FALSE') " +
                        "  AND e.encounter_type_uuid != '" + UuidDictionary.ENCOUNTER_VISIT_COMPLETE + "' " +
                        "ORDER BY e.encounter_time ASC",
                new String[]{visitUuid});
        try {
            while (c.moveToNext()) {
                list.add(new EncounterRecord(
                        c.getString(c.getColumnIndex("uuid")),
                        c.getString(c.getColumnIndex("encounter_type_uuid")),
                        c.getString(c.getColumnIndex("enc_type_name")),
                        c.getString(c.getColumnIndex("encounter_time")),
                        c.getString(c.getColumnIndex("provider_uuid"))));
            }
        } finally { c.close(); }
        return list;
    }

    // ── Stage/hour resolution ────────────────────────────────────────────────

    @SuppressLint("Range")
    private static void resolveStageHour(EncounterRecord enc, SQLiteDatabase db) {
        if (enc.isSos()) {
            // SOS encounters: look up SOS_STAGE_HOUR obs to find their placement
            Cursor c = db.rawQuery(
                    "SELECT value FROM tbl_obs " +
                            "WHERE encounteruuid = ? AND conceptuuid = ? " +
                            "  AND (voided = '0' OR voided = 'false') LIMIT 1",
                    new String[]{enc.uuid, UuidDictionary.SOS_STAGE_HOUR});
            try {
                if (c.moveToFirst()) {
                    String val = nvl(c.getString(0));
                    Matcher m = SOS_STAGE_HOUR_PATTERN.matcher(val);
                    if (m.find()) {
                        enc.stage  = Integer.parseInt(m.group(1));
                        enc.hour   = Integer.parseInt(m.group(2));
                        enc.subCol = Integer.parseInt(m.group(3));
                        return;
                    }
                }
            } finally { c.close(); }
            // Fallback: try the regular stage-hour pattern (some SOS may use it)
        }

        // Regular encounters: the encounter type name encodes the stage/hour
        // e.g. "Stage1_Hour2_1" or "Stage2_Hour1"
        Matcher m = STAGE_HOUR_PATTERN.matcher(enc.typeName);
        if (m.find()) {
            enc.stage  = Integer.parseInt(m.group(1));
            enc.hour   = Integer.parseInt(m.group(2));
            enc.subCol = (m.group(3) != null && !m.group(3).isEmpty())
                    ? Integer.parseInt(m.group(3)) : 1;
        }
        // stage == 0 means we couldn't resolve; caller skips these
    }

    // ── Observation filling ─────────────────────────────────────────────────

    @SuppressLint("Range")
    private static void fillObservations(SQLiteDatabase db, String encounterUuid,
                                         String encounterTime, String initials,
                                         int colIdx, JSONObject[][] values) {
        Cursor c = db.rawQuery(
                "SELECT conceptuuid, value, comment FROM tbl_obs " +
                        "WHERE encounteruuid = ? " +
                        "  AND (voided = '0' OR voided = 'false' OR voided = 'FALSE')",
                new String[]{encounterUuid});
        try {
            while (c.moveToNext()) {
                String conceptUuid = nvl(c.getString(c.getColumnIndex("conceptuuid")));
                String value       = nvl(c.getString(c.getColumnIndex("value")));
                String comment     = nvl(c.getString(c.getColumnIndex("comment")));

                Integer paramIdx = UUID_TO_PARAM_IDX.get(conceptUuid);
                if (paramIdx == null) continue;
                if (paramIdx >= values.length) continue;
                if (colIdx  >= values[paramIdx].length) continue;

                try {
                    values[paramIdx][colIdx] = buildObsValue(
                            paramIdx, value, comment, encounterTime, initials,
                            values[paramIdx][colIdx]);
                } catch (JSONException e) {
                    Log.w(TAG, "buildObsValue failed for param " + paramIdx, e);
                }
            }
        } finally { c.close(); }
    }

    private static JSONObject buildObsValue(int paramIdx, String value, String comment,
                                            String obsDatetime, String initials,
                                            JSONObject existing) throws JSONException {
        if (ARRAY_PARAMS.contains(paramIdx)) {
            // Accumulate into a JSONArray wrapped in a holder object
            JSONArray arr;
            if (existing != null && existing.has("arr")) {
                arr = existing.getJSONArray("arr");
            } else {
                arr = new JSONArray();
                existing = new JSONObject();
                existing.put("arr", arr);
            }
            JSONObject entry = new JSONObject();
            entry.put("value", value);
            entry.put("initial", initials);
            entry.put("obsDatetime", obsDatetime);
            entry.put("comment", comment);
            arr.put(entry);
            return existing;

        } else if (ARRAY_JSON_PARAMS.contains(paramIdx)) {
            JSONArray arr;
            if (existing != null && existing.has("arr")) {
                arr = existing.getJSONArray("arr");
            } else {
                arr = new JSONArray();
                existing = new JSONObject();
                existing.put("arr", arr);
            }
            JSONObject entry = new JSONObject();
            entry.put("value", parseJsonOrString(value));
            entry.put("initial", initials);
            entry.put("obsDatetime", obsDatetime);
            arr.put(entry);
            return existing;

        } else if (JSON_VALUE_PARAMS.contains(paramIdx)) {
            JSONObject obs = new JSONObject();
            obs.put("value", parseJsonOrString(value));
            obs.put("initial", initials);
            obs.put("obsDatetime", obsDatetime);
            return obs;

        } else {
            // Scalar: {value, comment}
            JSONObject obs = new JSONObject();
            obs.put("value", value);
            obs.put("comment", comment);
            return obs;
        }
    }

    // ── Parameters JSON array (29 entries) ─────────────────────────────────

    private static JSONArray buildParametersJson(JSONObject[][] s1Values,
                                                 JSONObject[][] s2Values,
                                                 int s1Cols, int s2Cols) throws JSONException {
        JSONArray params = new JSONArray();
        for (int i = 0; i < 29; i++) {
            JSONObject param = new JSONObject();
            param.put("stage1", colArrayToJson(s1Values[i], i, s1Cols));
            param.put("stage2", colArrayToJson(s2Values[i], i, s2Cols));
            params.put(param);
        }
        return params;
    }

    private static JSONArray colArrayToJson(JSONObject[] cols, int paramIdx,
                                            int totalCols) throws JSONException {
        JSONArray out = new JSONArray();
        for (int c = 0; c < totalCols; c++) {
            JSONObject cell = cols != null && c < cols.length ? cols[c] : null;
            if (cell == null) {
                out.put(JSONObject.NULL);
            } else if (ARRAY_PARAMS.contains(paramIdx) || ARRAY_JSON_PARAMS.contains(paramIdx)) {
                // Unwrap the holder — the HTML expects the JSONArray directly
                out.put(cell.has("arr") ? cell.getJSONArray("arr") : JSONObject.NULL);
            } else {
                out.put(cell);
            }
        }
        return out;
    }

    // ── History lists ────────────────────────────────────────────────────────

    private static void buildHistoryLists(JSONObject root,
                                          JSONObject[][] s1Values,
                                          JSONObject[][] s2Values) throws JSONException {
        // assessmentList (param 23), planList (param 24),
        // medicineList (params 20+26), oxytocinList (params 19+27), ivFluidList (params 21+28)
        root.put("assessmentList",  flattenArrayParam(22, s1Values, s2Values));
        root.put("planList",        flattenArrayParam(23, s1Values, s2Values));
        root.put("medicineList",    mergeArrayParams(new int[]{20, 26}, s1Values, s2Values));
        root.put("oxytocinList",    mergeArrayParams(new int[]{19, 27}, s1Values, s2Values));
        root.put("ivFluidList",     mergeArrayParams(new int[]{21, 28}, s1Values, s2Values));
    }

    private static JSONArray flattenArrayParam(int paramIdx,
                                               JSONObject[][] s1Values,
                                               JSONObject[][] s2Values) throws JSONException {
        JSONArray result = new JSONArray();
        appendFrom(result, s1Values[paramIdx]);
        appendFrom(result, s2Values[paramIdx]);
        return result;
    }

    private static JSONArray mergeArrayParams(int[] paramIdxs,
                                              JSONObject[][] s1Values,
                                              JSONObject[][] s2Values) throws JSONException {
        JSONArray result = new JSONArray();
        for (int idx : paramIdxs) {
            appendFrom(result, s1Values[idx]);
            appendFrom(result, s2Values[idx]);
        }
        return result;
    }

    private static void appendFrom(JSONArray target, JSONObject[] cols) throws JSONException {
        if (cols == null) return;
        for (JSONObject cell : cols) {
            if (cell == null) continue;
            if (cell.has("arr")) {
                JSONArray arr = cell.getJSONArray("arr");
                for (int i = 0; i < arr.length(); i++) target.put(arr.get(i));
            } else if (cell.has("value")) {
                target.put(cell);
            }
        }
    }

    // ── Visit complete card ──────────────────────────────────────────────────

    @SuppressLint("Range")
    private static void buildVisitCompleteData(SQLiteDatabase db, String visitUuid,
                                               JSONObject root) throws JSONException {
        // Check for a ENCOUNTER_VISIT_COMPLETE encounter for this visit
        Cursor ec = db.rawQuery(
                "SELECT uuid, provider_uuid FROM tbl_encounter " +
                        "WHERE visituuid = ? AND encounter_type_uuid = ? " +
                        "  AND (voided = '0' OR voided = 'false') LIMIT 1",
                new String[]{visitUuid, UuidDictionary.ENCOUNTER_VISIT_COMPLETE});
        String vcEncounterUuid = null;
        String vcProviderUuid  = null;
        try {
            if (ec.moveToFirst()) {
                vcEncounterUuid = ec.getString(0);
                vcProviderUuid  = ec.getString(1);
            }
        } finally { ec.close(); }

        if (vcEncounterUuid == null) {
            root.put("visitComplete", JSONObject.NULL);
            return;
        }

        // Gather obs for the visit-complete encounter
        JSONObject vc = new JSONObject();
        Map<String, String> initialsCache = new HashMap<>();
        String initials = getProviderInitials(db, vcProviderUuid, initialsCache);
        vc.put("initial", initials);

        Cursor oc = db.rawQuery(
                "SELECT conceptuuid, value, comment FROM tbl_obs " +
                        "WHERE encounteruuid = ? " +
                        "  AND (voided = '0' OR voided = 'false')",
                new String[]{vcEncounterUuid});
        try {
            while (oc.moveToNext()) {
                String conceptUuid = nvl(oc.getString(oc.getColumnIndex("conceptuuid")));
                String value       = nvl(oc.getString(oc.getColumnIndex("value")));
                String comment     = nvl(oc.getString(oc.getColumnIndex("comment")));

                // Map concept UUIDs to VC fields
                switch (conceptUuid) {
                    case UuidDictionary.BIRTH_OUTCOME:        vc.put("birthOutcome", value); break;
                    case UuidDictionary.BIRTH_WEIGHT:         vc.put("birthWeight", value); break;
                    case UuidDictionary.APGAR_1_MIN:          vc.put("apgar1Min", value); break;
                    case UuidDictionary.APGAR_5_MIN:          vc.put("apgar5Min", value); break;
                    case UuidDictionary.SEX:                  vc.put("sex", value); break;
                    case UuidDictionary.BABY_STATUS:          vc.put("babyStatus", value); break;
                    case UuidDictionary.MOTHER_STATUS:        vc.put("motherStatus", value); break;
                    case UuidDictionary.MOTHER_DECEASED:      vc.put("motherDeceased", value); break;
                    case UuidDictionary.MOTHER_DECEASED_FLAG: vc.put("motherDeceasedFlag", value); break;
                    case UuidDictionary.REFER_TYPE:           vc.put("referType", value); break;
                    case UuidDictionary.REFER_HOSPITAL:       vc.put("referHospital", value); break;
                    case UuidDictionary.REFER_DR_NAME:        vc.put("referDrName", value); break;
                    case UuidDictionary.REFER_NOTE:           vc.put("referNote", value); break;
                    case UuidDictionary.NEONATAL_DEATH:       vc.put("neonatalDeath", value); break;
                    case UuidDictionary.NEWBORN_DISCHARGE_TYPE: vc.put("newbornDischargeType", value); break;
                    case UuidDictionary.LABOUR_OTHER:         vc.put("labourOther", value); break;
                    case UuidDictionary.END_2ND_STAGE_OTHER:  vc.put("end2ndStageOther", value); break;
                    case UuidDictionary.OUT_OF_TIME:          vc.put("outOfTime", value); break;
                    case UuidDictionary.ASSESSMENT:           vc.put("assessment", value); break;
                    case UuidDictionary.PLAN:                 vc.put("plan", value); break;
                }
            }
        } finally { oc.close(); }

        root.put("visitComplete", vc);
    }

    // ── Provider initials ────────────────────────────────────────────────────

    @SuppressLint("Range")
    private static String getProviderInitials(SQLiteDatabase db, String providerUuid,
                                              Map<String, String> cache) {
        if (providerUuid == null || providerUuid.isEmpty()) return "";
        if (cache.containsKey(providerUuid)) return cache.get(providerUuid);

        Cursor c = db.rawQuery(
                "SELECT given_name, family_name FROM tbl_provider WHERE useruuid = ? LIMIT 1",
                new String[]{providerUuid});
        String initials = "";
        try {
            if (c.moveToFirst()) {
                String given  = nvl(c.getString(c.getColumnIndex("given_name")));
                String family = nvl(c.getString(c.getColumnIndex("family_name")));
                if (!given.isEmpty())  initials += given.charAt(0);
                if (!family.isEmpty()) initials += family.charAt(0);
                initials = initials.toUpperCase();
            }
        } finally { c.close(); }

        cache.put(providerUuid, initials);
        return initials;
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    /** Returns a JSONObject if value looks like JSON, otherwise the raw string. */
    private static Object parseJsonOrString(String value) {
        if (value != null && value.startsWith("{")) {
            try { return new JSONObject(value); } catch (JSONException ignored) {}
        }
        if (value != null && value.startsWith("[")) {
            try { return new JSONArray(value); } catch (JSONException ignored) {}
        }
        return value != null ? value : "";
    }
}