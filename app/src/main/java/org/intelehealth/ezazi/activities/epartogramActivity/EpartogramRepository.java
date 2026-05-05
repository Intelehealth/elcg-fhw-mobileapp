package org.intelehealth.ezazi.activities.epartogramActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.EncounterDAO;
import org.intelehealth.ezazi.database.dao.ObsDAO;
import org.intelehealth.ezazi.models.dto.ObsDTO;
import org.intelehealth.ezazi.partogram.PartogramConstants;
import org.intelehealth.ezazi.utilities.UuidDictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assembles an {@link EpartogramSnapshot} from local SQLite for offline rendering.
 *
 * Bucketing is driven by encounter-type names of the form {@code Stage{S}_Hour{H}_{SubCol}}
 * stored in {@code tbl_uuid_dictionary} (see InteleHealthDatabaseHelper seed data,
 * e.g. "Stage1_Hour3_2"). Each non-SOS encounter is mapped to a stage / hour / sub-column
 * cell, and observations on that encounter are placed into the matching parameter row.
 *
 * Parameter index order MUST match epartogram.component.html (parameters[0]..[N]).
 */
public class EpartogramRepository {

    private static final String TAG = "EpartogramRepository";

    /**
     * Order matters — Angular indexes parameters by position.
     * See epartogram.component.html: parameters[0] (Companion), parameters[4] (Baseline FHR),
     * parameters[15] (Contractions), parameters[17] (Cervix), parameters[19] (Oxytocin),
     * parameters[22] (Assessment), parameters[25] (Urine Acetone), etc.
     */
    private static final PartogramConstants.Params[] PARAM_ORDER = new PartogramConstants.Params[] {
            PartogramConstants.Params.COMPANION,                // 0
            PartogramConstants.Params.PAIN_RELIEF,              // 1
            PartogramConstants.Params.ORAL_FLUID,               // 2
            PartogramConstants.Params.POSTURE,                  // 3
            PartogramConstants.Params.BASELINE_FHR,             // 4
            PartogramConstants.Params.FHR_DEC,                  // 5
            PartogramConstants.Params.AMNIOTIC_FLUID,           // 6
            PartogramConstants.Params.FETAL_POSITION,           // 7
            PartogramConstants.Params.CAPUT,                    // 8
            PartogramConstants.Params.MOULDING,                 // 9
            PartogramConstants.Params.PULSE,                    // 10
            PartogramConstants.Params.SYSTOLIC_BP,              // 11
            PartogramConstants.Params.DIASTOLIC_BP,             // 12
            PartogramConstants.Params.TEMPERATURE,              // 13
            PartogramConstants.Params.URINE_PROTEIN,            // 14
            PartogramConstants.Params.CONTRACTION_PER_10_MIN,   // 15
            PartogramConstants.Params.DURATION_OF_CONTRACTION,  // 16
            PartogramConstants.Params.CERVIX_PLOT,              // 17
            PartogramConstants.Params.DESCENT_PLOT,             // 18
            PartogramConstants.Params.OXYTOCIN,                 // 19
            PartogramConstants.Params.MEDICINE,                 // 20
            PartogramConstants.Params.IV_FLUID,                 // 21
            PartogramConstants.Params.ASSESSMENT,               // 22
            PartogramConstants.Params.PLAN,                     // 23
            PartogramConstants.Params.SUPERVISOR_DOCTOR,        // 24
            PartogramConstants.Params.URINE_ACETONE,            // 25
    };

    private static final Pattern STAGE_HOUR_PATTERN =
            Pattern.compile("Stage(\\d+)_Hour(\\d+)_(\\d+)", Pattern.CASE_INSENSITIVE);

    private final EncounterDAO encounterDAO = new EncounterDAO();
    private final ObsDAO obsDAO = new ObsDAO();

    public EpartogramSnapshot buildSnapshot(String visitUuid) {
        EpartogramSnapshot snapshot = new EpartogramSnapshot();
        if (visitUuid == null || visitUuid.isEmpty()) {
            Log.w(TAG, "buildSnapshot called with empty visitUuid");
            return snapshot;
        }

        snapshot.pinfo = loadPatientInfo(visitUuid);
        loadVisitCompletion(visitUuid, snapshot);
        loadGrid(visitUuid, snapshot);
        loadHistories(visitUuid, snapshot);
        return snapshot;
    }

    // ---------------------------------------------------------------------------------------
    // Patient header (pinfo)
    // ---------------------------------------------------------------------------------------

    private EpartogramSnapshot.PatientInfo loadPatientInfo(String visitUuid) {
        EpartogramSnapshot.PatientInfo p = new EpartogramSnapshot.PatientInfo();
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT pat.first_name, pat.middle_name, pat.last_name " +
                        "FROM tbl_patient pat JOIN tbl_visit v ON v.patientuuid = pat.uuid " +
                        "WHERE v.uuid = ? LIMIT 1",
                new String[]{visitUuid})) {
            if (c.moveToFirst()) {
                String first = safeStr(c, "first_name");
                String middle = safeStr(c, "middle_name");
                String last = safeStr(c, "last_name");
                p.name = join(first, middle, last);
            }
        } catch (Exception e) {
            Log.w(TAG, "loadPatientInfo: " + e.getMessage());
        }

        // TODO: populate the remaining pinfo fields from the visit-note observations.
        // The Angular online flow currently sources these from the server; offline they
        // must be read from tbl_obs joined to the visit-note encounter for this visit.
        // Concept UUIDs to wire up (move to UuidDictionary when confirmed):
        //   Parity, Gravida, LMP, EDD, Labour onset, Active labour diagnosed,
        //   Membrane ruptured timestamp, Risk factors.
        return p;
    }

    // ---------------------------------------------------------------------------------------
    // Visit completion footer
    // ---------------------------------------------------------------------------------------

    private void loadVisitCompletion(String visitUuid, EpartogramSnapshot snapshot) {
        String visitCompleteEnc = encounterDAO.getVisitCompleteEncounterByVisitUUID(visitUuid);
        snapshot.visitCompleted = visitCompleteEnc != null && !visitCompleteEnc.isEmpty();
        if (!snapshot.visitCompleted) return;

        Map<String, ObsDTO> obs = obsByConcept(obsDAO.getOBSByEncounterUUID(visitCompleteEnc));
        snapshot.birthOutcome = valueOf(obs, UuidDictionary.BIRTH_OUTCOME);
        snapshot.birthWeight = valueOf(obs, UuidDictionary.BIRTH_WEIGHT);
        snapshot.apgar1 = valueOf(obs, UuidDictionary.APGAR_1_MIN);
        snapshot.apgar5 = valueOf(obs, UuidDictionary.APGAR_5_MIN);
        snapshot.babyStatus = valueOf(obs, UuidDictionary.BABY_STATUS);
        snapshot.outOfTimeReason = valueOf(obs, UuidDictionary.OUT_OF_TIME);
        snapshot.motherDeceased = valueOf(obs, UuidDictionary.MOTHER_DECEASED_FLAG);
        snapshot.motherDeceasedReason = valueOf(obs, UuidDictionary.MOTHER_DECEASED);

        // TODO: derive visitCompleteReason ('Out Of Time' / 'Other' / 'Newborn' / specific refer reason),
        // referTypeOtherReason, birthOutcomeOther, babyGender from the same obs set.
        // The Angular component branches on visitCompleteReason — keep these strings stable.
    }

    // ---------------------------------------------------------------------------------------
    // Grid (encounters → cells)
    // ---------------------------------------------------------------------------------------

    private void loadGrid(String visitUuid, EpartogramSnapshot snapshot) {
        for (PartogramConstants.Params p : PARAM_ORDER) {
            EpartogramSnapshot.Parameter row = new EpartogramSnapshot.Parameter();
            row.label = p.value;
            row.conceptUuid = p.conceptId;
            snapshot.parameters.add(row);
        }

        List<EncounterRow> encounters = loadVisitEncounters(visitUuid);
        if (encounters.isEmpty()) return;

        // Stage S → hour H → list of sub-column slots (each holds an EncounterRow or null).
        Map<Integer, Map<Integer, List<EncounterRow>>> byStageHour = new HashMap<>();
        List<EncounterRow> sosEncounters = new ArrayList<>();

        for (EncounterRow row : encounters) {
            if (row.encounterTypeUuid != null && row.encounterTypeUuid.equalsIgnoreCase(UuidDictionary.LCG_SOS)) {
                sosEncounters.add(row);
                snapshot.sosEncounterUUIDs.add(row.uuid);
                continue;
            }
            Matcher m = row.encounterTypeName == null ? null : STAGE_HOUR_PATTERN.matcher(row.encounterTypeName);
            if (m == null || !m.find()) continue;

            int stage = Integer.parseInt(m.group(1));
            int hour = Integer.parseInt(m.group(2));
            int sub = Integer.parseInt(m.group(3));
            row.stage = stage;
            row.hour = hour;
            row.subCol = sub;

            Map<Integer, List<EncounterRow>> hourMap =
                    byStageHour.computeIfAbsent(stage, k -> new HashMap<>());
            List<EncounterRow> slots = hourMap.computeIfAbsent(hour, k -> new ArrayList<>());
            while (slots.size() < sub) slots.add(null);
            slots.set(sub - 1, row);
        }

        flattenStage(byStageHour.get(1), 1, snapshot, snapshot.timeStage1, snapshot.timeFullStage1,
                snapshot.subColsPerHourStage1, snapshot.encuuid1, snapshot.encuuid1Full,
                snapshot.initialsStage1);
        flattenStage(byStageHour.get(2), 2, snapshot, snapshot.timeStage2, snapshot.timeFullStage2,
                snapshot.subColsPerHourStage2, snapshot.encuuid2, snapshot.encuuid2Full,
                snapshot.initialsStage2);
    }

    private void flattenStage(Map<Integer, List<EncounterRow>> hourMap,
                              int stage,
                              EpartogramSnapshot snapshot,
                              List<String> timeStage,
                              List<String> timeFull,
                              List<Integer> subColsPerHour,
                              List<EpartogramSnapshot.EncounterRef> encByHour,
                              List<EpartogramSnapshot.EncounterRef> encByCol,
                              List<String> initialsByHour) {
        if (hourMap == null || hourMap.isEmpty()) return;
        List<Integer> hours = new ArrayList<>(hourMap.keySet());
        Collections.sort(hours);

        int paramCount = snapshot.parameters.size();
        for (int p = 0; p < paramCount; p++) {
            // Pre-grow stage list to match column count below.
        }

        for (Integer hour : hours) {
            List<EncounterRow> slots = hourMap.get(hour);
            int subCount = slots == null ? 0 : slots.size();
            if (subCount == 0) continue;

            subColsPerHour.add(subCount);
            EncounterRow firstNonNull = firstNonNull(slots);
            timeStage.add(firstNonNull == null ? null : firstNonNull.encounterTime);
            encByHour.add(toRef(firstNonNull));
            initialsByHour.add(firstNonNull == null ? null : firstNonNull.initial);

            for (int sub = 0; sub < subCount; sub++) {
                EncounterRow row = slots.get(sub);
                timeFull.add(row == null ? null : row.encounterTime);
                encByCol.add(toRef(row));

                Map<String, ObsDTO> obsForRow = row == null
                        ? Collections.emptyMap()
                        : obsByConcept(obsDAO.getOBSByEncounterUUID(row.uuid));

                for (int p = 0; p < paramCount; p++) {
                    EpartogramSnapshot.Parameter param = snapshot.parameters.get(p);
                    EpartogramSnapshot.Cell cell = buildCell(param.conceptUuid, obsForRow, row);
                    List<EpartogramSnapshot.Cell> target = stage == 1 ? param.stage1values : param.stage2values;
                    target.add(cell);
                }
            }
        }
    }

    private EpartogramSnapshot.Cell buildCell(String conceptUuid,
                                              Map<String, ObsDTO> obsByConcept,
                                              EncounterRow row) {
        ObsDTO obs = obsByConcept == null ? null : obsByConcept.get(conceptUuid);
        if (obs == null) return null;
        EpartogramSnapshot.Cell cell = new EpartogramSnapshot.Cell();
        cell.value = obs.getValue();
        cell.comment = obs.getComment();
        cell.initial = row == null ? null : row.initial;
        cell.obsDatetime = obs.getObsServerModifiedDate() != null
                ? obs.getObsServerModifiedDate()
                : (row == null ? null : row.encounterTime);

        // TODO: Oxytocin (concept 9d316d82-...), IV Fluids (concept 98c5881f-...), and Medicine
        // (concept c38c0c50-...) store structured payloads. The current obs.value is a serialised
        // form — parse it into EpartogramSnapshot.InfusionValue / MedicineEntry so the Angular
        // template's item?.value?.strength / item?.value?.type / item.replace('::',' ') bindings
        // work. Until parsed, the offline view will show the raw string.
        return cell;
    }

    // ---------------------------------------------------------------------------------------
    // Decision histories (assessment / plan / med / oxytocin / iv accordions)
    // ---------------------------------------------------------------------------------------

    private void loadHistories(String visitUuid, EpartogramSnapshot snapshot) {
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getReadableDatabase();
        snapshot.assessmentHistory = historyFor(db, visitUuid, PartogramConstants.Params.ASSESSMENT.conceptId);
        snapshot.planHistory = historyFor(db, visitUuid, PartogramConstants.Params.PLAN.conceptId);
        snapshot.medicationPrescribedHistory = historyFor(db, visitUuid, PartogramConstants.Params.PRESCRIBED_MEDICINE.conceptId);
        snapshot.oxytocinPrescribedHistory = historyFor(db, visitUuid, PartogramConstants.Params.PRESCRIBED_OXYTOCIN.conceptId);
        snapshot.ivPrescribedHistory = historyFor(db, visitUuid, PartogramConstants.Params.PRESCRIBED_IV_FLUID.conceptId);
    }

    private List<EpartogramSnapshot.HistoryItem> historyFor(SQLiteDatabase db, String visitUuid, String conceptUuid) {
        List<EpartogramSnapshot.HistoryItem> out = new ArrayList<>();
        String sql = "SELECT o.value, o.obsservermodifieddate, o.creator " +
                "FROM tbl_obs o JOIN tbl_encounter e ON o.encounteruuid = e.uuid " +
                "WHERE e.visituuid = ? AND o.conceptuuid = ? AND o.voided = '0' " +
                "ORDER BY o.obsservermodifieddate DESC";
        try (Cursor c = db.rawQuery(sql, new String[]{visitUuid, conceptUuid})) {
            while (c.moveToNext()) {
                EpartogramSnapshot.HistoryItem item = new EpartogramSnapshot.HistoryItem();
                item.value = safeStr(c, "value");
                item.obsDatetime = safeStr(c, "obsservermodifieddate");
                item.initial = safeStr(c, "creator");
                out.add(item);
            }
        } catch (Exception e) {
            Log.w(TAG, "historyFor(" + conceptUuid + "): " + e.getMessage());
        }
        // TODO: parse oxytocin/IV value strings into structured InfusionValue so the
        // accordion's "STARTED OXYTOCIN - 10 U/L (30 drops/minute)" labels render.
        return out;
    }

    // ---------------------------------------------------------------------------------------
    // Encounter loading (joined to type-name dictionary for stage/hour parsing)
    // ---------------------------------------------------------------------------------------

    private List<EncounterRow> loadVisitEncounters(String visitUuid) {
        List<EncounterRow> out = new ArrayList<>();
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getReadableDatabase();
        String sql =
                "SELECT e.uuid, e.encounter_type_uuid, e.encounter_time, e.provider_uuid, d.name " +
                        "FROM tbl_encounter e LEFT JOIN tbl_uuid_dictionary d ON d.uuid = e.encounter_type_uuid " +
                        "WHERE e.visituuid = ? AND e.voided IN ('0','false','FALSE') " +
                        "AND e.encounter_type_uuid != ? " +
                        "ORDER BY e.encounter_time ASC";
        try (Cursor c = db.rawQuery(sql, new String[]{visitUuid, UuidDictionary.ENCOUNTER_VISIT_COMPLETE})) {
            while (c.moveToNext()) {
                EncounterRow row = new EncounterRow();
                row.uuid = safeStr(c, "uuid");
                row.encounterTypeUuid = safeStr(c, "encounter_type_uuid");
                row.encounterTypeName = safeStr(c, "name");
                row.encounterTime = safeStr(c, "encounter_time");
                row.providerUuid = safeStr(c, "provider_uuid");
                row.initial = providerInitial(db, row.providerUuid);
                out.add(row);
            }
        } catch (Exception e) {
            Log.w(TAG, "loadVisitEncounters: " + e.getMessage());
        }
        return out;
    }

    private String providerInitial(SQLiteDatabase db, String providerUuid) {
        if (providerUuid == null || providerUuid.isEmpty()) return null;
        try (Cursor c = db.rawQuery(
                "SELECT given_name, family_name FROM tbl_provider WHERE uuid = ? LIMIT 1",
                new String[]{providerUuid})) {
            if (c.moveToFirst()) {
                String given = safeStr(c, "given_name");
                String family = safeStr(c, "family_name");
                String initial = "";
                if (given != null && !given.isEmpty()) initial += given.charAt(0);
                if (family != null && !family.isEmpty()) initial += family.charAt(0);
                return initial.isEmpty() ? null : initial.toUpperCase();
            }
        } catch (Exception ignored) { }
        return null;
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private static Map<String, ObsDTO> obsByConcept(List<ObsDTO> obs) {
        Map<String, ObsDTO> map = new HashMap<>();
        if (obs == null) return map;
        for (ObsDTO o : obs) {
            if (o.getConceptuuid() != null) map.put(o.getConceptuuid(), o);
        }
        return map;
    }

    private static String valueOf(Map<String, ObsDTO> obs, String conceptUuid) {
        ObsDTO o = obs.get(conceptUuid);
        return o == null ? null : o.getValue();
    }

    private static EpartogramSnapshot.EncounterRef toRef(EncounterRow row) {
        if (row == null) return null;
        EpartogramSnapshot.EncounterRef ref = new EpartogramSnapshot.EncounterRef();
        ref.encUuid = row.uuid;
        ref.encounterTime = row.encounterTime;
        return ref;
    }

    private static EncounterRow firstNonNull(List<EncounterRow> rows) {
        if (rows == null) return null;
        for (EncounterRow r : rows) if (r != null) return r;
        return null;
    }

    private static String safeStr(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        if (idx < 0 || c.isNull(idx)) return null;
        return c.getString(idx);
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static class EncounterRow {
        String uuid;
        String encounterTypeUuid;
        String encounterTypeName;
        String encounterTime;
        String providerUuid;
        String initial;
        int stage;
        int hour;
        int subCol;
    }
}
