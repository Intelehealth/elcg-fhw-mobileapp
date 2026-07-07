package org.intelehealth.ezazi.activities.visitCreation;

import static org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity;

/**
 * Host for the visit-creation flow. Currently shows the obstetric intake as its first
 * and only step; creating the visit and moving on to the timeline is wired later.
 */
public class VisitCreationActivity extends BaseActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_visit_creation);
        super.onCreate(savedInstanceState);
        initUI();
        setupActionBar();
        enableProperPadding(VisitCreationActivity.this);
    }

    @Override
    protected int getScreenTitle() {
        return R.string.visit_creation_title;
    }

    private void initUI() {
        View viewToolbar = findViewById(R.id.toolbar_common);
        Toolbar toolbar = viewToolbar.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_visit_creation, new ObstetricIntakeFragment())
                .commit();
    }
}
