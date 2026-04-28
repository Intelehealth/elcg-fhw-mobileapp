package org.intelehealth.ezazi.ui.shared;

import static org.intelehealth.ezazi.app.AppConstants.SHIFTED_DATA;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.activities.visitSummaryActivity.ShiftChangeData;
import org.intelehealth.ezazi.activities.visitSummaryActivity.TimelineVisitSummaryActivity;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.ProviderDAO;
import org.intelehealth.ezazi.database.dao.SyncDAO;
import org.intelehealth.ezazi.databinding.DialogShiftedPatientsBinding;
import org.intelehealth.ezazi.optimized_sync.network.NetworkConnectivityListener;
import org.intelehealth.ezazi.optimized_sync.network.NetworkConnectivityManager;
import org.intelehealth.ezazi.optimized_sync.network.NetworkStatus;
import org.intelehealth.ezazi.syncModule.SyncUtils;
import org.intelehealth.ezazi.ui.dialog.CustomViewDialogFragment;
import org.intelehealth.ezazi.ui.dialog.adapter.ShiftedPatientAdapter;
import org.intelehealth.ezazi.ui.rtc.activity.EzaziChatActivity;
import org.intelehealth.ezazi.utilities.AppNotification;
import org.intelehealth.ezazi.utilities.NotificationUtils;
import org.intelehealth.ezazi.utilities.exception.DAOException;
import org.intelehealth.klivekit.model.ChatMessage;
import org.intelehealth.klivekit.model.RtcArgs;
import org.intelehealth.klivekit.socket.SocketManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Created by Vaghela Mithun R. on 03-06-2023 - 19:29.
 * Email : mithun@intelehealth.org
 * Mob   : +919727206702
 **/
public class BaseActivity extends AppCompatActivity implements SocketManager.NotificationListener, NetworkConnectivityListener {
    private static final String TAG = "BaseActivity";
    private NetworkConnectivityManager networkManager;

    private FrameLayout flInternetStatus;
    private TextView tvInternetStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        hideBottomSystemTaskbar();
        super.onCreate(savedInstanceState);
        networkManager = new NetworkConnectivityManager(BaseActivity.this);
        networkManager.addListener(this);
        SocketManager.getInstance().setNotificationListener(this);
        showShiftedPatientDialog(getIntent());
    }

    private void hideBottomSystemTaskbar() {
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Configure the behavior of the hidden system bars.
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        // Add a listener to update the behavior of the toggle fullscreen button when
        // the system bars are hidden or revealed.
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view, windowInsets) -> {
            // You can hide the caption bar even when the other system bars are visible.
            // To account for this, explicitly check the visibility of navigationBars()
            // and statusBars() rather than checking the visibility of systemBars().
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())) {
                    // Hide both the status bar and the navigation bar.
                    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
                }
            }
            return view.onApplyWindowInsets(windowInsets);
        });
    }

    protected void initializeNetworkBannerComponents() {
        flInternetStatus = findViewById(R.id.fl_connection_bar);
        tvInternetStatus = findViewById(R.id.tv_connection_status);
    }

    @Override
    public void showNotification(@NonNull ChatMessage chatMessage) {
        RtcArgs args = new RtcArgs();
        args.setPatientName(chatMessage.getPatientName());
        args.setPatientId(chatMessage.getPatientId());
        args.setVisitId(chatMessage.getVisitId());
        args.setNurseId(chatMessage.getToUser());
        args.setDoctorUuid(chatMessage.getFromUser());
        Log.e(TAG, "showNotification: " + args.toJson());
        try {
            String title = new ProviderDAO().getProviderName(args.getDoctorUuid());
            new AppNotification.Builder(this)
                    .title(title)
                    .body(chatMessage.getMessage())
                    .pendingIntent(EzaziChatActivity.getPendingIntent(this, args))
                    .send();
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showShiftedPatientDialog(intent);
    }

    private void showShiftedPatientDialog(Intent intent) {
        if (intent == null || intent.getExtras() == null) return;
        if (intent.hasExtra(SHIFTED_DATA)) {
            ShiftChangeData data = getShiftedData(intent);
            if (data == null) return;

            DialogShiftedPatientsBinding binding = DialogShiftedPatientsBinding.inflate(getLayoutInflater(), null, false);
            binding.setNurse(data.getAssignorNurse());
            binding.rvShiftedPatientList.setLayoutManager(new LinearLayoutManager(this));
            binding.rvShiftedPatientList.setAdapter(new ShiftedPatientAdapter(BaseActivity.this, data.buildPatients()));
            CustomViewDialogFragment dialogFragment = new CustomViewDialogFragment.Builder(this)
                    .view(binding.getRoot())
                    .positiveButtonLabel(R.string.okay)
                    .hideNegativeButton(true)
                    .build();
//            dialogFragment.setWrapContentDialog(true);
            dialogFragment.setListener(() -> new SyncDAO().pullData_Background(BaseActivity.this));
            dialogFragment.show(getSupportFragmentManager(), dialogFragment.getClass().getName());
        }
    }

    private ShiftChangeData getShiftedData(Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return intent.getSerializableExtra(SHIFTED_DATA, ShiftChangeData.class);
        } else {
            return (ShiftChangeData) intent.getSerializableExtra(SHIFTED_DATA);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        networkManager.startListening();
        handleCurrentInternetStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //registerReceiver(shiftedPatientReceiver, new IntentFilter(AppConstants.getShiftedPatientReceiver()));
        ContextCompat.registerReceiver(BaseActivity.this, shiftedPatientReceiver, new IntentFilter(AppConstants.getShiftedPatientReceiver()), ContextCompat.RECEIVER_EXPORTED);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(shiftedPatientReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        networkManager.stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkManager.removeListener(this);
    }

    protected void handleCurrentInternetStatus() {
        NetworkStatus status = networkManager.getCurrentStatus();
        if (status.isConnected()) {
            onNetworkAvailable(status);
        } else {
            onNetworkLost();
        }
    }

    private final BroadcastReceiver shiftedPatientReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showShiftedPatientDialog(intent);
        }
    };

    @Override
    public void onNetworkAvailable(@NotNull NetworkStatus status) {
        updateConnectionBanner(status.getHasInternet(), flInternetStatus, tvInternetStatus);
    }

    @Override
    public void onNetworkLost() {
        updateConnectionBanner(false, flInternetStatus, tvInternetStatus);
    }

    @Override
    public void onNetworkChanged(@NotNull NetworkStatus status) {
        updateConnectionBanner(status.getHasInternet(), flInternetStatus, tvInternetStatus);
    }

    private boolean sessionExperiencedHardwareDrop = false;
    private Runnable hideRunnable;

    protected void updateConnectionBanner(boolean isOnline, FrameLayout flInternetStatus, TextView tvInternetStatus) {

        if (flInternetStatus == null || tvInternetStatus == null) return;

        // Cancel any pending hide + animations
        if (hideRunnable != null) {
            flInternetStatus.removeCallbacks(hideRunnable);
            hideRunnable = null;
        }

        flInternetStatus.animate().cancel();
        flInternetStatus.setAlpha(1f);

        if (isOnline) {
            // Skip "Back Online" on first launch
            if (!sessionExperiencedHardwareDrop) {
                flInternetStatus.setVisibility(View.GONE);
                return;
            }

            // Show green banner
            flInternetStatus.setVisibility(View.VISIBLE);
            tvInternetStatus.setText(R.string.back_online);
            tvInternetStatus.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.green)
            );

            // Prepare hide runnable
            hideRunnable = () -> flInternetStatus.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        if (flInternetStatus.getAlpha() == 0f) {
                            flInternetStatus.setVisibility(View.GONE);
                            sessionExperiencedHardwareDrop = false;
                        }
                    });

            // Schedule it
            flInternetStatus.postDelayed(hideRunnable, 3000);

        } else {
            // Mark that we've seen a drop
            sessionExperiencedHardwareDrop = true;

            // Show red banner immediately
            flInternetStatus.setVisibility(View.VISIBLE);
            tvInternetStatus.setText(R.string.no_internet);
            tvInternetStatus.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.red)
            );
        }
    }
}
