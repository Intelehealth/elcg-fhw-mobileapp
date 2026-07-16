package org.intelehealth.ezazi.stage3.postpartum

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.intelehealth.ezazi.BuildConfig
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity
import org.intelehealth.ezazi.utilities.FileUtils
import org.intelehealth.ezazi.utilities.NetworkConnection
import org.intelehealth.ezazi.utilities.SessionManager
import org.intelehealth.ezazi.utilities.WebViewPdfExporter
import org.intelehealth.ezazi.widget.materialprogressbar.CustomProgressDialog
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViewPostPartumReportActivity : BaseActionBarActivity() {

    companion object {
        private const val TAG = "ViewPostPartumReport"

        // Existing LCG URL
         val URL_LCG = BuildConfig.SERVER_URL + "/intelehealth/index.html#/epartogram/"

        // New Postpartum Report URL
         val URL_POSTPARTUM = BuildConfig.SERVER_URL + "/intelehealth/index.html#/dashboard/stage3/"

        // Intent extras
        const val EXTRA_PATIENT_UUID = "patientuuid"

        const val EXTRA_VISIT_UUID = "visituuid"

        const val VIEW_TYPE_LCG = "lcg"
        const val VIEW_TYPE_POSTPARTUM = "postpartum"

        private const val PAGE_TIMEOUT_MS = 20_000L
        private const val REQUEST_STORAGE_PERMISSION = 4322
    }

    private lateinit var webView: WebView
    private lateinit var progressDialog: CustomProgressDialog
    private lateinit var sessionManager: SessionManager
    private lateinit var timeoutHandler: Handler

    private var patientUuid: String = ""
    private var patientName: String = ""

    private var visitUuid: String = ""
    private var viewType: String = VIEW_TYPE_LCG

    private var isPageLoaded = false
    private var isErrorShown = false
    private var webArchiveFileDir: String = ""

    private var optionsMenu: Menu? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_epartogram_ezazi)
        super.onCreate(savedInstanceState)
        setupActionBar()

        timeoutHandler = Handler(Looper.getMainLooper())
        sessionManager = SessionManager(this)
        webArchiveFileDir = FileUtils.getProjectCatchDir(this)

        progressDialog = CustomProgressDialog(this).apply {
            setCancelable(false)
            show()
        }

        readIntent()
        setupWebView()
        loadContent()
    }

    override fun getScreenTitle(): Int = 0

    // region ---- Options menu (Download PDF) ----

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Reuses the ePartogram menu; this screen exposes only the download action
        menuInflater.inflate(R.menu.menu_epartogram, menu)
        optionsMenu = menu
        setDownloadActionEnabled(isPageLoaded && !isErrorShown)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_download_pdf -> {
                downloadReportAsPdf()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Download only makes sense once the report has actually rendered. */
    private fun setDownloadActionEnabled(enabled: Boolean) {
        optionsMenu?.findItem(R.id.action_download_pdf)?.let { item ->
            item.isEnabled = enabled
            item.icon?.alpha = if (enabled) 255 else 100
        }
    }

    // endregion

    // region ---- Download as PDF ----

    private fun downloadReportAsPdf() {
        if (!isPageLoaded || isErrorShown) {
            Toast.makeText(this, R.string.epartogram_not_loaded, Toast.LENGTH_SHORT).show()
            return
        }

        // Pre-Android 10 needs WRITE_EXTERNAL_STORAGE for public Downloads
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
            return
        }

        generatePdf()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                generatePdf()
            } else {
                Toast.makeText(
                    this, R.string.storage_permission_needed, Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun generatePdf() {
        progressDialog.show()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val displayName = "PostpartumReport_${visitUuid}_$timestamp.pdf"

        WebViewPdfExporter.export(this, webView, displayName,
            object : WebViewPdfExporter.Callback {
                override fun onSuccess(uri: Uri, displayName: String) {
                    progressDialog.dismiss()
                    showPdfSavedDialog(uri, displayName)
                }

                override fun onFailure(message: String?) {
                    Timber.tag(TAG).e("PDF export failed: %s", message)
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ViewPostPartumReportActivity,
                        R.string.epartogram_export_failed, Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun showPdfSavedDialog(pdfUri: Uri, displayName: String) {
        ConfirmationDialogFragment.Builder(this)
            .title(R.string.pdf_saved_title)
            .content(getString(R.string.pdf_saved_body, displayName))
            .positiveButtonLabel(R.string.action_open)
            .negativeButtonLabel(R.string.ok)
            .build()
            .apply {
                setListener(object :
                    ConfirmationDialogFragment.OnConfirmationActionListener {
                    override fun onAccept() = openPdf(pdfUri)
                    override fun onDecline() {}
                })
            }
            .show(supportFragmentManager, ConfirmationDialogFragment::class.java.canonicalName)
    }

    private fun openPdf(pdfUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.no_pdf_viewer_found, Toast.LENGTH_LONG).show()
        }
    }

    // endregion

    // Setup

    private fun readIntent() {
        intent?.let {
            patientName = it.getStringExtra("patientName").orEmpty()
            patientUuid = it.getStringExtra(EXTRA_PATIENT_UUID).orEmpty()
            visitUuid = it.getStringExtra(EXTRA_VISIT_UUID).orEmpty()
        }
        Log.v(TAG, "patientUuid=$patientUuid  visitUuid=$visitUuid  viewType=$viewType")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = findViewById(R.id.webview_epartogram)

        webView.apply {
            clearCache(true)
            clearHistory()
            isSaveEnabled = true
            scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
            isScrollbarFadingEnabled = false
            visibility = View.VISIBLE

            settings.apply {
                allowFileAccess = true
                userAgentString = "Android"
                javaScriptEnabled = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                useWideViewPort = true
                defaultTextEncodingName = "UTF-8"
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                domStorageEnabled = true
            }

            webViewClient = buildWebViewClient()
        }
    }

    // Content loading

    private fun loadContent() {
        val finalUrl = "$URL_POSTPARTUM$visitUuid"
        Timber.tag(TAG).d("targetUrl => $finalUrl")

        when {
            NetworkConnection.isOnline(this) -> {
                isPageLoaded = false
                progressDialog.show()

                timeoutHandler.postDelayed({
                    if (!isPageLoaded) {
                        Log.e(TAG, "Manual timeout triggered")
                        webView.stopLoading()
                        progressDialog.dismiss()
                        handleError()
                    }
                }, PAGE_TIMEOUT_MS)

                webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                webView.loadUrl(finalUrl)
                Log.v(TAG, "webviewUrl: $finalUrl")
            }

            sessionManager.getLCGContentFile(visitUuid).isNotEmpty() -> {
                showInternetRequireDialog()
            }

            else -> {
                webView.visibility = View.GONE
                showPageLoadingErrorDialog()
            }
        }
    }

    // WebViewClient
    private fun buildWebViewClient() = object : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            isPageLoaded = false
            setDownloadActionEnabled(false)

            timeoutHandler.postDelayed({
                if (!isPageLoaded) {
                    Log.e(TAG, "Server timeout detected")
                    progressDialog.dismiss()
                    handleError()
                }
            }, PAGE_TIMEOUT_MS)
        }

        override fun onPageFinished(view: WebView, url: String) {
            Log.d(TAG, "onPageFinished")
            isPageLoaded = true
            timeoutHandler.removeCallbacksAndMessages(null)

            if (progressDialog.isShowing) progressDialog.dismiss()

            if (!isErrorShown) setDownloadActionEnabled(true)

            if (NetworkConnection.isOnline(this@ViewPostPartumReportActivity)) {
                val fileName = "$visitUuid.mht"
                Timber.tag(TAG).d("fileName => $fileName")
                sessionManager.setLCGContentFile(fileName, visitUuid)
                val filePath = webArchiveFileDir + fileName
                val archive = File(filePath)
                if (archive.exists()) {
                    if (archive.delete()) view.saveWebArchive(filePath)
                } else {
                    view.saveWebArchive(filePath)
                }
            }
        }

        @Deprecated("Deprecated in API 23")
        override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            Log.i("WEB_VIEW_TEST", "error code: $errorCode")
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame) {
                Log.e(TAG, "Main frame error: ${error.errorCode}")
                handleError()
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            if (request.isForMainFrame) {
                Log.e(TAG, "HTTP error: ${errorResponse.statusCode}")
                if (errorResponse.statusCode >= 400) handleError()
            }
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.cancel()
            handleError()
        }
    }

    // Error handling

    private fun handleError() {
        progressDialog.takeIf { it.isShowing }?.dismiss()
        if (isErrorShown) return
        isErrorShown = true
        setDownloadActionEnabled(false)
        webView.visibility = View.GONE
        showPageLoadingErrorDialog()
    }

    private fun showPageLoadingErrorDialog() {
        ConfirmationDialogFragment.Builder(this)
            .title(R.string.no_internet_title)
            .content(getString(R.string.no_internet_content))
            .positiveButtonLabel(R.string.action_exit)
            .hideNegativeButton(true)
            .build()
            .apply {
                setListener(object : ConfirmationDialogFragment.OnConfirmationActionListener {
                    override fun onAccept() = finish()
                    override fun onDecline() = finish()
                })
            }
            .show(supportFragmentManager, ConfirmationDialogFragment::class.java.canonicalName)
    }

    private fun showInternetRequireDialog() {
        ConfirmationDialogFragment.Builder(this)
            .title(R.string.no_internet_timeline_screen_title)
            .content(getString(R.string.no_internet_timeline_screen_body))
            .positiveButtonLabel(R.string.ok)
            .hideNegativeButton(true)
            .build()
            .apply { setListener { onBackNavigate() } }
            .show(supportFragmentManager, ConfirmationDialogFragment::class.java.name)
    }

    override fun onDestroy() {
        timeoutHandler.removeCallbacksAndMessages(null)
        progressDialog.takeIf { it.isShowing }?.dismiss()
        super.onDestroy()
    }
}