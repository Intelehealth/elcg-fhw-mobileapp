package org.intelehealth.ezazi.activities.visitSummaryActivity;


import static org.intelehealth.ezazi.app.AppConstants.EVENT_SHIFT_CHANGED;
import static org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding;
import static org.intelehealth.ezazi.utilities.UuidDictionary.DELIVERY_OUTCOME_STAGE3;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ajalt.timberkt.Timber;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.activities.epartogramActivity.EpartogramViewActivity;
import org.intelehealth.ezazi.activities.epartogramActivity.OfflineEPartogramViewActivity;
import org.intelehealth.ezazi.activities.homeActivity.HomeActivity;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.app.IntelehealthApplication;
import org.intelehealth.ezazi.database.dao.EncounterDAO;
import org.intelehealth.ezazi.database.dao.ObsDAO;
import org.intelehealth.ezazi.database.dao.PatientsDAO;
import org.intelehealth.ezazi.database.dao.ProviderDAO;
import org.intelehealth.ezazi.database.dao.VisitAttributeListDAO;
import org.intelehealth.ezazi.database.dao.VisitsDAO;
import org.intelehealth.ezazi.databinding.DialogOutOfTimeEzaziBinding;
import org.intelehealth.ezazi.enums.StageEnum;
import org.intelehealth.ezazi.models.FamilyMemberRes;
import org.intelehealth.ezazi.models.dto.EncounterDTO;
import org.intelehealth.ezazi.models.dto.ObsDTO;
import org.intelehealth.ezazi.models.dto.PatientAttributesDTO;
import org.intelehealth.ezazi.models.dto.ProviderDTO;
import org.intelehealth.ezazi.optimized_sync.network.NetworkStatus;
import org.intelehealth.ezazi.services.firebase_services.FirebaseRealTimeDBUtils;
import org.intelehealth.ezazi.stage3.activities.WomenDeliveryDetailsActivity;
import org.intelehealth.ezazi.stage3.postpartum.OfflineStage3ViewActivity;
import org.intelehealth.ezazi.stage3.postpartum.ViewPostPartumReportActivity;
import org.intelehealth.ezazi.syncModule.SyncUtils;
import org.intelehealth.ezazi.ui.dialog.AppDialogUtils;
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment;
import org.intelehealth.ezazi.ui.dialog.CustomViewDialogFragment;
import org.intelehealth.ezazi.ui.dialog.SingleChoiceDialogFragment;
import org.intelehealth.ezazi.ui.dialog.model.MultiChoiceItem;
import org.intelehealth.ezazi.ui.dialog.model.SingChoiceItem;
import org.intelehealth.ezazi.ui.prescription.activity.PrescriptionActivity;
import org.intelehealth.ezazi.ui.prescription.data.PrescriptionRepository;
import org.intelehealth.ezazi.ui.prescription.fragment.PrescriptionFragment;
import org.intelehealth.ezazi.ui.rtc.activity.EzaziChatActivity;
import org.intelehealth.ezazi.ui.rtc.activity.EzaziVideoCallActivity;
import org.intelehealth.ezazi.ui.rtc.call.CallInitializer;
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity;
import org.intelehealth.ezazi.ui.visit.data.VisitRepository;
import org.intelehealth.ezazi.ui.visit.dialog.CompleteVisitOnEnd2StageDialog;
import org.intelehealth.ezazi.ui.visit.dialog.CompleteVisitOnEnd3StageDialog;
import org.intelehealth.ezazi.ui.visit.dialog.CompleteVisitOnEndStage1Dialog;
import org.intelehealth.ezazi.ui.visit.model.CompletedVisitStatus;
import org.intelehealth.ezazi.ui.visit.model.VisitOutcome;
import org.intelehealth.ezazi.utilities.InternetDialogHelper;
import org.intelehealth.ezazi.utilities.Logger;
import org.intelehealth.ezazi.utilities.NetworkConnection;
import org.intelehealth.ezazi.utilities.NotificationReceiver;
import org.intelehealth.ezazi.utilities.NotificationUtils;
import org.intelehealth.ezazi.utilities.SessionManager;
import org.intelehealth.ezazi.utilities.UuidDictionary;
import org.intelehealth.ezazi.utilities.exception.DAOException;
import org.intelehealth.klivekit.chat.model.ItemHeader;
import org.intelehealth.klivekit.model.RtcArgs;
import org.intelehealth.klivekit.socket.SocketManager;
import org.intelehealth.klivekit.utils.DateTimeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimelineVisitSummaryActivity extends BaseActionBarActivity {

    private RecyclerView recyclerView;
    private TimelineAdapter adapter;
    private Context context;
    private String patientName;
    private Intent intent;
    private String patientUuid;
    private String visitUuid;
    private String whichScreenUserCameFromTag = "";
    private String providerID;
    private SessionManager sessionManager;
    private EncounterDAO encounterDAO = new EncounterDAO();
    private Button endStageButton;
    private int stageNo = 0;
    private String isVCEPresent = "";
    private Button fabc, fabv, fabSOS, fabPrescription, btnShiftToPostnatalWard;
    private TextView outcomeTV;
    public static final String TAG = "TimelineVisitSummary";

    private boolean isVisitCompleted = false;

    private boolean hwHasEditAccess = true;
    private boolean isNewEncounterCreated = false;
    private boolean isDecisionPending;
    private String fromScreen;
    private ConstraintLayout layoutPendingFlag;
    private List<ItemHeader> prescriptions;
    private ObsDAO obsDAO = new ObsDAO();
    private PatientsDAO patientsDAO = new PatientsDAO();
    private ProviderDAO providerDAO = new ProviderDAO();
    private VisitsDAO visitsDAO = new VisitsDAO();
    SyncUtils syncUtils = new SyncUtils();


    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            fetchAllEncountersFromVisitForTimelineScreen(visitUuid);
        }
    };

    private final BroadcastReceiver visitTimeOutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initUI();
//            recreate();
//            checkInternetAndUploadVisitEncounter(false);
        }
    };
    private final BroadcastReceiver checkEncounterEditMode = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(AppConstants.CURRENT_ENC_EDIT_INTENT_ACTION)) {
                isNewEncounterCreated = intent.getBooleanExtra("newEncounter", false);
                Log.d(TAG, "kkencounter edit onReceive: isNewEncounterCreated : " + isNewEncounterCreated);
                fetchAllEncountersFromVisitForTimelineScreen(visitUuid);
                //TimelineAdapter timelineAdapter = new TimelineAdapter();
                //timelineAdapter.setEditMode(isNewEncounterCreated);
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mMessageReceiver);
        unregisterReceiver(visitTimeOutReceiver);
        unregisterReceiver(syncBroadcastReceiver);
        unregisterReceiver(checkEncounterEditMode);
    }

    @Override
    protected void onResume() {
        super.onResume();
       /* registerReceiver(visitTimeOutReceiver, new IntentFilter(AppConstants.VISIT_DECISION_PENDING_ACTION));
        registerReceiver(mMessageReceiver, new IntentFilter(AppConstants.NEW_CARD_INTENT_ACTION));
        registerReceiver(syncBroadcastReceiver, new IntentFilter(AppConstants.SYNC_INTENT_ACTION));
        registerReceiver(checkEncounterEditMode, new IntentFilter(AppConstants.CURRENT_ENC_EDIT_INTENT_ACTION));*/

        ContextCompat.registerReceiver(TimelineVisitSummaryActivity.this, visitTimeOutReceiver, new IntentFilter(AppConstants.VISIT_DECISION_PENDING_ACTION), ContextCompat.RECEIVER_EXPORTED);
        ContextCompat.registerReceiver(TimelineVisitSummaryActivity.this, mMessageReceiver, new IntentFilter(AppConstants.NEW_CARD_INTENT_ACTION), ContextCompat.RECEIVER_EXPORTED);
        ContextCompat.registerReceiver(TimelineVisitSummaryActivity.this, syncBroadcastReceiver, new IntentFilter(AppConstants.SYNC_INTENT_ACTION), ContextCompat.RECEIVER_EXPORTED);
        ContextCompat.registerReceiver(TimelineVisitSummaryActivity.this, checkEncounterEditMode, new IntentFilter(AppConstants.CURRENT_ENC_EDIT_INTENT_ACTION), ContextCompat.RECEIVER_EXPORTED);

        manageVisibilityOfPendingFlag();
        if (visitUuid != null && !visitUuid.isEmpty()) {
            fetchAllEncountersFromVisitForTimelineScreen(visitUuid);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_timeline_ezazi);
        super.onCreate(savedInstanceState);
        setupActionBar();
        enableProperPadding(TimelineVisitSummaryActivity.this);
        encounterDAO = new EncounterDAO();
        initUI();
        calculateStageDurationsAndShowDialog();

        fabSOS.setOnClickListener(view -> {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(1000);
            }
            showEmergencyDialog();
        });
        fabc.setOnClickListener(view -> {
            boolean isInternetAvailable = InternetDialogHelper.checkInternetOrShow(TimelineVisitSummaryActivity.this);
            if (isInternetAvailable) {
                showDoctorSelectionDialog(true);
            }
        });
        fabv.setOnClickListener(view -> {
            boolean isInternetAvailable = InternetDialogHelper.checkInternetOrShow(TimelineVisitSummaryActivity.this);
            if (isInternetAvailable) {
                showDoctorSelectionDialog(false);
            }
        });
        /*fabPrescription.setOnClickListener(view -> PrescriptionActivity.startPrescriptionActivity(
                TimelineVisitSummaryActivity.this,
                visitUuid)
        );*/
        fabPrescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (prescriptions != null && !prescriptions.isEmpty()) {
                    PrescriptionActivity.startPrescriptionActivity(TimelineVisitSummaryActivity.this, visitUuid);
                } else {
                    // No data found
                    Toast.makeText(context, getString(R.string.no_prescription_available), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupShiftToPostnatalWardBtn() {
        try {
            Log.d("CCCCC", "called" + stageNo);
            String providerId = sessionManager.getProviderID();
            String ward = providerDAO.checkNurseWard(providerId);
            Boolean isVisitActive = visitsDAO.isVisitActive(visitUuid, providerId);
            if (((ward.isEmpty() || ward.equals("Labor Ward"))
                    && isVisitActive
                    && stageNo == 3)) {
                btnShiftToPostnatalWard.setVisibility(View.VISIBLE);
            } else {
                btnShiftToPostnatalWard.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            btnShiftToPostnatalWard.setVisibility(View.GONE);
            e.printStackTrace();
        }

        btnShiftToPostnatalWard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<MultiChoiceItem> items = new ArrayList<>();
                FamilyMemberRes patientNameInfo = null;
                try {
                    patientNameInfo = patientsDAO.getPatientNameInfo(patientUuid);
                    patientNameInfo.setVisitUuid(visitUuid);
                    items.add(patientNameInfo);
                    showNextShiftNursesDialog(items);
                } catch (DAOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void showNextShiftNursesDialog(List<MultiChoiceItem> items) {
        List<String> visitUuids = new ArrayList<>();
        List<FamilyMemberRes> patients = new ArrayList<>();
        Log.e(TAG, "Selected patients : " + items.size());
        for (MultiChoiceItem item : items) {
            if (item instanceof FamilyMemberRes) {
                patients.add((FamilyMemberRes) item);
                visitUuids.add(((FamilyMemberRes) item).getVisitUuid());
                Log.e(TAG, "Visit Uuid =>" + ((FamilyMemberRes) item).getVisitUuid());
            }
        }
        showNurseAssignDialog(visitUuids, patients);
    }

    private void showNurseAssignDialog(List<String> visitUUIDList, List<FamilyMemberRes> patientsList) {
        try {
            ProviderDAO providerDAO = new ProviderDAO();
            String myCreatorUUID = new SessionManager(IntelehealthApplication.getAppContext()).getCreatorID();
            List<ProviderDTO> mProviderNurseList = providerDAO.getNurseList(false);
//            String[] nurseNames = new String[mProviderNurseList.size() - 1];
//            String[] nurseUUID = new String[mProviderNurseList.size() - 1];

            ArrayList<SingChoiceItem> choiceItems = new ArrayList<>();
            for (int i = 0; i < mProviderNurseList.size(); i++) {
                if (!mProviderNurseList.get(i).getUserUuid().equals(myCreatorUUID)) {
                    SingChoiceItem item = new SingChoiceItem();
                    item.setItem(mProviderNurseList.get(i).getGivenName() + " " + mProviderNurseList.get(i).getFamilyName());
                    item.setItemId(mProviderNurseList.get(i).getUuid());
                    item.setUserUuid(mProviderNurseList.get(i).getUserUuid());
                    item.setItemIndex(i);
                    choiceItems.add(item);
                }
            }

            showNurseSelectionDialog(visitUUIDList, choiceItems, patientsList);
        } catch (DAOException e) {
            e.printStackTrace();
        }
    }

    private void showNurseSelectionDialog(List<String> visitUUIDList, ArrayList<SingChoiceItem> choiceItems, List<FamilyMemberRes> patientsList) {
        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(this)
                .title(R.string.select_nurse)
                .positiveButtonLabel(R.string.save_button)
                .content(choiceItems)
                .build();

        dialog.isSearchable(true);
        dialog.setListener(item -> assignNurseToPatient(visitUUIDList, item.getItemId(), item.getUserUuid(), patientsList));

        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());
    }

    private void assignNurseToPatient(List<String> visitUUIDList, String selectedNurseProviderUuid, String assigneeNurseUserUuid, List<FamilyMemberRes> patientsList) {
        PatientsDAO patientsDAO = new PatientsDAO();
        Log.e(TAG, "assignNurseToPatient: " + selectedNurseProviderUuid);
        Log.e(TAG, "assignNurseToPatient: assigneeNurseUserUuid check2 :: " + assigneeNurseUserUuid);

        VisitAttributeListDAO visitsAttrsDAO = new VisitAttributeListDAO();
        for (int j = 0; j < visitUUIDList.size(); j++) {
            try {
                visitsAttrsDAO.updateVisitTypeAttributeUuid(visitUUIDList.get(j), selectedNurseProviderUuid);
                new VisitsDAO().updateVisitSync(visitUUIDList.get(j), "0");
            } catch (DAOException e) {
                throw new RuntimeException(e);
            }
        }

        ShiftChangeData shiftChangeData = new ShiftChangeData();
        shiftChangeData.setToHwUserUuid(assigneeNurseUserUuid);
        shiftChangeData.setProviderID(sessionManager.getProviderID());
        shiftChangeData.setAssignorNurse(sessionManager.getChwname());
        shiftChangeData.generatePatientsInfo(patientsList);
        shiftChangeData.setTag("shiftChange");
        Log.d("1122Shift", new Gson().toJson(shiftChangeData));


        SocketManager.getInstance().emit(EVENT_SHIFT_CHANGED, new Gson().toJson(shiftChangeData));
        //mPendingForLogout = true;
        sync();
        Toast.makeText(context, getString(R.string.patient_assigned_successfully), Toast.LENGTH_SHORT).show();
        finish();

    }

    private void sync() {
        if (isNetworkConnected()) {
            Toast.makeText(context, getString(R.string.syncInProgress), Toast.LENGTH_LONG).show();
            syncUtils.syncForeground("home");
        } else {
            showErrorOnNoInternetOnRefresh();
            //Toast.makeText(context, context.getString(R.string.failed_synced), Toast.LENGTH_LONG).show();
        }
    }

    private void showErrorOnNoInternetOnRefresh() {
        AppDialogUtils.showSingleButtonDialog(
                this,
                getString(R.string.no_internet_setup_screen_title),
                getString(R.string.no_internet_setup_screen_body),
                getString(R.string.ok),
                () -> {
                    //finish();
                    return null;
                }
        );
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }


    @Override
    protected int getScreenTitle() {
        return 0;
    }

    /**
     * Show the single choice doctor selection dialog and move forward
     * to video call with selected doctor from list
     */
    private void showDoctorSelectionDialog(boolean isChat) {
        LinkedList<SingChoiceItem> choiceItems = CallInitializer.getDoctorsDetails(patientUuid);
        Timber.tag(TAG).d(new Gson().toJson(choiceItems));
        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(this).title(R.string.select_doctor).content(choiceItems).build();

        dialog.setListener(item -> {
            if (isChat) {
                startChatActivity(item);
            } else {
                startVideoCallActivity(item);
            }
        });

        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());
    }

    private void startChatActivity(SingChoiceItem item) {
        if (SocketManager.getInstance().checkUserIsOnline(item.getItemId())) {
            RtcArgs args = new RtcArgs();
            args.setPatientName(patientName);
            args.setPatientId(patientUuid);
            args.setVisitId(visitUuid);
            args.setNurseId(sessionManager.getProviderID());
            args.setDoctorUuid(item.getItemId());
            EzaziChatActivity.startChatActivity(this, args);
        } else Toast.makeText(this, item.getItem() + " is offline ", Toast.LENGTH_SHORT).show();
    }

    /**
     * Start video call with selected doctor from primary and secondary list.
     *
     * @param item SingChoiceItem
     */
    private void startVideoCallActivity(SingChoiceItem item) {
        if (SocketManager.getInstance().checkUserIsOnline(item.getItemId())) {
            Toast.makeText(this, item.getItem(), Toast.LENGTH_LONG).show();
            EncounterDTO encounterDTO = encounterDAO.getEncounterByVisitUUIDLimit1(visitUuid);
            RtcArgs args = new RtcArgs();
            try {
                String patientOpenMrsId = new PatientsDAO().getOpenmrsId(patientUuid);
                args.setPatientOpenMrsId(patientOpenMrsId);
            } catch (DAOException e) {
                throw new RuntimeException(e);
            }

            String nurseId = encounterDTO.getProvideruuid();
            String roomId = patientUuid;

            args.setVisitId(visitUuid);
            args.setPatientId(patientUuid);
            args.setPatientPersonUuid(patientUuid);
            args.setPatientName(patientName);
            args.setDoctorName(item.getItem());
            args.setDoctorUuid(item.getItemId());
            args.setIncomingCall(false);
            args.setNurseId(nurseId);
            args.setNurseName(sessionManager.getChwname());
            args.setRoomId(roomId);
            new CallInitializer(args).initiateVideoCall(args1 -> EzaziVideoCallActivity.startVideoCallActivity(TimelineVisitSummaryActivity.this, args1));

        } else {
            Toast.makeText(this, item.getItem() + " is offline", Toast.LENGTH_LONG).show();
        }
    }

    private void showEmergencyDialog() {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(this).title(R.string.title_sos_data_entry).positiveButtonLabel(R.string.yes).content(getString(R.string.are_you_sure_to_capture_emergency_data)).build();

        dialog.setListener(this::collectEmergencyData);

        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());

    }

   /* private void collectEmergencyData() {
        EncounterDTO encounterDTO = encounterDAO.getEncounterByVisitUUIDLimit1(visitUuid);
        String latestEncounterName = encounterDAO.getEncounterTypeNameByUUID(encounterDTO.getEncounterTypeUuid());
        if (latestEncounterName != null && latestEncounterName.length() > 0) {
            if (!latestEncounterName.toLowerCase().contains("stage") && !latestEncounterName.toLowerCase().contains("hour"))
                return;
            String[] parts = latestEncounterName.toLowerCase().replaceAll("stage", "").replaceAll("hour", "").split("_");
            if (parts.length != 3) return;
            int stageNumber = Integer.parseInt(parts[0]);
            int hourNumber = Integer.parseInt(parts[1]) + 1;
            int cardNumber = 1;//Integer.parseInt(parts[2]);


            String nextEncounterTypeName = "Stage" + stageNumber + "_" + "Hour" + hourNumber + "_" + cardNumber;
            String encounterUuid = UUID.randomUUID().toString();
            new ObsDAO().createEncounterType(encounterUuid, EncounterDTO.Type.SOS.name(), sessionManager.getCreatorID(), TAG);
            isNewEncounterCreated = createNewEncounter(encounterUuid, visitUuid, nextEncounterTypeName);
            fetchAllEncountersFromVisitForTimelineScreen(visitUuid);
        }
    }*/

    //for india - regular 2 stages
    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.timeline_menu, menu);
        Button btn = menu.findItem(R.id.action_view_partogram).getActionView().findViewById(R.id.btnViewPartogram);
        btn.setOnClickListener(view -> {
            onOptionsItemSelected(menu.findItem(R.id.action_view_partogram));
        });
        return true;
    }*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.timeline_menu, menu);
        //Button btn = menu.findItem(R.id.action_view_partogram).getActionView().findViewById(R.id.btnViewPartogram);
        LinearLayout btn = menu.findItem(R.id.action_view_partogram).getActionView().findViewById(R.id.layoutActionMenuViewReports);
        btn.setOnClickListener(view -> showViewDropdownMenu(view));
        return true;
    }

    /*@SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_partogram:
//                boolean isTablet = getResources().getBoolean(R.bool.isTablet);
//                if (isTablet) showEpartogram();
//                else showRequireTabletView();
                //show dialog for no data for elcg
                boolean doesVisitHasData = obsDAO.getObsCountForVisit(visitUuid);
                if (doesVisitHasData) {
                    showEpartogram();
                } else {
                    showNoDataDialogForViewLcg();
                }
                break;
            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }*/

    private void showRequireTabletView() {
        new ConfirmationDialogFragment.Builder(this).content(getString(R.string.this_option_available_tablet_device)).positiveButtonLabel(R.string.ok).hideNegativeButton(true).build().show(getSupportFragmentManager(), "ConfirmationDialogFragment");
    }

    private void showEpartogram() {
        Map<String, String> log = new HashMap<>();
        log.put("TAG", TAG);
        log.put("action", "showEpartogram");
        TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        Logger.logV("PHONE_TYPE_NONE", String.valueOf(Objects.requireNonNull(manager).getPhoneType()));
        FirebaseRealTimeDBUtils.logData(log);

        Intent intent;
        if (currentNetworkStatus().getHasInternet()) {
            intent = new Intent(context, EpartogramViewActivity.class);
        } else {
            intent = new Intent(context, OfflineEPartogramViewActivity.class);
        }
        intent.putExtra("patientName", patientName);
        intent.putExtra("patientuuid", patientUuid);
        intent.putExtra("visituuid", visitUuid);
        startActivity(intent);
    }

    private void initUI() {
        fabSOS = findViewById(R.id.btnSOS);
        fabv = findViewById(R.id.btnVideoOnOff);
        outcomeTV = findViewById(R.id.outcomeTV);
        fabc = findViewById(R.id.btnFlipCamera);
        fabPrescription = findViewById(R.id.btnPrescription);
        btnShiftToPostnatalWard = findViewById(R.id.btnShiftToPostnatalWard);
        recyclerView = findViewById(R.id.recyclerview_timeline);
        endStageButton = findViewById(R.id.btnEndStage);
        LinearLayoutManager linearLayout = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        recyclerView.setLayoutManager(linearLayout);
        context = TimelineVisitSummaryActivity.this;
        intent = this.getIntent(); // The intent was passed to the activity

        sessionManager = new SessionManager(this);
        String language = sessionManager.getAppLanguage();
        //In case of crash still the org should hold the current lang fix.
        if (!language.equalsIgnoreCase("")) {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }
        sessionManager.setCurrentLang(getResources().getConfiguration().locale.toString());

        if (intent != null) {
            patientName = intent.getStringExtra("patientNameTimeline");
            patientUuid = intent.getStringExtra("patientUuid");
            visitUuid = intent.getStringExtra("visitUuid");
            providerID = intent.getStringExtra("providerID");
            whichScreenUserCameFromTag = intent.getStringExtra("tag");

            //hwHasEditAccess = new VisitsDAO().checkLoggedInUserAccessVisit(visitUuid, sessionManager.getProviderID());
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler mainHandler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                hwHasEditAccess = new VisitsDAO().checkLoggedInUserAccessVisit(visitUuid, sessionManager.getProviderID());
                /*mainHandler.post(() -> {
                    // Update UI here
                    updateUI(hasAccess);
                });*/
            });
            if (whichScreenUserCameFromTag != null && whichScreenUserCameFromTag.equalsIgnoreCase("new")) {
                triggerAlarm_Stage1_every30mins(); // Notification to show every 30min.
            }

            showToastIfNoPrescription();
            // fetchAllEncountersFromVisitForTimelineScreen(visitUuid); // fetch all records... taken on -onresume
        }

        setTitle(patientName);
        encounterDAO = new EncounterDAO();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        //moved this code to separate method
        manageEndStageButtonText();


        // clicking on this open dialog to confirm and start stage 2 | If stage 2 already open then ends visit.
        endStageButton.setOnClickListener(v -> {
            if (stageNo == 1) {
                // showEndShiftDialog(); //old flow
                FragmentManager fragmentManager = getSupportFragmentManager();

                new CompleteVisitOnEndStage1Dialog(this, visitUuid, (isEndStage1) -> {
                    if (isEndStage1) {
                        //for end stage 1 option
                        cancelStage1ConfirmationDialog();
                    } else {
                        //for all refer options and mother deceased
                        showToastAndUploadVisitForStage1(true, getResources().getString(R.string.data_added_successfully));
                    }
                }).buildDialogSingleSelection(fragmentManager); //for single selection
                //buildDialog();  //for custom dialog
            } else if (stageNo == 2) {
                // show dialog and add birth outcome also show extra options like: Refer to other hospital & Self Discharge
                new CompleteVisitOnEnd2StageDialog(this, visitUuid, (hasLabour, hasMotherDeceased) -> {
                    if (!hasLabour) {
                        showToastAndUploadVisit(true, getResources().getString(R.string.data_added_successfully));
                    } else {
                        showLabourBottomSheetDialog(hasMotherDeceased);
                    }
                }).buildDialog();
            } else if (stageNo == 3) {
                new CompleteVisitOnEnd3StageDialog(this, visitUuid, () -> {
                    showToastAndUploadVisit(true, getResources().getString(R.string.data_saved_visit_closed));
                }).buildDialog();
            }

        });

        mCountDownTimer.cancel();
        mCountDownTimer.start();
        //manageVisibilityOfPendingFlag();


    }

    private void showToastIfNoPrescription() {
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getReadableDatabase();
        PrescriptionRepository repository = new PrescriptionRepository(db, obsDAO);
        prescriptions = repository.fetchPrescription(visitUuid, PrescriptionFragment.PrescriptionType.FULL);
        if (prescriptions != null && !prescriptions.isEmpty()) {
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_vector_prescription);
            fabPrescription.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null);

        } else {
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_vector_empty_prescription);
            fabPrescription.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null);
        }
    }

    private void manageVisibilityOfPendingFlag() {
        VisitRepository repository = new VisitRepository(AppConstants.inteleHealthDatabaseHelper.getReadableDatabase());
        boolean status = repository.getOutcomePendingStatus(visitUuid);
        TextView tvPendingDecision = findViewById(R.id.tv_message_pending_outcome);
        layoutPendingFlag = findViewById(R.id.layout_decision_pending);
        MaterialButton ivCloseMessage = findViewById(R.id.iv_close_decision_pending);
        layoutPendingFlag.setVisibility(status ? View.VISIBLE : View.GONE);
        ivCloseMessage.setOnClickListener(view -> layoutPendingFlag.setVisibility(View.GONE));
        if (stageNo == 1) {
            tvPendingDecision.setText(getResources().getString(R.string.outcome_decision_stage_1));
        } else if (stageNo == 2) {
            tvPendingDecision.setText(getResources().getString(R.string.outcome_decision_stage_2));
        } else if (stageNo == 3) {
            tvPendingDecision.setText(getResources().getString(R.string.outcome_decision_stage_3));
        }
        isDecisionPending = status;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                if (!fromScreen.isEmpty() && fromScreen.equals("searchPatient")) {
//                    isDecisionPending = new VisitAttributeListDAO().checkIsVisitOutcomePending(visitUuid);
//                }
//            }
//        });
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG, "manageVisibilityOfPendingFlag: isDecisionPending:" + isDecisionPending);
//
//            }
//        });

    }

    private void showLabourBottomSheetDialog(boolean hasMotherDeceased) {
        //VisitLabourActivity.startLabourCompleteActivity(this, visitUuid, hasMotherDeceased); // non nepal flow
        Intent activity = new Intent(this, WomenDeliveryDetailsActivity.class);
        activity.putExtra("patientNameTimeline", patientName);
        activity.putExtra("patientUuid", patientUuid);
        activity.putExtra("visitUuid", visitUuid);
        activity.putExtra("providerID", providerID);
        activity.putExtra("tag", "timline");
        startActivity(activity);
    }

    private void checkForOutOfTime(VisitOutcome outcome) {
        MaterialButton button = findViewById(R.id.btnAddOutOfTimeReason);
        boolean isOutOfTime = new ObsDAO().checkIsOutOfTimeEncounter(isVCEPresent);
        if (isOutOfTime) {
            button.setTag(1);
            button.setVisibility(View.VISIBLE);
            button.setTag(R.id.btnAddOutOfTimeReason, outcome);
            if (!outcome.getOtherComment().equals(CompletedVisitStatus.OutOfTime.OUT_OF_TIME.value())) {
                button.setTag(2);
                button.setText(getString(R.string.view_more));
                updateOutOfTimeOutcomeText(outcome);
                button.setOnClickListener(outOfTimeClickListener);
            } else if (!hwHasEditAccess) {
                button.setVisibility(View.GONE);
            } else {
                button.setOnClickListener(outOfTimeClickListener);
                button.setVisibility(View.VISIBLE);
            }
        } else {
            manageOtherOutcome(outcome);
        }
    }

    private void manageOtherOutcome(VisitOutcome visitOutcome) {
        MaterialButton button = findViewById(R.id.btnAddOutOfTimeReason);
        if (visitOutcome.getMotherDeceasedReason() != null || visitOutcome.getOtherComment() != null) {
            button.setText(getString(R.string.view_more));
            button.setTag(R.id.btnAddOutOfTimeReason, visitOutcome);
            updateOutOfTimeOutcomeText(visitOutcome);
            button.setOnClickListener(viewMoreClickListener);
            button.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.GONE);
        }
    }

    private final View.OnClickListener viewMoreClickListener = v -> {
        VisitOutcome outcome = (VisitOutcome) v.getTag(R.id.btnAddOutOfTimeReason);
        showContentDialog(outcome);
    };

    private void showContentDialog(VisitOutcome outcome) {
        String content = outcome.getOtherComment();
        if (outcome.isHasMotherDeceased()) {
            content = content != null ? "Other Comment:\n" + content + "\n\n" + "Mother Deceased Reason:\n" + outcome.getMotherDeceasedReason() : "Mother Deceased Reason:\n" + outcome.getMotherDeceasedReason();
        }

        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(this).title(outcome.getOutcome()).content(content).positiveButtonLabel(R.string.okay).hideNegativeButton(true).build();

        dialog.show(getSupportFragmentManager(), dialog.getTag());
    }

    private void updateOutOfTimeOutcomeText(VisitOutcome visitOutcome) {
        outcomeTV.setGravity(Gravity.START);
        String label = visitOutcome.getOutcome();
        String content = visitOutcome.getOtherComment() != null ? visitOutcome.getOtherComment() + (visitOutcome.getMotherDeceasedReason() != null ? "/" + visitOutcome.getMotherDeceasedReason() : "") : visitOutcome.getMotherDeceasedReason();
        String mainReason = getString(R.string.outcome_reason, content);
        setOutcomeText(label + mainReason);
    }

    private void setOutcomeText(String outcome) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outcomeTV.setText(Html.fromHtml(getString(R.string.lbl_outcome, outcome), 0));
        } else {
            outcomeTV.setText(Html.fromHtml(getString(R.string.lbl_outcome, outcome)));
        }
    }

    private final View.OnClickListener outOfTimeClickListener = v -> {
        int isUpdateRequest = (int) v.getTag();
        VisitOutcome outcome = (VisitOutcome) v.getTag(R.id.btnAddOutOfTimeReason);
        showOutOfTimeReasonInputDialog(isUpdateRequest, outcome);
    };

    private void showOutOfTimeReasonInputDialog(int isUpdateRequest, VisitOutcome outcome) {
        DialogOutOfTimeEzaziBinding binding = DialogOutOfTimeEzaziBinding.inflate(getLayoutInflater(), null, true);
        binding.etOutOfTimeReason.setText(outcome.getOtherComment().equalsIgnoreCase(outcome.getOutcome()) ? "" : outcome.getOtherComment());
        binding.etOutOfTimeReasonLayout.setMultilineInputEndIconGravity();
        binding.etOutOfTimeReason.setEnabled(hwHasEditAccess);
        int positiveLbl = hwHasEditAccess ? isUpdateRequest == 2 ? R.string.update_out_of_time_reason : R.string.add_out_of_time_reason : R.string.okay;

        CustomViewDialogFragment dialog = new CustomViewDialogFragment.Builder(this).title(R.string.time_out_reason).positiveButtonLabel(positiveLbl).negativeButtonLabel(R.string.cancel).hideNegativeButton(!hwHasEditAccess).view(binding.getRoot()).build();

        if (hwHasEditAccess) {
            dialog.setListener(() -> {
                String reason = binding.etOutOfTimeReason.getText().toString();
                if (reason.length() > 0) {
                    outcome.setOtherComment(reason);
                    int updated = new ObsDAO().updateOutOfTimeEncounterReason(reason, isVCEPresent, visitUuid);
                    if (updated > 0) {
                        Toast.makeText(context, context.getString(R.string.time_out_info_submitted_successfully), Toast.LENGTH_SHORT).show();
//                            outcomeTV.setText(getString(R.string.lbl_outcome, reason));
                        updateOutOfTimeOutcomeText(outcome);
                        updateButtonText(R.string.view_more, 2, outcome);
                        SyncUtils syncUtils = new SyncUtils();
                        syncUtils.syncBackground();
                    } else {
                        Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Time out reason should not be empty", Toast.LENGTH_SHORT).show();
                }
            });
        }
        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());
    }

    private void updateButtonText(int label, int tag, VisitOutcome outcome) {
        MaterialButton button = findViewById(R.id.btnAddOutOfTimeReason);
        button.setTag(tag);
        button.setTag(R.id.btnAddOutOfTimeReason, outcome);
        button.setText(getString(label));
    }

//    private void showEndShiftDialog() {
//        final String[] stage1Options = {getString(R.string.move_to_stage2), getString(R.string.refer_to_other_hospital), getString(R.string.self_discharge_medical_advice)};
//        ArrayList<SingChoiceItem> choiceItems = new ArrayList<>();
//        int count = 0;
//        for (String str : stage1Options) {
//            SingChoiceItem item = new SingChoiceItem();
//            item.setItem(str);
//            item.setItemIndex(count);
//            choiceItems.add(item);
//            count++;
//        }
//
//        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(this).title(R.string.select_an_option).positiveButtonLabel(R.string.yes).content(choiceItems).build();
//
//        dialog.setListener(item -> manageStageSelection(item.getItemIndex(), item.getItem()));
//
//        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());
//
//    }

//    private void manageStageSelection(int position, String value) {
//        if (position == 0) cancelStage1ConfirmationDialog(); // cancel and start stage 2
//        else if (position == 1 || position == 2) // refer other hospital // call visit complete enc.
//            closeVisitFromStage1WithReferType(value);
////            showReferToOtherHospitalConfirmationDialog(value);
////        else if (position == 2) { // self discharge // call visit complete enc.
//////            showSelfDischargeConfirmationDialog(value);
//////            closeVisitFromStage1WithReferType(value);
////        }
//        else
//            Toast.makeText(context, context.getString(R.string.please_select_an_option), Toast.LENGTH_SHORT).show();
//    }

//    private void closeVisitFromStage1WithReferType(String value) {
//        ReferTypeHelper helper = new ReferTypeHelper(this, visitUuid);
//        helper.completeVisitWithReferType(value, CompletedVisitStatus.ReferType.conceptUuid(), () -> {
//            Toast.makeText(context, context.getString(R.string.refer_successfully, value), Toast.LENGTH_SHORT).show();
//            checkInternetAndUploadVisitEncounter(true);
//        });
//    }

//    private void showReferToOtherHospitalConfirmationDialog(String value) {
//        showConfirmationDialog(R.string.are_you_sure_want_to_refer_other, () -> {
//            referOtherHospitalDialog(value);
//        });
//    }

//    private void showSelfDischargeConfirmationDialog(String value) {
//        showConfirmationDialog(R.string.are_you_sure_want_to_self_discharge, () -> {
//            selfDischarge(value);
//        });
//    }

//    private void selfDischarge(String value) {
//        boolean isInserted = false;
//        try {
//            isInserted = insertVisitCompleteObs(visitUuid, value, UuidDictionary.REFER_TYPE);
//        } catch (DAOException e) {
//            e.printStackTrace();
//        }
//        if (isInserted) {
//            Toast.makeText(context, context.getString(R.string.self_discharge_successful), Toast.LENGTH_SHORT).show();

    /// /            Intent intent = new Intent(context, HomeActivity.class);
    /// /            startActivity(intent);
//            checkInternetAndUploadVisitEncounter(true);
//        } else {
//            Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
//        }
//    }
    private void showConfirmationDialog(@StringRes int content, ConfirmationDialogFragment.OnConfirmationActionListener listener) {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(this).content(getString(content)).positiveButtonLabel(R.string.yes).build();

        dialog.setListener(listener);

        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());
    }

//    private void showCustomViewDialog(@StringRes int title,
//                                      @StringRes int positiveLbl,
//                                      @StringRes int negLbl,
//                                      View view,
//                                      CustomViewDialogFragment.OnConfirmationActionListener listener) {
//        CustomViewDialogFragment dialog = new CustomViewDialogFragment.Builder(this)
//                .title(title)
//                .positiveButtonLabel(positiveLbl)
//                .negativeButtonLabel(negLbl)
//                .view(view)
//                .build();
//
//        dialog.setListener(listener);
//
//        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());
//    }

//    private void referOtherHospitalDialog(String referType) {
//        DialogReferHospitalEzaziBinding binding = DialogReferHospitalEzaziBinding.inflate(getLayoutInflater(), null, false);
//
//        showCustomViewDialog(R.string.refer_section, R.string.yes, R.string.no, binding.getRoot(), () -> {
//            boolean isInserted = false;
//            String hospitalName = binding.referHospitalName.getText().toString(), doctorName = binding.referDoctorName.getText().toString(), note = binding.referNote.getText().toString();
//
//            // call visitcompleteenc and add obs for refer type and referal values entered...
//            try {
//                isInserted = insertVisitCompleteEncounterAndObsReferHospital(visitUuid, referType, hospitalName, doctorName, note);
//            } catch (DAOException e) {
//                e.printStackTrace();
//            }
//
//            if (isInserted) {
//                Toast.makeText(context, context.getString(R.string.refer_data_submitted_successfully), Toast.LENGTH_SHORT).show();
    /// /                Intent intent = new Intent(context, HomeActivity.class);
    /// /                startActivity(intent);
//                checkInternetAndUploadVisitEncounter(true);
//            } else {
//                Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
//            }
//        });
//
//    }


    private final CountDownTimer mCountDownTimer = new CountDownTimer(24 * 60 * 60 * 1000, 60 * 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            fetchAllEncountersFromVisitForTimelineScreen(visitUuid);
        }

        @Override
        public void onFinish() {

        }
    };

    private static boolean createNewEncounter(String encounterUuid, String visit_UUID, String nextEncounterTypeName) {
        boolean isCreated = false;
        EncounterDAO encounterDAO = new EncounterDAO();
        EncounterDTO encounterDTO = new EncounterDTO();
        String typeUuid = encounterDAO.getEncounterTypeUuid(nextEncounterTypeName);
        if (typeUuid != null && typeUuid.length() > 0) {
            encounterDTO.setUuid(encounterUuid);
            encounterDTO.setVisituuid(visit_UUID);
            encounterDTO.setEncounterTime(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
            encounterDTO.setProvideruuid(new SessionManager(IntelehealthApplication.getAppContext()).getProviderID());
            encounterDTO.setEncounterTypeUuid(typeUuid);
            encounterDTO.setSyncd(false); // false as this is the one that is started and would be pushed in the payload...
            encounterDTO.setVoided(0);
            encounterDTO.setPrivacynotice_value("true");

            try {
                isCreated = encounterDAO.createEncountersToDB(encounterDTO);
            } catch (DAOException e) {
                e.printStackTrace();
            }
        }
        return isCreated;
    }


//    private boolean insertVisitCompleteEncounterAndObsReferHospital(String visitUuid, String referType, String hospitalName, String doctorName, String note) throws DAOException {
//        boolean isInserted = true;
//        String encounterUuid = "";
//        encounterUuid = encounterDAO.insertVisitCompleteEncounterToDb(visitUuid, sessionManager.getProviderID());
//
//        VisitsDAO visitsDAO = new VisitsDAO();
//        visitsDAO.updateVisitEnddate(visitUuid, AppConstants.dateAndTimeUtils.currentDateTime());
//        new VisitAttributeListDAO().markVisitAsRead(visitUuid);
//
//        ////
//        // Now get this encounteruuid and create refer obs table.
//        if (!encounterUuid.isEmpty()) {
//            ObsDAO obsDAO = new ObsDAO();
//            ObsDTO obsDTO;
//            List<ObsDTO> obsDTOList = new ArrayList<>();
//
//            // 1. Refer Type
//            obsDTO = new ObsDTO();
//            obsDTO.setUuid(UUID.randomUUID().toString());
//            obsDTO.setEncounteruuid(encounterUuid);
//            obsDTO.setValue(referType);
//            obsDTO.setConceptuuid(UuidDictionary.REFER_TYPE);
//            obsDTOList.add(obsDTO);
//
//            // 2. Refer Hospital Name
//            if (hospitalName != null && hospitalName.length() > 0) {
//                obsDTO = new ObsDTO();
//                obsDTO.setUuid(UUID.randomUUID().toString());
//                obsDTO.setEncounteruuid(encounterUuid);
//                obsDTO.setValue(hospitalName);
//                obsDTO.setConceptuuid(UuidDictionary.REFER_HOSPITAL);
//                obsDTOList.add(obsDTO);
//            }
//
//            // 3. Refer Doctor Name
//            if (doctorName != null && doctorName.length() > 0) {
//                obsDTO = new ObsDTO();
//                obsDTO.setUuid(UUID.randomUUID().toString());
//                obsDTO.setEncounteruuid(encounterUuid);
//                obsDTO.setValue(doctorName);
//                obsDTO.setConceptuuid(UuidDictionary.REFER_DR_NAME);
//                obsDTOList.add(obsDTO);
//            }
//
//            // 4. Refer Note
//            if (note != null && note.length() > 0) {
//                obsDTO = new ObsDTO();
//                obsDTO.setUuid(UUID.randomUUID().toString());
//                obsDTO.setEncounteruuid(encounterUuid);
//                obsDTO.setValue(note);
//                obsDTO.setConceptuuid(UuidDictionary.REFER_NOTE);
//                obsDTOList.add(obsDTO);
//            }
//
//            isInserted = obsDAO.insertObsToDb(obsDTOList, TAG);
//        }
//
//        return isInserted;
//    }


//    private boolean insertVisitCompleteObs(String visitUuid, String value, String conceptId) throws DAOException {
//        //  EncounterDAO encounterDAO = new EncounterDAO();
//        ObsDAO obsDAO = new ObsDAO();
//        boolean isInserted = false;
//        String encounterUuid = "";
//        encounterUuid = encounterDAO.insertVisitCompleteEncounterToDb(visitUuid, sessionManager.getProviderID());
//
//        VisitsDAO visitsDAO = new VisitsDAO();
//        try {
//            visitsDAO.updateVisitEnddate(visitUuid, AppConstants.dateAndTimeUtils.currentDateTime());
//            new VisitAttributeListDAO().markVisitAsRead(visitUuid);
//        } catch (DAOException e) {
//            FirebaseCrashlytics.getInstance().recordException(e);
//        }
//        ////
//
//        // Now get this encounteruuid and create BIRTH_OUTCOME in obs table.
//        isInserted = obsDAO.insert_Obs(encounterUuid, sessionManager.getCreatorID(), value, conceptId);
//
//        return isInserted;
//    }

    public void checkInternetAndUploadVisitEncounter(boolean isCompleteVisitCall) {
        isVisitCompleted = isCompleteVisitCall;
        if (NetworkConnection.isOnline(getApplication())) {
            Toast.makeText(context, getResources().getString(R.string.syncInProgress), Toast.LENGTH_LONG).show();
            SyncUtils syncUtils = new SyncUtils();
            syncUtils.syncForeground(TAG);
        } else {
            Toast.makeText(context, context.getString(R.string.failed_synced), Toast.LENGTH_LONG).show();
        }
    }

    // Timeline stage 1 end confirmation dialog
    private void cancelStage1ConfirmationDialog() {
        showConfirmationDialog(R.string.are_you_sure_want_to_end_stage_1, () -> {
            String encounterUuid = UUID.randomUUID().toString();
            new ObsDAO().createEncounterType(encounterUuid, EncounterDTO.Type.NORMAL.name(), sessionManager.getCreatorID(), TAG);
            createNewEncounter(encounterUuid, visitUuid, "Stage2_Hour1_1");
            fetchAllEncountersFromVisitForTimelineScreen(visitUuid);
            stageNo = 2;
            endStageButton.setText(context.getResources().getText(R.string.end2StageButton));
            updateVisitDecisionPendingFlag(); //added for decision pending visits
        });

    }

    private void cancelStage2_Alarm() { // visituuid : 0 - 5
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, Integer.parseInt(visitUuid.replaceAll("[^\\d]", "").substring(0, 5)), intent, NotificationUtils.getPendingIntentFlag());
        // to set different alarms for different patients.
        // vistiuuid: 0 - 4 index for stage 2
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

//    private void cancelStage1_Alarm() { // visituuid : 2 - 7
//        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//        Intent intent = new Intent(context, NotificationReceiver.class);
//        Log.v("timeline", "visituuid_int " + visitUuid.replaceAll("[^\\d]", ""));
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, Integer.parseInt(visitUuid.replaceAll("[^\\d]", "").substring(2, 7)), intent, NotificationUtils.getPendingIntentFlag());
//        // to set different alarms for different patients.
//        alarmManager.cancel(pendingIntent);
//        pendingIntent.cancel();
//    }

    // fetch all encounters from encounter tbl local db for this particular visit and show on timeline...
    private void fetchAllEncountersFromVisitForTimelineScreen(String visitUuid) {
        int stageOneDuration = 0;
        int stageTwoDuration = 0;
        //  encounterDAO = new EncounterDAO();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            ArrayList<EncounterDTO> encounterListDTO = encounterDAO.getEncountersByVisitUUID(visitUuid);
            for (int i = 0; i < encounterListDTO.size(); i++) {
                String name = encounterDAO.getEncounterTypeNameByUUID(encounterListDTO.get(i).getEncounterTypeUuid());
                EncounterDTO.Type type = obsDAO.getEncounterType(encounterListDTO.get(i).getUuid(), sessionManager.getCreatorID());
                String obsValue = obsDAO.getEncounterTypeFromDb(encounterListDTO.get(i).getUuid());
                encounterListDTO.get(i).setEncounterTypeName(name);
                encounterListDTO.get(i).setEncounterType(type);
                encounterListDTO.get(i).setObsValue(obsValue);
            }
            isVCEPresent = encounterDAO.getVisitCompleteEncounterByVisitUUID(visitUuid);

            encounterListDTO.removeIf(dto -> dto.getEncounterTypeUuid().equalsIgnoreCase(DELIVERY_OUTCOME_STAGE3)); // remove this from timeline ui
            Collections.reverse(encounterListDTO);
            Log.d("TIMELINE", "Adapter set with size: " + encounterListDTO.size());
            mainHandler.post(() -> {
                adapter = new TimelineAdapter(context, intent, encounterListDTO, sessionManager, isVCEPresent, isNewEncounterCreated, isDecisionPending);


                     /*   adapter = new TimelineAdapter(context, intent, encounterListDTO,
                                sessionManager, vce, isNewEncounterCreated, isDecisionPending);*/
                recyclerView.setAdapter(adapter);
                manageEndStageButtonText();
            });
        });
    }


    private void calculateStageDurationsAndShowDialog() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            ArrayList<EncounterDTO> stageOneList = new ArrayList<>();
            ArrayList<EncounterDTO> stageTwoList = new ArrayList<>();

            int parityCount = 0;

            ArrayList<EncounterDTO> encounterListDTO =
                    encounterDAO.getEncountersByVisitUUID(visitUuid);

            String parity = patientsDAO.getPatientAttributeValue(
                    patientUuid,
                    PatientAttributesDTO.Columns.PARITY
            );

            // Parsing parity
            if (parity != null && !parity.isEmpty()) {
                String[] parityArray = parity.split(",");
                if (parityArray.length > 1) {
                    try {
                        parityCount = Integer.parseInt(parityArray[0].trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            for (EncounterDTO encounter : encounterListDTO) {

                if (patientsDAO.checkStage(encounter.getEncounterTypeUuid(), StageEnum.ONE)) {
                    stageOneList.add(encounter);

                } else if (patientsDAO.checkStage(encounter.getEncounterTypeUuid(), StageEnum.TWO)) {
                    stageTwoList.add(encounter);
                } else if (patientsDAO.checkStage(encounter.getEncounterTypeUuid(), StageEnum.THREE)) {
                    //no need to show alert dialog for stage 3
                    return;
                }
            }

            double stageOneDuration = calculateStageDiff(stageOneList);
            double stageTwoDuration = 0.0;

            if (!stageTwoList.isEmpty()) {
                stageOneDuration = 0;
                stageTwoDuration = calculateStageDiff(stageTwoList);
            }


            String dialogMsg = "";

            if ((parityCount == 0 && stageOneDuration >= 12 * 60)
                    || (parityCount > 0 && stageOneDuration >= 10 * 60)) {
                dialogMsg = getString(R.string.prolonged_labour_detected);
            } else if ((parityCount == 0 && stageTwoDuration >= 3 * 60)
                    || (parityCount > 0 && stageTwoDuration >= 2 * 60)) {
                dialogMsg = getString(R.string.second_stage_prolonged);
            }

            String finalDialogMsg = dialogMsg;

            // Switching to UI thread
            mainHandler.post(() -> {
                if (!finalDialogMsg.isEmpty()) {
                    AppDialogUtils.showSingleButtonDialog(
                            (FragmentActivity) context,
                            context.getString(R.string.generic_warning),
                            finalDialogMsg,
                            context.getString(R.string.okay),
                            () -> null
                    );
                }
            });
        });
    }

    private double calculateStageDiff(ArrayList<EncounterDTO> encounterList) {

        if (encounterList == null || encounterList.size() < 2) {
            return 0;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime first = LocalDateTime.parse(
                encounterList.get(0).getEncounterTime(), formatter);

        LocalDateTime last = LocalDateTime.parse(
                encounterList.get(encounterList.size() - 1).getEncounterTime(), formatter);

        return Duration.between(first, last).toMinutes();
    }

//    private void triggerAlarm_Stage2_every15mins(String visitUuid) { // TODO: change 1min to 15mins..... // visituuid : 0 - 5
//        Calendar calendar = Calendar.getInstance(); // current time and from there evey 15mins notifi will be triggered...
//        calendar.add(Calendar.MINUTE, 15); // So that after 15mins this notifi is triggered and scheduled...
//        //  calendar.add(Calendar.MINUTE, 1); // Testing
//
//        Intent intent = new Intent(context, NotificationReceiver.class);
//        intent.putExtra("patientNameTimeline", patientName);
//        intent.putExtra("timeTag", 15);
//        intent.putExtra("patientUuid", patientUuid);
//        intent.putExtra("visitUuid", visitUuid);
//        intent.putExtra("providerID", providerID);
//        intent.putExtra("Stage2_Hour1_1", "Stage1_Hour1_1");

    /// /        intent.putExtra("Stage2_Hour1_1","Stage2_Hour1_1");
//
//        Log.v("timeline", "patientname_3 " + patientName + " " + patientUuid + " " + visitUuid);
//        Log.v("timeline", "visituuid_int_15min " + visitUuid.replaceAll("[^\\d]", ""));
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, Integer.parseInt(visitUuid.replaceAll("[^\\d]", "").substring(0, 5)), intent, NotificationUtils.getPendingIntentFlag());
//        // to set different alarams for different patients.
//
//        /*AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//        if (alarmManager != null) {
//            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
//                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
//        }*/
//    }
    private void triggerAlarm_Stage1_every30mins() { // TODO: change 1min to 15mins..... // visituuid : 2 - 7
        Calendar calendar = Calendar.getInstance(); // current time and from there evey 30mins notifi will be triggered...
        calendar.add(Calendar.MINUTE, 30); // So that after 15mins this notifi is triggered and scheduled...
        // calendar.add(Calendar.MINUTE, 2); // Testing

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("patientNameTimeline", patientName);
        intent.putExtra("timeTag", 30);
        intent.putExtra("patientUuid", patientUuid);
        intent.putExtra("visitUuid", visitUuid);
        intent.putExtra("providerID", providerID);
        intent.putExtra("Stage1_Hour1_1", "Stage1_Hour1_1");

        Log.v("timeline", "patientname_3 " + patientName + " " + patientUuid + " " + visitUuid);
        Log.v("timeline", "visituuid_int_30min " + visitUuid.replaceAll("[^\\d]", ""));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, Integer.parseInt(visitUuid.replaceAll("[^\\d]", "").substring(2, 7)), intent, NotificationUtils.getPendingIntentFlag()); // to set different alarams for different patients.

        /*AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_HALF_HOUR, pendingIntent);
        }*/
    }

    @Override
    public void onBackPressed() {
        if (whichScreenUserCameFromTag.equals("new") || whichScreenUserCameFromTag.equals("shiftChange")) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra(AppConstants.REFRESH_SCREEN_EVENT, true);
            startActivity(intent);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() { // when finish() called in Epartogram screen than onStart() is called here.
        super.onStart();
        /*if (adapter != null) {
            adapter.notifyDataSetChanged();
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    private void showToastAndUploadVisit(boolean isInserted, String message) {
        if (isInserted) {
            updateVisitDecisionPendingFlag(); //added for decision pending visits
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            cancelStage2_Alarm(); // cancel stage 2 alarm so that again 15mins interval doesnt starts.
            checkInternetAndUploadVisitEncounter(true);
        } else {
            Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateVisitDecisionPendingFlag() {
        Log.d(TAG, "updateVisitDecisionPendingFlag: visituuid : " + visitUuid);
        if (visitUuid != null && !visitUuid.isEmpty()) {
            if (new VisitAttributeListDAO().isSameOutcomePendingValueExist(visitUuid, "false"))
                return;
            long updated = new VisitAttributeListDAO().updateVisitAttribute(visitUuid, UuidDictionary.DECISION_PENDING, "false");
            if (updated > 0) {
                try {
                    isDecisionPending = false; //reset flag
                    layoutPendingFlag.setVisibility(View.GONE);
                    VisitsDAO visitsDAO = new VisitsDAO();
                    visitsDAO.updateVisitSync(visitUuid, "false");
                    //Intent intent = new Intent(AppConstants.VISIT_DECISION_PENDING_ACTION);
                    //IntelehealthApplication.getAppContext().sendBroadcast(intent);
                } catch (DAOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private final BroadcastReceiver syncBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.logD("syncBroadcastReceiver", "onReceive! " + intent);

            if (intent != null && intent.hasExtra(AppConstants.SYNC_INTENT_DATA_KEY) && isVisitCompleted) {
                isVisitCompleted = false;
                onBackPressed();
            }
        }
    };

    private void showToastAndUploadVisitForStage1(boolean isCompleteVisitCall, String message) {
        updateVisitDecisionPendingFlag(); //added for decision pending visits
        isVisitCompleted = isCompleteVisitCall;
        if (NetworkConnection.isOnline(getApplication())) {
            Toast.makeText(context, getResources().getString(R.string.syncInProgress), Toast.LENGTH_LONG).show();
            SyncUtils syncUtils = new SyncUtils();
            syncUtils.syncForeground(TAG);
        } else {
            Toast.makeText(context, context.getString(R.string.failed_synced), Toast.LENGTH_LONG).show();
        }
     /*   Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        checkInternetAndUploadVisitEncounter(true);*/
    }


//    public static PendingIntent getPendingIntent(Context context, RtcArgs args) {
//        Intent shiftChangeIntent = new Intent(context, HomeActivity.class);
//        shiftChangeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        shiftChangeIntent.putExtra("shiftChangeNotification", true);
//
//        return PendingIntent.getActivity(context, 0, buildExtra(shiftChangeIntent, args),
//                NotificationUtils.getPendingIntentFlag());
//    }

//    private static Intent buildExtra(Intent shiftIntent, RtcArgs args) {
//        shiftIntent.putExtra("patientNameTimeline", args.getPatientNameTimeline());
//        shiftIntent.putExtra("patientUuid", args.getPatientUuid());
//        shiftIntent.putExtra("visitUuid", args.getVisitUuid());
//        shiftIntent.putExtra("providerID", args.getProviderID());
//        return shiftIntent;
//    }

    private void collectEmergencyData() {
        EncounterDTO encounterDTO = encounterDAO.getEncounterByVisitUUIDLimit1(visitUuid);
        if (encounterDTO == null) return;
        Log.d(TAG, "collectEmergencyData: encounterDTO : " + new Gson().toJson(encounterDTO));

        String latestEncounterName = encounterDAO.getEncounterTypeNameByUUID(encounterDTO.getEncounterTypeUuid());
        Log.d("kzsos", "collectEmergencyData: latestEncounterName : " + latestEncounterName);
        int currentStage = -1;
        int currentHour = -1;

        if (latestEncounterName != null && !latestEncounterName.isEmpty()) {
            String name = latestEncounterName.toLowerCase();
            Log.d("kzsos", "collectEmergencyData: name : " + name);

            if (!name.contains("stage") || !name.contains("hour")) return;
            String[] parts = name
                    .replace("stage", "")
                    .replace("hour", "")
                    .split("_");

            if (parts.length != 3) return;
            currentStage = Integer.parseInt(parts[0]);
            currentHour = Integer.parseInt(parts[1]);
            Log.d("kzsos", "collectEmergencyData: currentStage : " + currentStage);
            Log.d("kzsos", "collectEmergencyData: currentHour : " + currentHour);

        }

        if (currentStage == -1 || currentHour == -1) return;
        int cardNumber = 1;
        EncounterDTO lastSosEncounter = encounterDAO.getSosEncounterByVisitUUIDLimit1(visitUuid);
        Log.d("kzsos", "collectEmergencyData: lastSosEncounter : " + new Gson().toJson(lastSosEncounter));

        if (lastSosEncounter != null) {
            String latestSosObs = new ObsDAO().getLatestSosObsByEncounterUuid(lastSosEncounter.getUuid());
            Log.d("kzsos", "collectEmergencyData: latestSosObs : " + new Gson().toJson(latestSosObs));

            if (latestSosObs != null && !latestSosObs.isEmpty()
                    && latestSosObs.toLowerCase().contains("stage")
                    && latestSosObs.toLowerCase().contains("hour")
                    && latestSosObs.toLowerCase().contains("sos")) {

                try {
                    String[] parts = latestSosObs
                            .toLowerCase()
                            .replace("stage", "")
                            .replace("hour", "")
                            .replace("sos", "")
                            .split("_");

                    if (parts.length == 3) {
                        int lastStage = Integer.parseInt(parts[0]);
                        int lastHour = Integer.parseInt(parts[1]);
                        int lastSOS = Integer.parseInt(parts[2]);

                        if (lastStage == currentStage && lastHour == currentHour) {
                            cardNumber = lastSOS + 1;
                        }
                    }
                    Log.d("kzsos", "collectEmergencyData: cardNumber : " + cardNumber);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        String nextEncounterTypeName = "Stage" + currentStage + "_Hour" + currentHour + "_SOS" + cardNumber;
        String encounterUuid = UUID.randomUUID().toString();
        Log.d("kzsos", "collectEmergencyData: nextEncounterTypeName : " + nextEncounterTypeName);
        //1 Create sos encounter
        try {
            EncounterDTO sosEncounter = new EncounterDTO();
            sosEncounter.setUuid(encounterUuid);
            sosEncounter.setVisituuid(encounterDTO.getVisituuid());
            sosEncounter.setEncounterTime(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
            sosEncounter.setProvideruuid(encounterDTO.getProvideruuid());
            sosEncounter.setEncounterTypeUuid(UuidDictionary.LCG_SOS);
            sosEncounter.setVoided(encounterDTO.getVoided());
            sosEncounter.setPrivacynotice_value(encounterDTO.getPrivacynotice_value());
            sosEncounter.setSyncd(false);
            encounterDAO.createSosEncountersToDB(sosEncounter);

            //2 Create sos - obs

            ObsDTO obsDTO = new ObsDTO();
            obsDTO.setUuid(UUID.randomUUID().toString());
            obsDTO.setConceptuuid(UuidDictionary.SOS_STAGE_HOUR);
            obsDTO.setCreator(sessionManager.getCreatorID());
            obsDTO.setEncounteruuid(encounterUuid);
            obsDTO.setCreatorUuid(sessionManager.getCreatorID());
            obsDTO.setValue(nextEncounterTypeName);
            obsDTO.setCreatedDate(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
            //obsDTO.setComment(nextEncounterTypeName);
            obsDAO.insertObs(obsDTO);

        } catch (DAOException e) {
            Log.d(TAG, "collectEmergencyData: e : " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        //new ObsDAO().createEncounterType(encounterUuid, EncounterDTO.Type.SOS.name(), sessionManager.getCreatorID(), TAG);
        //isNewEncounterCreated = createNewEncounter(encounterUuid, visitUuid, nextEncounterTypeName);

        fetchAllEncountersFromVisitForTimelineScreen(visitUuid);
    }

    /*  private void showErrorOnNoInternet() {
          AppDialogUtils.showSingleButtonDialog(
                  this,
                  getString(R.string.no_internet_timeline_screen_title),
                  getString(R.string.no_internet_timeline_screen_body),
                  getString(R.string.ok),
                  () -> {
                      return null;
                  }
          );
      }*/
    private void showNoDataDialogForViewLcg(String title, String message) {
        AppDialogUtils.showSingleButtonDialog(
                this,
                title,
                message,
                getString(R.string.ok),
                () -> {
                    return null;
                }
        );
    }

    private void manageEndStageButtonText() {
        EncounterDTO encounterDTO = encounterDAO.getEncounterByVisitUUIDLimit1(visitUuid); // get latest encounter.
        String latestEncounterName = encounterDAO.findCurrentStage(encounterDTO.getVisituuid());
        Log.d(TAG, "initUI: latestEncounterName : " + latestEncounterName);
        if (isVCEPresent.equalsIgnoreCase("")) { // "" ie. not present
            endStageButton.setEnabled(true);
            endStageButton.setClickable(true);
            endStageButton.setBackground(ContextCompat.getDrawable(context, R.drawable.ic_rectangle_76));
            if (latestEncounterName.toLowerCase().contains("stage3")) {
                stageNo = 3;
                endStageButton.setText(context.getResources().getText(R.string.end3StageButton));
            } else if (latestEncounterName.toLowerCase().contains("stage2")) {
                stageNo = 2;
                endStageButton.setText(context.getResources().getText(R.string.end2StageButton));
            } else if (latestEncounterName.toLowerCase().contains("stage1")) {
                stageNo = 1;
                endStageButton.setText(context.getResources().getText(R.string.endStageButton));
            } else {
                stageNo = 0;
            }

            if (!hwHasEditAccess) {
                fabc.setEnabled(false);
                fabv.setEnabled(false);
                fabSOS.setEnabled(false);
                fabPrescription.setEnabled(false);
                endStageButton.setEnabled(false);
            }

        } else {
            setVisitOutcome();
        }

        //setting shift to postnatal button visibility here bcs we are getting last stage here
        setupShiftToPostnatalWardBtn();
    }

    private void showPostpartumReport() {
        Intent intent;

        if (currentNetworkStatus().getHasInternet()) {
            intent = new Intent(context, ViewPostPartumReportActivity.class);
        } else {
            intent = new Intent(context, OfflineStage3ViewActivity.class);
        }

        intent.putExtra("patientName", patientName);
        intent.putExtra("patientuuid", patientUuid);
        intent.putExtra("visituuid", visitUuid);
        startActivity(intent);
    }

    private void showViewDropdownMenu(View anchor) {
        PopupMenu popup = new PopupMenu(
                new ContextThemeWrapper(this, R.style.RoundedPopupMenu),
                anchor,
                Gravity.END
        );
        popup.getMenuInflater().inflate(R.menu.menu_view_options_partogram, popup.getMenu());

        try {
            Field field = PopupMenu.class.getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuHelper = field.get(popup);
            menuHelper.getClass()
                    .getDeclaredMethod("setForceShowIcon", boolean.class)
                    .invoke(menuHelper, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_view_lcg) {
                boolean doesVisitHasData = obsDAO.getObsCountForVisit(visitUuid);
                if (doesVisitHasData) {
                    showEpartogram();
                } else {
                    showNoDataDialogForViewLcg(getString(R.string.no_data_for_view_lcg_title), getString(R.string.no_data_for_view_lcg_body));
                }
                return true;
            } else if (id == R.id.action_view_postpartum) {
                boolean doesVisitHasStage3Data = obsDAO.isStage3ObsForVisitExist(visitUuid);
                if (doesVisitHasStage3Data) {
                    showPostpartumReport();
                } else {
                    showNoDataDialogForViewLcg(getString(R.string.no_data_for_view_lcg_title), getString(R.string.no_data_for_view_postpartum_report));
                }
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void setVisitOutcome() {
        VisitOutcome outcome = new ObsDAO().getCompletedVisitType(isVCEPresent);
        Log.d(TAG, "setVisitOutcome:outcome :  " + new Gson().toJson(outcome));
        endStageButton.setVisibility(View.INVISIBLE);
        if (outcome != null && outcome.getOutcome() != null && !outcome.getOutcome().equalsIgnoreCase("")) {
            outcomeTV.setVisibility(View.VISIBLE);

            SpannableStringBuilder combinedOutcome = new SpannableStringBuilder();

            // Mother outcome (existing business logic untouched)
            String motherStr = getString(R.string.lbl_mother_outcome, outcome.getOutcome());
            combinedOutcome.append(Html.fromHtml(motherStr, Html.FROM_HTML_MODE_COMPACT));

            // Baby outcome - append in new line if present
            if (outcome.getBabyOutcome() != null && !outcome.getBabyOutcome().isEmpty()) {
                String babyStr = getString(R.string.lbl_newborn_outcome, outcome.getBabyOutcome());
                combinedOutcome.append("\n");
                combinedOutcome.append(Html.fromHtml(babyStr, Html.FROM_HTML_MODE_COMPACT));
            }

            outcomeTV.setText(combinedOutcome);
            outcomeTV.setGravity(Gravity.CENTER);
            checkForOutOfTime(outcome);
        }
        fabc.setVisibility(View.GONE);
        fabv.setVisibility(View.GONE);
        fabSOS.setVisibility(View.GONE);
        fabPrescription.setVisibility(View.GONE);
    }

    @Override
    public void onNetworkAvailable(@NotNull NetworkStatus status) {
        super.onNetworkAvailable(status);
    }

    @Override
    public void onNetworkChanged(@NotNull NetworkStatus status) {
        super.onNetworkChanged(status);
    }

    @Override
    public void onNetworkLost() {
        super.onNetworkLost();
    }
}