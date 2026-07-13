package org.intelehealth.ezazi.ui.rtc.call;

import org.intelehealth.ezazi.BuildConfig;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.VisitAttributeListDAO;
import org.intelehealth.ezazi.models.dto.VisitAttributeDTO;
import org.intelehealth.ezazi.networkApiCalls.ApiClient;
import org.intelehealth.ezazi.networkApiCalls.ApiInterface;
import org.intelehealth.ezazi.ui.dialog.model.SingChoiceItem;
import org.intelehealth.ezazi.ui.rtc.data.RtcTokenDataSource;
import org.intelehealth.klivekit.model.RtcArgs;
import org.intelehealth.klivekit.utils.RemoteActionType;

import java.util.LinkedHashMap;
import java.util.LinkedList;

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

    /**
     * Primary and secondary doctors are stored as visit attributes (value = uuid@#@name),
     * so they are read for the given visit rather than the patient.
     */
    public static LinkedList<SingChoiceItem> getDoctorsDetails(String visitUuid) {
        VisitAttributeListDAO visitAttributeListDAO = new VisitAttributeListDAO();
        LinkedHashMap<String, SingChoiceItem> doctors = new LinkedHashMap<>();
        addDoctor(doctors, visitAttributeListDAO.getVisitAttributeValue(visitUuid, VisitAttributeDTO.Columns.PRIMARY_DOCTOR.uuid), AppConstants.PRIMARY);
        addDoctor(doctors, visitAttributeListDAO.getVisitAttributeValue(visitUuid, VisitAttributeDTO.Columns.SECONDARY_DOCTOR.uuid), AppConstants.SECONDARY);
        return new LinkedList<>(doctors.values());
    }

    private static void addDoctor(LinkedHashMap<String, SingChoiceItem> doctors, String value, String type) {
        if (value == null || !value.contains("@#@")) return;
        String[] parts = value.split("@#@");
        if (parts.length < 2 || parts[0].isEmpty() || parts[0].equalsIgnoreCase(AppConstants.NOT_APPLICABLE)) return;
        doctors.put(parts[0], buildItem(parts[0], parts[1], type));
    }

    private static SingChoiceItem buildItem(String uuid, String name, String type) {
        SingChoiceItem item = new SingChoiceItem();
        item.setItemId(uuid);
        item.setItem(name);
        item.setSecondaryName(type);
        return item;
    }
}
