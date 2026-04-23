package org.intelehealth.ezazi.ui.shared;

import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.optimized_sync.network.NetworkStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Created by Vaghela Mithun R. on 03-06-2023 - 19:29.
 * Email : mithun@intelehealth.org
 * Mob   : +919727206702
 **/
public abstract class BaseActionBarActivity extends BaseActivity {

    private FrameLayout flInternetStatus;
    private TextView tvInternetStatus;

    protected void initializeNetworkBanner() {
        flInternetStatus = findViewById(R.id.fl_connection_bar);
        tvInternetStatus = findViewById(R.id.tv_connection_status);
        handleCurrentInternetStatus();
    }

    protected void setupActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        if (getScreenTitle() != 0)
            getSupportActionBar().setTitle(getString(getScreenTitle()));
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackNavigate());
    }

    protected abstract @StringRes int getScreenTitle();

    protected void onBackNavigate() {
        onBackPressed();
    }

    @Override
    public void onNetworkAvailable(@NotNull NetworkStatus status) {
        updateConnectionBanner(status.getHasInternet(), flInternetStatus, tvInternetStatus);
    }

    @Override
    public void onNetworkChanged(@NotNull NetworkStatus status) {
        updateConnectionBanner(status.getHasInternet(), flInternetStatus, tvInternetStatus);
    }

    @Override
    public void onNetworkLost() {
        updateConnectionBanner(false, flInternetStatus, tvInternetStatus);
    }
}
