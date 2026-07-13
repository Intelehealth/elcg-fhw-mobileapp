package org.intelehealth.ezazi.activities.patientDetailActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.VisitAttributeListDAO;
import org.intelehealth.ezazi.models.dto.VisitAttributeDTO;
import org.intelehealth.ezazi.utilities.UuidDictionary;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Loads read-only Past Visit Details for a patient's closed visits: obstetric visit
 * attributes (active labour, risk factors, parity) plus the delivery-outcome observations
 * (mode of delivery, baby/mother status) recorded on the visit-complete encounter.
 * The "Final Outcome Report" document is not implemented in this app, so it stays blank.
 */
public class PastVisitLoader {

    private PastVisitLoader() {
    }

    public static List<PastVisitDetails> loadForPatient(String patientUuid) {
        List<PastVisitDetails> list = new ArrayList<>();
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getReadableDatabase();
        VisitAttributeListDAO attrDao = new VisitAttributeListDAO();

        Cursor visits = db.rawQuery(
                "SELECT uuid, startdate, enddate FROM tbl_visit WHERE patientuuid = ? " +
                        "AND voided IN ('0','false','FALSE') " +
                        "AND ((enddate IS NOT NULL AND enddate <> '') " +
                        "OR uuid IN (SELECT visituuid FROM tbl_encounter WHERE encounter_type_uuid = ?)) " +
                        "ORDER BY startdate DESC",
                new String[]{patientUuid, UuidDictionary.ENCOUNTER_VISIT_COMPLETE});

        while (visits.moveToNext()) {
            String visitUuid = visits.getString(0);
            PastVisitDetails details = new PastVisitDetails();
            details.visitUuid = visitUuid;
            details.visitDate = formatDateTime(visits.getString(1));
            details.deliveryDate = formatDateTime(visits.getString(2));
            details.activeLabourDiagnosed = attrDao.getVisitAttributeValue(visitUuid, VisitAttributeDTO.Columns.ACTIVE_LABOR_DIAGNOSED.uuid);
            details.riskFactors = attrDao.getVisitAttributeValue(visitUuid, VisitAttributeDTO.Columns.RISK_FACTORS.uuid);
            details.parity = formatParity(attrDao.getVisitAttributeValue(visitUuid, VisitAttributeDTO.Columns.PARITY.uuid));

            String vce = visitCompleteEncounter(db, visitUuid);
            if (!vce.isEmpty()) {
                details.modeOfDelivery = obsValue(db, vce, UuidDictionary.MODE_OF_DELIVERY);
                details.babyStatus = obsValue(db, vce, UuidDictionary.BABY_STATUS);
                details.motherStatus = resolveMotherStatus(db, vce);
            }
            list.add(details);
        }
        visits.close();
        return list;
    }

    private static String visitCompleteEncounter(SQLiteDatabase db, String visitUuid) {
        String uuid = "";
        Cursor cursor = db.rawQuery(
                "SELECT uuid FROM tbl_encounter WHERE visituuid = ? AND encounter_type_uuid = ?",
                new String[]{visitUuid, UuidDictionary.ENCOUNTER_VISIT_COMPLETE});
        if (cursor.moveToFirst()) uuid = cursor.getString(0);
        cursor.close();
        return uuid == null ? "" : uuid;
    }

    private static String obsValue(SQLiteDatabase db, String encounterUuid, String conceptUuid) {
        String value = "";
        Cursor cursor = db.rawQuery(
                "SELECT value FROM tbl_obs WHERE encounteruuid = ? AND conceptuuid = ? AND voided IN ('0','false','FALSE')",
                new String[]{encounterUuid, conceptUuid});
        if (cursor.moveToLast()) value = cursor.getString(0);
        cursor.close();
        return value == null ? "" : value;
    }

    /**
     * Mother status: the recorded status text when the mother is alive; otherwise "Deceased"
     * if maternal death was recorded on either path — MOTHER_DECEASED_FLAG (Stage 2 completion)
     * or MOTHER_DECEASED (Stage 1 completion). Stage-agnostic, so it works whichever way the
     * visit was closed.
     */
    private static String resolveMotherStatus(SQLiteDatabase db, String vce) {
        String status = obsValue(db, vce, UuidDictionary.MOTHER_STATUS);
        if (!TextUtils.isEmpty(status)) return status;
        boolean deceased = obsValue(db, vce, UuidDictionary.MOTHER_DECEASED_FLAG).equalsIgnoreCase("YES")
                || !TextUtils.isEmpty(obsValue(db, vce, UuidDictionary.MOTHER_DECEASED));
        return deceased ? "Deceased" : status;
    }

    private static String formatParity(String parity) {
        return parity == null ? "" : parity.replace(",", ", ");
    }

    private static String formatDateTime(String value) {
        if (TextUtils.isEmpty(value)) return "";
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd HH:mm:ss"};
        for (String pattern : patterns) {
            try {
                Date parsed = new SimpleDateFormat(pattern, Locale.ENGLISH).parse(value);
                if (parsed != null) {
                    return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(parsed);
                }
            } catch (Exception ignored) {
            }
        }
        return value;
    }
}
