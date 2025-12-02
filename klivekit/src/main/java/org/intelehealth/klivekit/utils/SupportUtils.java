package org.intelehealth.klivekit.utils;

import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.intelehealth.klivekit.R;

import timber.log.Timber;

public class SupportUtils {
    public static void enableProperPadding(Activity activity) {

        Window window = activity.getWindow();

        // make system bars opaque again (disable edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        window.setStatusBarColor(ContextCompat.getColor(activity, R.color.white));
        window.setNavigationBarColor(ContextCompat.getColor(activity, R.color.white));

        View root = activity.findViewById(android.R.id.content);
        View footer = null;//activity.findViewById(R.id.bottomNavMenu);
        View appBar = null;//activity.findViewById(R.id.simpleAppBar); // if exists
        // Set dark icons
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        // Apply only bottom inset (not top)
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {

            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(systemBars.bottom, imeInsets.bottom);

            // Bottom padding = whichever is larger (keyboard or system nav bar)
            int bottomPadding = Math.max(systemBars.bottom, imeInsets.bottom);

            Timber.tag("SupportUtils").v("MAIN-CONTENT- Activity" + activity.getLocalClassName() + "systemBars.bottom: " + systemBars.bottom + ", imeInsets.bottom: " + imeInsets.bottom + ", applied bottomPadding: " + bottomPadding);

            // Apply padding to the main root layout
            view.setPadding(systemBars.left, systemBars.top-(systemBars.top/4), systemBars.right, bottomPadding);
            //view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // Root gets NO bottom padding
            //view.setPadding(systemBars.left, 0, systemBars.right, 0);

            // Footer gets ALL the bottom inset
           /* if(footer != null) {
                footer.setPadding(
                        footer.getPaddingLeft(),
                        footer.getPaddingTop(),
                        footer.getPaddingRight(),
                        bottomInset
                );
            }
            // App bar gets top inset
            if (appBar != null) {
                appBar.setPadding(0, systemBars.top, 0, 0);
            }*/
            return insets;
        });

        if (footer != null)
            ViewCompat.setOnApplyWindowInsetsListener(footer, (view, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                int bottomInset = Math.max(systemBars.bottom, imeInsets.bottom);
                Timber.tag("SupportUtils").v("FOOTER - Activity" + activity.getLocalClassName() + "systemBars.bottom: " + systemBars.bottom + ", imeInsets.bottom: " + imeInsets.bottom + ", applied bottomInset: " + bottomInset);

                // Remove automatic padding!
                view.setPadding(
                        0,0,0,0
                );
                return insets;
            });


    }

}
