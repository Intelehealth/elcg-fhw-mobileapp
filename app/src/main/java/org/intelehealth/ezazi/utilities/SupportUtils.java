package org.intelehealth.ezazi.utilities;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.intelehealth.ezazi.R;

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
        View footer = activity.findViewById(R.id.bottomNavMenu);
        View appBar = activity.findViewById(R.id.simpleAppBar); // if exists
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
    public static void enableProperPaddingV2(Activity activity) {

        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false); // IMPORTANT

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        window.setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
        window.setNavigationBarColor(ContextCompat.getColor(activity, R.color.white));

        View root = activity.findViewById(android.R.id.content);
        View footer = activity.findViewById(R.id.bottomNavMenu);
        View appBar = activity.findViewById(R.id.simpleAppBar);
        View scrollContainer =  null;// activity.findViewById(R.id.scrollContainer);

        // Light status bar icons
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);

        // ROOT GETS ZERO PADDING ALWAYS
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            view.setPadding(0, 0, 0, 0);
            return insets;
        });

        // SCROLLABLE CONTENT GETS FULL INSETS
        if (scrollContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollContainer, (view, insets) -> {

                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

                int bottom = Math.max(systemBars.bottom, ime.bottom);

                view.setPadding(
                        systemBars.left,
                        systemBars.top,
                        systemBars.right,
                        bottom
                );

                return insets;
            });
        }

        // BOTTOM NAV BAR GETS ZERO PADDING ALWAYS
        if (footer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(footer, (view, insets) -> {

                // Prevent Android 15 from adding bottom 132px padding
                view.setPadding(0, 0, 0, 0);

                return insets;
            });
        }

        // APP BAR GETS ONLY TOP INSET
        if (appBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBar, (view, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                view.setPadding(
                        view.getPaddingLeft(),
                        systemBars.top,
                        view.getPaddingRight(),
                        view.getPaddingBottom()
                );

                return insets;
            });
        }
    }

}
