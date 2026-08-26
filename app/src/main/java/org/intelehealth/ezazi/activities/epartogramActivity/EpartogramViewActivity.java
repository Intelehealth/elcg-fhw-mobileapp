package org.intelehealth.ezazi.activities.epartogramActivity;

import static org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.ajalt.timberkt.Timber;

import org.intelehealth.ezazi.BuildConfig;
import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.activities.epartogramActivity.print.LcgPdfExport;
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment;
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity;
import org.intelehealth.ezazi.utilities.FileUtils;
import org.intelehealth.ezazi.utilities.NetworkConnection;
import org.intelehealth.ezazi.utilities.SessionManager;
import org.intelehealth.ezazi.utilities.WebViewPdfExporter;
import org.intelehealth.ezazi.widget.materialprogressbar.CustomProgressDialog;

import java.io.File;

public class EpartogramViewActivity extends BaseActionBarActivity {

    private WebView webView;
    private static final String TAG = "EpartogramViewActivity";

    private static final int REQUEST_STORAGE_PERMISSION = 4321;


    private String patientUuid, visitUuid;
    private static final String URL = BuildConfig.SERVER_URL + "/intelehealth/index.html#/epartogram/";

    private SwipeRefreshLayout mySwipeRefreshLayout;
    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;

    private String webArchiveFileDir;
    private SessionManager sessionManager;

    private Handler timeoutHandler;
    private Handler printJobHandler;
    private boolean isPageLoaded = false;
    private boolean isErrorShown = false;
    private CustomProgressDialog progressDialog;

    private Menu optionsMenu;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_epartogram_ezazi);
        super.onCreate(savedInstanceState);
        setupActionBar();
        enableProperPadding(EpartogramViewActivity.this);
        timeoutHandler = new Handler(Looper.getMainLooper());
        progressDialog = new CustomProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.show();

        sessionManager = new SessionManager(this);
        webArchiveFileDir = FileUtils.getProjectCatchDir(this);
        Timber.tag(TAG).d("webArchive =>%s", webArchiveFileDir);
        Intent intent = this.getIntent();
        if (intent != null) {
            patientUuid = intent.getStringExtra("patientuuid");
            visitUuid = intent.getStringExtra("visituuid");
        }
        Log.v("epartog", "epratog: " + "puid: " + patientUuid + "--" + " vuid: " + visitUuid);

        webView = findViewById(R.id.webview_epartogram);
        mySwipeRefreshLayout = (SwipeRefreshLayout) this.findViewById(R.id.swipeContainer);

        webView.clearCache(true);
        webView.clearHistory();

        webView.setSaveEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setUserAgentString("Android");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setDefaultTextEncodingName("UTF-8");
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(webViewClient);

        if (NetworkConnection.isOnline(this)) {
            Log.e(TAG, "onCreate: isOnline");
            isPageLoaded = false;
            progressDialog.show();

            timeoutHandler.postDelayed(() -> {
                if (!isPageLoaded) {
                    Log.e(TAG, "Manual timeout triggered");
                    webView.stopLoading();
                    progressDialog.dismiss();
                    handleError();
                }
            }, 20000);  // 20 seconds
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.loadUrl(URL + visitUuid);
        } else if (!sessionManager.getLCGContentFile(visitUuid).isEmpty()) {
            Log.e(TAG, "onCreate: isOffline, archive available");
            showInternetRequireDialog();
        } else {
            webView.setVisibility(View.GONE);
            showPageLoadingErrorDialog();
        }

        Log.v("epartog", "webviewUrl: " + URL + visitUuid);
        mySwipeRefreshLayout.setOnRefreshListener(() -> webView.reload());
    }

    // region ---- Options menu (Print / Download PDF) ----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_epartogram, menu);
        optionsMenu = menu;
        setExportActionsEnabled(isPageLoaded && !isErrorShown);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_print) {
            printEpartogram();
            return true;
        } else if (id == R.id.action_download_pdf) {
            downloadEpartogramAsPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Print / Download only make sense once the partogram has actually rendered,
     * otherwise the user exports a blank page.
     */
    private void setExportActionsEnabled(boolean enabled) {
        if (optionsMenu == null) return;
        MenuItem print = optionsMenu.findItem(R.id.action_print);
        MenuItem download = optionsMenu.findItem(R.id.action_download_pdf);
        if (print != null) {
            print.setEnabled(enabled);
            if (print.getIcon() != null) print.getIcon().setAlpha(enabled ? 255 : 100);
        }
        if (download != null) {
            download.setEnabled(enabled);
            if (download.getIcon() != null) download.getIcon().setAlpha(enabled ? 255 : 100);
        }
    }

    // endregion

    // region ---- Print ----

    /**
     * Expands the page's scroll containers (so the full LCG width, including
     * Stage 2, gets painted) and hands the WebView's print adapter to the
     * system PrintManager at 100%.
     *
     * Paper size and orientation are deliberately NOT requested. The print
     * dialog owns them: the deployment prints on whatever the site has (A4
     * today, larger later), and the print pipeline scales the page to whatever
     * sheet is chosen. Requesting a sheet here achieved nothing — services
     * substitute their own default for any size they do not stock — while
     * pre-zooming the page could only ever make the result smaller, never
     * larger, than that scaling already makes it.
     */
    private void printEpartogram() {
        if (!isPageLoaded || isErrorShown) {
            Toast.makeText(this, R.string.epartogram_not_loaded, Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.show();

        WebViewPdfExporter.expandAndMeasure(webView, cssWidth -> {
            Timber.tag(TAG).d("Print: expanded content width = " + cssWidth + " CSS px");
            webView.postDelayed(this::launchPrintJob, 300);
        });
    }

    private void launchPrintJob() {
        progressDialog.dismiss();
        try {
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            String jobName = getString(R.string.app_name) + "_ePartogram_" + visitUuid;
            PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);

            PrintAttributes attributes = new PrintAttributes.Builder()
                    .setResolution(new PrintAttributes.Resolution("epartogram", "epartogram", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();

            PrintJob printJob = printManager.print(jobName, adapter, attributes);

            // Step 3: the job renders asynchronously — only restore the page's
            // CSS once it reaches a terminal state, otherwise the printed pages
            // would capture the restored (clipped) layout.
            monitorPrintJob(printJob);

        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Print failed");
            WebViewPdfExporter.restorePageStyles(webView);
            Toast.makeText(this, R.string.epartogram_export_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void monitorPrintJob(final PrintJob printJob) {
        if (printJobHandler != null) printJobHandler.removeCallbacksAndMessages(null);
        printJobHandler = new Handler(Looper.getMainLooper());

        Runnable checker = new Runnable() {
            @Override
            public void run() {
                if (printJob == null
                        || printJob.isCompleted()
                        || printJob.isFailed()
                        || printJob.isCancelled()) {
                    WebViewPdfExporter.restorePageStyles(webView);
                } else {
                    printJobHandler.postDelayed(this, 1000);
                }
            }
        };
        printJobHandler.postDelayed(checker, 1000);
    }

    // endregion

    // region ---- Download as PDF ----

    private void downloadEpartogramAsPdf() {
        if (!isPageLoaded || isErrorShown) {
            Toast.makeText(this, R.string.epartogram_not_loaded, Toast.LENGTH_SHORT).show();
            return;
        }

        // Pre-Android 10 needs WRITE_EXTERNAL_STORAGE to place the file in public Downloads.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            return;
        }

        generatePdf();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePdf();
            } else {
                Toast.makeText(this, R.string.storage_permission_needed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void generatePdf() {
        progressDialog.show();

        LcgPdfExport.export(this, visitUuid, LcgPdfExport.defaultSheet(),
                new LcgPdfExport.Callback() {
                    @Override
                    public void onSuccess(@NonNull Uri uri, @NonNull String name) {
                        progressDialog.dismiss();
                        showPdfSavedDialog(uri, name);
                    }

                    @Override
                    public void onFailure(String message) {
                        Timber.tag(TAG).e("LCG export failed: %s", message);
                        progressDialog.dismiss();
                        Toast.makeText(EpartogramViewActivity.this,
                                R.string.epartogram_export_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showPdfSavedDialog(Uri pdfUri, String displayName) {
        ConfirmationDialogFragment dialogFragment = new ConfirmationDialogFragment.Builder(this)
                .title(R.string.pdf_saved_title)
                .content(getString(R.string.pdf_saved_body, displayName))
                .positiveButtonLabel(R.string.action_open)
                .negativeButtonLabel(R.string.ok)
                .build();

        dialogFragment.setListener(new ConfirmationDialogFragment.OnConfirmationActionListener() {
            @Override
            public void onAccept() {
                openPdf(pdfUri);
            }

            @Override
            public void onDecline() {
                ConfirmationDialogFragment.OnConfirmationActionListener.super.onDecline();
            }
        });

        dialogFragment.show(getSupportFragmentManager(),
                dialogFragment.getClass().getCanonicalName());
    }

    private void openPdf(Uri pdfUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.no_pdf_viewer_found, Toast.LENGTH_LONG).show();
        }
    }

    // endregion

    private final WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            isPageLoaded = false;
            setExportActionsEnabled(false);

            timeoutHandler.postDelayed(() -> {
                if (!isPageLoaded) {
                    Log.e(TAG, "Server timeout detected");
                    progressDialog.dismiss();
                    handleError();
                }
            }, 20000); // 20 sec timeout
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mySwipeRefreshLayout.setRefreshing(false);
            Log.d(TAG, "onPageFinished");
            isPageLoaded = true;
            timeoutHandler.removeCallbacksAndMessages(null);

            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (!isErrorShown) {
                setExportActionsEnabled(true);
            }

            if (NetworkConnection.isOnline(EpartogramViewActivity.this)) {
                String fileName = visitUuid + ".mht";
                Timber.tag(TAG).d("fileName => %s", fileName);
                sessionManager.setLCGContentFile(fileName, visitUuid);
                String filePath = webArchiveFileDir + fileName;
                File archive = new File(filePath);
                if (archive.exists()) {
                    if (archive.delete()) view.saveWebArchive(filePath);
                } else view.saveWebArchive(filePath);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.i("WEB_VIEW_TEST", "error code:" + errorCode);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedError(WebView view,
                                    WebResourceRequest request,
                                    WebResourceError error) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (request.isForMainFrame()) {
                    Log.e(TAG, "Main frame error: " + error.getErrorCode());
                    handleError();
                }
            }
        }

        @Override
        public void onReceivedHttpError(WebView view,
                                        WebResourceRequest request,
                                        WebResourceResponse errorResponse) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (request.isForMainFrame()) {
                    Log.e(TAG, "HTTP error: " + errorResponse.getStatusCode());
                    if (errorResponse.getStatusCode() >= 400) {
                        handleError();
                    }
                }
            }
        }

        @Override
        public void onReceivedSslError(WebView view,
                                       SslErrorHandler handler,
                                       SslError error) {
            handler.cancel(); // Important
            handleError();
        }
    };

    private void handleError() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (isErrorShown) return;
        isErrorShown = true;

        setExportActionsEnabled(false);
        webView.setVisibility(View.GONE);
        showPageLoadingErrorDialog();
    }

    private void showPageLoadingErrorDialog() {
        ConfirmationDialogFragment dialogFragment = new ConfirmationDialogFragment.Builder(this)
                .title(R.string.no_internet_title)
                .content(getString(R.string.no_internet_content))
                .positiveButtonLabel(R.string.action_exit)
                .hideNegativeButton(true)
                .build();

        dialogFragment.setListener(new ConfirmationDialogFragment.OnConfirmationActionListener() {
            @Override
            public void onAccept() {
                ConfirmationDialogFragment.OnConfirmationActionListener.super.onDecline();
                finish();
            }

            @Override
            public void onDecline() {
                ConfirmationDialogFragment.OnConfirmationActionListener.super.onDecline();
                finish();
            }
        });

        dialogFragment.show(getSupportFragmentManager(), dialogFragment.getClass().getCanonicalName());
    }

    private void showInternetRequireDialog() {
        ConfirmationDialogFragment dialogFragment = new ConfirmationDialogFragment.Builder(this)
                .title(R.string.no_internet_timeline_screen_title)
                .content(getString(R.string.no_internet_timeline_screen_body))
                .positiveButtonLabel(R.string.ok)
                .hideNegativeButton(true)
                .build();

        dialogFragment.setListener(EpartogramViewActivity.super::onBackNavigate);
        dialogFragment.show(getSupportFragmentManager(), dialogFragment.getClass().getName());
    }

    @Override
    protected int getScreenTitle() {
        return 0;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mySwipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener =
                () -> {
                    if (webView.getScrollY() == 0)
                        mySwipeRefreshLayout.setEnabled(true);
                    else
                        mySwipeRefreshLayout.setEnabled(false);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mySwipeRefreshLayout.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
    }

    @Override
    protected void onDestroy() {
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        if (printJobHandler != null) {
            printJobHandler.removeCallbacksAndMessages(null);
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }
}