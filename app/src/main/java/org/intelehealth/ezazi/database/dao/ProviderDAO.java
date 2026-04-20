package org.intelehealth.ezazi.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.app.IntelehealthApplication;
import org.intelehealth.ezazi.models.dto.ProviderDTO;
import org.intelehealth.ezazi.utilities.SessionManager;
import org.intelehealth.ezazi.utilities.exception.DAOException;

import java.util.ArrayList;
import java.util.List;

public class ProviderDAO {

    long createdRecordsCount = 0;

    public boolean insertProviders(List<ProviderDTO> providerDTOS) throws DAOException {

        boolean isInserted = true;
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        db.beginTransaction();
        try {
            for (ProviderDTO provider : providerDTOS) {
                createProviders(provider, db);
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            isInserted = false;
            throw new DAOException(e.getMessage(), e);
        } finally {
            db.endTransaction();

        }

        return isInserted;
    }

    private boolean createProviders(ProviderDTO provider, SQLiteDatabase db) throws DAOException {
        boolean isCreated = true;

        ContentValues values = new ContentValues();
        try {
            values.put("uuid", provider.getUuid());
            values.put("identifier", provider.getIdentifier());
            values.put("given_name", provider.getGivenName());
            values.put("family_name", provider.getFamilyName());
            values.put("role", provider.getRole());
            values.put("useruuid", provider.getUserUuid());
            values.put("voided", provider.getVoided());
            values.put("modified_date", AppConstants.dateAndTimeUtils.currentDateTime());
            values.put("sync", "TRUE");

            createdRecordsCount = db.insertWithOnConflict("tbl_provider", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLException e) {
            isCreated = false;
            throw new DAOException(e.getMessage(), e);
        } finally {
        }
        return isCreated;
    }

    public List<String> getProvidersList() throws DAOException {
        List<String> providersList = new ArrayList<>();
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        db.beginTransaction();
        try {
            String query = "select distinct a.uuid,a.given_name,a.family_name from tbl_provider a, tbl_encounter b , tbl_visit c where a.uuid=b.provider_uuid and b.visituuid=c.uuid";
            Cursor cursor = db.rawQuery(query, new String[]{});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    providersList.add(cursor.getString(cursor.getColumnIndexOrThrow("given_name")) + cursor.getString(cursor.getColumnIndexOrThrow("family_name")));

                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }
        return providersList;

    }

    public List<String> getProvidersUuidList() throws DAOException {
        List<String> providersList = new ArrayList<>();
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        db.beginTransaction();
        try {
            String query = "select distinct a.uuid,a.given_name,a.family_name from tbl_provider a, tbl_encounter b , tbl_visit c where a.uuid=b.provider_uuid and b.visituuid=c.uuid";
            Cursor cursor = db.rawQuery(query, new String[]{});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    providersList.add(cursor.getString(cursor.getColumnIndexOrThrow("uuid")));

                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }
        return providersList;

    }

    public String getProviderGiven_Lastname(String uuid) throws DAOException {
        String fullname = "";
        String givenname = "", familyname = "";
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        db.beginTransaction();
        try {
            String query = "select * from tbl_provider where uuid = ?";
            Cursor cursor = db.rawQuery(query, new String[]{uuid});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    givenname = cursor.getString(cursor.getColumnIndexOrThrow("given_name"));
                    familyname = cursor.getString(cursor.getColumnIndexOrThrow("family_name"));
                    fullname = givenname + " " + familyname;
                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }

        if (!fullname.equalsIgnoreCase(""))
            return fullname;
        else
            return "Test Doctor";

    }

    public String getProviderName(String userUuid) throws DAOException {
        String fullname = "";
        String givenname = "", familyname = "";
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        db.beginTransaction();
        try {
            String query = "select * from tbl_provider where useruuid = ?";
            Cursor cursor = db.rawQuery(query, new String[]{userUuid});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    givenname = cursor.getString(cursor.getColumnIndexOrThrow("given_name"));
                    familyname = cursor.getString(cursor.getColumnIndexOrThrow("family_name"));
                    fullname = givenname + " " + familyname;
                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }

        if (!fullname.equalsIgnoreCase(""))
            return fullname;
        else
            return "Test Doctor";

    }

    public String getProviderNameById(String providerId) throws DAOException {
        String fullname = "";
        String givenname = "", familyname = "";
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        db.beginTransaction();
        try {
            String query = "select * from tbl_provider where uuid = ?";
            Cursor cursor = db.rawQuery(query, new String[]{providerId});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    givenname = cursor.getString(cursor.getColumnIndexOrThrow("given_name"));
                    familyname = cursor.getString(cursor.getColumnIndexOrThrow("family_name"));
                    fullname = givenname + " " + familyname;
                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }

        if (!fullname.equalsIgnoreCase(""))
            return fullname;
        else
            return "Test Doctor";

    }


    public List<ProviderDTO> getDoctorList() throws DAOException {
        List<ProviderDTO> providersList = new ArrayList<>();
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        db.beginTransaction();
        try {
            String query = "select * from tbl_provider where role='Organizational: Doctor'";
            Cursor cursor = db.rawQuery(query, new String[]{});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    ProviderDTO providerDTO = new ProviderDTO();
                    providerDTO.setFamilyName(cursor.getString(cursor.getColumnIndexOrThrow("family_name")));
                    providerDTO.setGivenName(cursor.getString(cursor.getColumnIndexOrThrow("given_name")));
                    providerDTO.setUuid(cursor.getString(cursor.getColumnIndexOrThrow("uuid")));
                    providerDTO.setIdentifier(cursor.getString(cursor.getColumnIndexOrThrow("identifier")));
                    providerDTO.setRole(cursor.getString(cursor.getColumnIndexOrThrow("role")));
                    providerDTO.setUserUuid(cursor.getString(cursor.getColumnIndexOrThrow("useruuid")));

                    providersList.add(providerDTO);


                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }
        return providersList;
    }

    public List<ProviderDTO> getNurseList(
            Boolean isFromHome
    ) throws DAOException {
        List<ProviderDTO> providersList = new ArrayList<>();
        String providerId = new SessionManager(IntelehealthApplication.getAppContext()).getProviderID();
        String ward = checkNurseWard(providerId);
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        db.beginTransaction();
        try {
            // EZ-757
            // Filtering nurses based on ward assignment

            // Default behavior:
            // - If nurse is assigned to "Labor Ward":
            //     → Return nurses from "Labor Ward" + "Unassigned Ward"
            // - If nurse is assigned to "Postnatal Ward":
            //     → Return nurses from "Postnatal Ward" + "Unassigned Ward"
            // - If nurse has no ward or is "Unassigned Ward":
            //     → Return only "Unassigned Ward" nurses

            // Special case: When called from Timeline Activity and we need to shift the patient to postnatal ward:
            // - If nurse is assigned to "Labor Ward":
            //     → Return nurses from "Postnatal Ward" + "Unassigned Ward"
            //   (This overrides the default behavior for Labor Ward in timeline context)

            // Note:
            // - "Unassigned Ward" nurses are always included where applicable
            String query = "select * from tbl_provider where role='Organizational: Nurse'";
            if(ward.equals("Labor Ward") && isFromHome){
                query = "select p.*, pa.* from tbl_provider as p " +
                        "left join tbl_provider_attribute as pa on  p.uuid = pa.provideruuid " +
                        "where p.role='Organizational: Nurse'" +
                        "and (pa.value is null or pa.value != 'Post Natal Ward') order by pa.value desc";
            }else if(ward.equals("Post Natal Ward") && isFromHome){
                query = "select p.*, pa.* from tbl_provider as p " +
                        "left join tbl_provider_attribute as pa on  p.uuid = pa.provideruuid " +
                        "where p.role='Organizational: Nurse'" +
                        "and (pa.value is null or pa.value != 'Labor Ward') order by pa.value desc";
            }else if(ward.equals("Labor Ward") && !isFromHome){
                query = "select p.*, pa.* from tbl_provider as p " +
                        "left join tbl_provider_attribute as pa on  p.uuid = pa.provideruuid " +
                        "where p.role='Organizational: Nurse'" +
                        "and (pa.value is null or pa.value != 'Labor Ward') order by pa.value desc";
            }
            Cursor cursor = db.rawQuery(query, new String[]{});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    ProviderDTO providerDTO = new ProviderDTO();
                    providerDTO.setFamilyName(cursor.getString(cursor.getColumnIndexOrThrow("family_name")));
                    providerDTO.setGivenName(cursor.getString(cursor.getColumnIndexOrThrow("given_name")));
                    providerDTO.setUuid(cursor.getString(cursor.getColumnIndexOrThrow("uuid")));
                    providerDTO.setIdentifier(cursor.getString(cursor.getColumnIndexOrThrow("identifier")));
                    providerDTO.setRole(cursor.getString(cursor.getColumnIndexOrThrow("role")));
                    providerDTO.setUserUuid(cursor.getString(cursor.getColumnIndexOrThrow("useruuid")));

                    providersList.add(providerDTO);


                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }
        return providersList;
    }

    public String checkNurseWard(String providerUuid) throws DAOException {
        String wardType = "";
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            String query = "select value from tbl_provider_attribute where provideruuid = ?";
            Cursor cursor = db.rawQuery(query, new String[]{providerUuid});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    wardType = cursor.getString(cursor.getColumnIndexOrThrow("value"));
                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }
        return wardType;
    }

    public String getUserUuid(String providerUuid) throws DAOException {
        String userUuid = "";
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            String query = "select * from tbl_provider where uuid = ?";
            Cursor cursor = db.rawQuery(query, new String[]{providerUuid});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    userUuid = cursor.getString(cursor.getColumnIndexOrThrow("useruuid"));
                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }
        return userUuid;
    }

    public String getCreatorGivenName(String providerUuid) throws DAOException {
        String givenname = "";
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        db.beginTransaction();
        try {
            String query = "select substr(given_name,1,1) || substr(family_name,1,1) as given_name from tbl_provider where uuid = ?";
            Cursor cursor = db.rawQuery(query, new String[]{providerUuid});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    givenname = cursor.getString(cursor.getColumnIndexOrThrow("given_name"));
                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
            throw new DAOException(s);
        } finally {
            db.endTransaction();

        }
        return givenname;

    }

    public String getGivenNameByUserUuid(String userUuid) {
        String name = "";
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            String query = "select substr(given_name,1,1) || substr(family_name,1,1) as  given_name from tbl_provider where useruuid = ?";
            Cursor cursor = db.rawQuery(query, new String[]{userUuid});
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    name = cursor.getString(cursor.getColumnIndexOrThrow("given_name"));
                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
        } finally {
            db.endTransaction();
        }
        return name;
    }

}
