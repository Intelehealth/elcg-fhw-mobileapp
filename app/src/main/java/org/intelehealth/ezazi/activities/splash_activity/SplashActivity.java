package org.intelehealth.ezazi.activities.splash_activity;

import static org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.intelehealth.ezazi.BuildConfig;
import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.activities.homeActivity.HomeActivity;
import org.intelehealth.ezazi.activities.loginActivity.LoginActivity;
import org.intelehealth.ezazi.activities.setupActivity.SetupActivity;
import org.intelehealth.ezazi.dataMigration.SmoothUpgrade;
import org.intelehealth.ezazi.services.firebase_services.TokenRefreshUtils;
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment;
import org.intelehealth.ezazi.ui.dialog.PermissionRequiredDialog;
import org.intelehealth.ezazi.utilities.Logger;
import org.intelehealth.ezazi.utilities.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SplashActivity extends AppCompatActivity {
    private static final int GROUP_PERMISSION_REQUEST = 1000;
    SessionManager sessionManager = null;
    //    ProgressDialog TempDialog;
    //int i = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_ezazi);
        enableProperPadding(SplashActivity.this);
//        Getting App language through the session manager
        sessionManager = new SessionManager(SplashActivity.this);
        //  startService(new Intent(getBaseContext(), OnClearFromRecentService.class));
        String appLanguage = sessionManager.getAppLanguage();
        if (!appLanguage.equalsIgnoreCase("")) {
            Locale locale = new Locale(appLanguage);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }
        // refresh the fcm token
        TokenRefreshUtils.refreshToken(this);
        initFirebaseRemoteConfig();
        ImageView logo = findViewById(R.id.ivSplashLogo);
        logo.setScaleX(1.3f);
        logo.setScaleY(1.3f);
    }

    private void initFirebaseRemoteConfig() {
        FirebaseApp.initializeApp(this);
        FirebaseRemoteConfig instance = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        instance.setConfigSettingsAsync(configSettings);

        instance.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if (task.isSuccessful() && !isFinishing()) {
                    long force_update_version_code = instance.getLong("force_update_version_code");
                    if (force_update_version_code > BuildConfig.VERSION_CODE) {
                        showForceUpdateDialog();
                    } else {
                        checkPerm();
                    }
                } else {
                    checkPerm();
                }
            }
        });
    }

    private void showForceUpdateDialog() {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(this)
                .title(R.string.generic_warning)
                .positiveButtonLabel(R.string.ok)
                .hideNegativeButton(true)
                .content(getString(R.string.warning_app_update))
                .build();

        dialog.setListener(() -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
            }
            finish();
        });

        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());
    }

    private void checkPerm() {
        if (checkAndRequestPermissions()) {
            if (sessionManager.isMigration()) {
                final Handler handler = new Handler();
                //Do something after 100ms
                handler.postDelayed(this::nextActivity, 2000);
            } else {
                final Handler handler = new Handler();
                handler.postDelayed(() -> { //Do something after 100ms
                    SmoothUpgrade smoothUpgrade = new SmoothUpgrade(SplashActivity.this);
                    boolean smoothupgrade = smoothUpgrade.checkingDatabase();
                    if (smoothupgrade) {
                        nextActivity();
                    }
                }, 2000);
            }
        }
       /* PermissionListener permissionlistener = new PermissionListener() {

            @Override
            public void onPermissionGranted() {
//                Toast.makeText(SplashActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
//                Timer t = new Timer();
//                t.schedule(new splash(), 2000);

//                TempDialog = new ProgressDialog(SplashActivity.this, R.style.AlertDialogStyle);
//                TempDialog.setMessage("Data migrating...");
//                TempDialog.setCancelable(false);
//                TempDialog.setProgress(i);
//                TempDialog.show();


            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(SplashActivity.this, getString(R.string.permission_denied) + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }

        };
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage(R.string.reject_permission_results)
                .setPermissions(*//*Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,*//*
                        Manifest.permission.GET_ACCOUNTS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GROUP_PERMISSION_REQUEST) {
            boolean allGranted = grantResults.length != 0;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
           /* if (allGranted) {
                checkPerm();
            } else {
                showPermissionDeniedAlert(permissions);
            }*/
            if (allGranted) {

                Fragment dialog =
                        getSupportFragmentManager().findFragmentByTag("PermissionDialog");

                if (dialog != null) {
                    ((DialogFragment) dialog).dismissAllowingStateLoss();
                }

                checkPerm();
            }else {
                showPermissionDeniedAlert(permissions);
            }

        }
    }


   /* private void showPermissionDeniedAlert(String[] permissions) {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(this)
                .title(R.string.required_permission)
                .positiveButtonLabel(R.string.retry_again)
                .negativeButtonLabel(R.string.ok_close_now)
                .content(getString(R.string.reject_permission_results))
                .build();

        dialog.setListener(() -> checkPerm());
        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());
//        MaterialAlertDialogBuilder alertdialogBuilder = new MaterialAlertDialogBuilder(this);
//
//        // AlertDialog.Builder alertdialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
//        alertdialogBuilder.setMessage(R.string.reject_permission_results);
//        alertdialogBuilder.setPositiveButton(R.string.retry_again, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                checkPerm();
//            }
//        });
//        alertdialogBuilder.setNegativeButton(R.string.ok_close_now, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                finish();
//            }
//        });
//
//        AlertDialog alertDialog = alertdialogBuilder.create();
//        alertDialog.show();
//
//        Button positiveButton = alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
//        Button negativeButton = alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
//
//        positiveButton.setTextColor(getResources().getColor(R.color.colorPrimary));
//        //positiveButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
//
//        negativeButton.setTextColor(getResources().getColor(R.color.colorPrimary));
//        //negativeButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
//        IntelehealthApplication.setAlertDialogCustomTheme(this, alertDialog);
    }
*/

    private boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int getAccountPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS);
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            int fullScreenIntent = ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FULL_SCREEN_INTENT);
            if (fullScreenIntent != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.USE_FULL_SCREEN_INTENT);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES);
            int notificationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        int phoneStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);


        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
       /* if (getAccountPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.GET_ACCOUNTS);
        }*/
        if (writeExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listPermissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        if (phoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), GROUP_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private void nextActivity() {

        boolean setup = sessionManager.isSetupComplete();

        String LOG_TAG = "SplashActivity";
        Logger.logD(LOG_TAG, String.valueOf(setup));
        if (setup) {

            if (sessionManager.isLogout()) {
                Logger.logD(LOG_TAG, "Starting login");
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Logger.logD(LOG_TAG, "Starting home");
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
                finish();
            }

        } else {
            Logger.logD(LOG_TAG, "Starting setup");
            Intent intent = new Intent(this, SetupActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
//        TempDialog.dismiss();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void showPermissionDeniedAlert(String[] permissions) {

        Fragment existing =
                getSupportFragmentManager().findFragmentByTag("PermissionDialog");

        if (existing != null) {
            return; // dialog already visible, don't show again
        }

        String submit = getString(R.string.retry_again);
        String close = getString(R.string.ok_close_now);

        PermissionRequiredDialog dialog =
                new PermissionRequiredDialog(
                        close,
                        submit,
                        new PermissionRequiredDialog.OnActionClickListener() {

                            @Override
                            public void onRetryClicked() {
                                checkPerm();
                            }

                            @Override
                            public void onCloseClicked() {
                            }
                        });

        dialog.show(getSupportFragmentManager(), "PermissionDialog");
    }
}
