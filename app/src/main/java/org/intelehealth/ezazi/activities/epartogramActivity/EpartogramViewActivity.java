package org.intelehealth.ezazi.activities.epartogramActivity;

import static org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import com.github.ajalt.timberkt.Timber;

import org.intelehealth.ezazi.BuildConfig;
import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment;
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity;
import org.intelehealth.ezazi.utilities.NetworkConnection;
import org.intelehealth.ezazi.widget.materialprogressbar.CustomProgressDialog;

public class EpartogramViewActivity extends BaseActionBarActivity {

    private static final String TAG = "EpartogramViewActivity";

    private static final String REMOTE_URL_PREFIX = BuildConfig.SERVER_URL + "/intelehealth/index.html#/epartogram/";
    private static final String ASSET_DOMAIN = "appassets.androidplatform.net";
    private static final String OFFLINE_URL_PREFIX = "https://" + ASSET_DOMAIN + "/assets/epartogram/index.html#/epartogram/";

    private static final long PAGE_LOAD_TIMEOUT_MS = 20_000;

    private WebView webView;
    private SwipeRefreshLayout mySwipeRefreshLayout;
    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;
    private CustomProgressDialog progressDialog;

    private String patientUuid;
    private String visitUuid;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private boolean isPageLoaded = false;
    private boolean isErrorShown = false;

    private WebViewAssetLoader assetLoader;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_epartogram_ezazi);
        super.onCreate(savedInstanceState);
        setupActionBar();
        enableProperPadding(this);

        progressDialog = new CustomProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Intent intent = getIntent();
        if (intent != null) {
            patientUuid = intent.getStringExtra("patientuuid");
            visitUuid = intent.getStringExtra("visituuid");
        }
        Log.v(TAG, "patientUuid=" + patientUuid + " visitUuid=" + visitUuid);

        webView = findViewById(R.id.webview_epartogram);
        mySwipeRefreshLayout = findViewById(R.id.swipeContainer);

        configureWebView();
        attachBridge();

        boolean online = NetworkConnection.isOnline(this);
        if (online) {
            loadWithTimeout(REMOTE_URL_PREFIX + visitUuid);
        } else {
            // Offline path always loads the bundled assets — the bridge supplies data from local DB.
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            loadWithTimeout(OFFLINE_URL_PREFIX + visitUuid);
        }

        mySwipeRefreshLayout.setOnRefreshListener(() -> webView.reload());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(ASSET_DOMAIN)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.clearCache(true);
        webView.clearHistory();
        webView.setSaveEnabled(true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setUserAgentString("Android");
        // WebViewAssetLoader serves over a virtual https origin; no need for setAllowFileAccess.

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(webViewClient);
    }

    private void attachBridge() {
        EpartogramBridge bridge = new EpartogramBridge(new EpartogramRepository());
        webView.addJavascriptInterface(bridge, EpartogramBridge.NAME);
    }

    private void loadWithTimeout(String url) {
        isPageLoaded = false;
        Timber.tag(TAG).d("Loading %s", url);
        if (!progressDialog.isShowing()) progressDialog.show();
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.loadUrl(url);
        timeoutHandler.postDelayed(() -> {
            if (!isPageLoaded) {
                Log.e(TAG, "Manual timeout triggered");
                webView.stopLoading();
                if (progressDialog.isShowing()) progressDialog.dismiss();
                handleError();
            }
        }, PAGE_LOAD_TIMEOUT_MS);
    }

    private final WebViewClientCompat webViewClient = new WebViewClientCompat() {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            // Routes https://appassets.androidplatform.net/assets/* to app/src/main/assets/*
            return assetLoader.shouldInterceptRequest(request.getUrl());
        }

        // API < 21 fallback (kept because minSdk could be lowered; harmless on 26+)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return assetLoader.shouldInterceptRequest(Uri.parse(url));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            isPageLoaded = false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mySwipeRefreshLayout.setRefreshing(false);
            isPageLoaded = true;
            timeoutHandler.removeCallbacksAndMessages(null);
            if (progressDialog.isShowing()) progressDialog.dismiss();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, androidx.webkit.WebResourceErrorCompat error) {
            if (request != null && request.isForMainFrame()) {
                Log.e(TAG, "Main frame error: " + error.getErrorCode());
                handleError();
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400) {
                    Log.e(TAG, "HTTP error: " + errorResponse.getStatusCode());
                    handleError();
                }
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            handleError();
        }
    };

    private void handleError() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        if (isErrorShown) return;
        isErrorShown = true;
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

    @Override
    protected int getScreenTitle() {
        return 0;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mySwipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener =
                () -> mySwipeRefreshLayout.setEnabled(webView.getScrollY() == 0));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mySwipeRefreshLayout.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
    }
}
