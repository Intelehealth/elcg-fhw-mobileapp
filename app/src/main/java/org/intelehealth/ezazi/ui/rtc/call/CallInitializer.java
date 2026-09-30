package org.intelehealth.ezazi.ui.rtc.call;

import android.widget.Toast;

import org.intelehealth.ezazi.BuildConfig;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.core.data.BaseDataSource;
import org.intelehealth.ezazi.models.dto.EncounterDTO;
import org.intelehealth.ezazi.models.dto.PatientAttributesDTO;
import org.intelehealth.ezazi.networkApiCalls.ApiClient;
import org.intelehealth.ezazi.networkApiCalls.ApiInterface;
import org.intelehealth.ezazi.ui.dialog.model.SingChoiceItem;
import org.intelehealth.ezazi.utilities.ObstetricValueReader;
import org.intelehealth.ezazi.ui.password.listener.OnAPISuccessListener;
import org.intelehealth.ezazi.ui.rtc.data.RtcTokenDataSource;
import org.intelehealth.ezazi.ui.rtc.model.UserToken;
import org.intelehealth.klivekit.model.RtcArgs;
import org.intelehealth.klivekit.utils.RemoteActionType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by Vaghela Mithun R. on 06-07-2023 - 14:19.
 * Email : mithun@intelehealth.org
 * Mob   : +919727206702
 **/
public class CallInitializer {
    public interface OnCallInitializedListener {
        void onInitialized(RtcArgs args);
    }

    private final RtcArgs args;

    public CallInitializer(RtcArgs args) {
        this.args = args;
    }

    public void initiateVideoCall(OnCallInitializedListener listener) {
        String BASE_URL = BuildConfig.SERVER_URL + ":3000";
        ApiClient.changeApiBaseUrl(BASE_URL);
        ApiInterface apiService = ApiClient.createService(ApiInterface.class);
        new RtcTokenDataSource(apiService).getRtcToken(result -> {
            args.setToken(result.getToken());
            args.setActionType(RemoteActionType.VIDEO_CALL.name());
            args.setAppToken(result.getAppToken());
            listener.onInitialized(args);
        }, args);
    }

    public static LinkedList<SingChoiceItem> getDoctorsDetails(String patientUuid, String visitUuid) {
        Map<PatientAttributesDTO.Columns, String> values = ObstetricValueReader.values(
                patientUuid, visitUuid,
                Arrays.asList(
                        PatientAttributesDTO.Columns.PRIMARY_DOCTOR,
                        PatientAttributesDTO.Columns.SECONDARY_DOCTOR
                ));

        LinkedHashMap<String, SingChoiceItem> tempMap = new LinkedHashMap<>();

        String[] primary = splitString(values.get(PatientAttributesDTO.Columns.PRIMARY_DOCTOR));
        if (isUsableDoctor(primary)) {
            tempMap.put(primary[0], buildItem(primary[0], primary[1], AppConstants.PRIMARY));
        }

        String[] secondary = splitString(values.get(PatientAttributesDTO.Columns.SECONDARY_DOCTOR));
        if (isUsableDoctor(secondary) && !secondary[0].equalsIgnoreCase(AppConstants.NOT_APPLICABLE)) {
            tempMap.put(secondary[0], buildItem(secondary[0], secondary[1], AppConstants.SECONDARY));
        }

        return new LinkedList<>(tempMap.values());
    }

    private static String[] splitString(String value) {
        return value == null ? new String[0] : value.split("@#@");
    }

    /** split() drops trailing empties, so a stored "uuid@#@" arrives as one element and [1] throws. */
    private static boolean isUsableDoctor(String[] parts) {
        return parts.length > 1 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty();
    }

    private static SingChoiceItem buildItem(String uuid, String name, String type) {
        SingChoiceItem item = new SingChoiceItem();
        item.setItemId(uuid);
        item.setItem(name);
        item.setSecondaryName(type);
        return item;
    }
}
