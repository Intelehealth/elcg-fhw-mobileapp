package org.intelehealth.ezazi.stage3.postpartum

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.optimized_sync.network.NetworkStatus
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity
import org.intelehealth.ezazi.utilities.SessionManager
import org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding

class OfflineStage3ViewActivity : BaseActionBarActivity() {

    private lateinit var webView: WebView
    private lateinit var tvLastUpdated: TextView

    private lateinit var manager: SessionManager
    private var visitUuid: String? = null
    private var isDataInjected: Boolean = false

    companion object {
        private const val BASE_URL: String = "file:///android_asset"
        private const val STAGE3_FILE_NAME: String = "stage3.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stage3_offline_data)
        manager = SessionManager(this@OfflineStage3ViewActivity)
        enableProperPadding(this@OfflineStage3ViewActivity)

        setupActionBar()
        fetchIntentData()
        initializeViews()
        initializeWebView()
        loadStage3Assets()
    }

    private fun fetchIntentData() {
        visitUuid = intent.getStringExtra("visituuid")
    }

    private fun initializeViews() {
        webView = findViewById(R.id.webview_stage3)
        tvLastUpdated = findViewById(R.id.tv_last_updated_date_time)
        tvLastUpdated.text = getString(R.string.last_updated_on, manager.lastSyncDateTime)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowUniversalAccessFromFileURLs = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (isDataInjected) return
                isDataInjected = true
                view?.let { lifecycleScope.launch { displayData(webView) } }
            }
        }
    }

    private suspend fun displayData(webView: WebView) {
        val json = visitUuid?.let { Stage3DataTransformer.transform(it) } ?: run {
            showPageLoadingErrorDialog()
            return
        }
        val escaped = json.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        webView.evaluateJavascript("window.renderStage3('$escaped')", null)
    }

    private fun showPageLoadingErrorDialog() {
        val dialogFragment = ConfirmationDialogFragment.Builder(this)
            .title(R.string.no_internet_title)
            .content(getString(R.string.no_internet_content))
            .positiveButtonLabel(R.string.action_exit)
            .hideNegativeButton(true)
            .build()

        dialogFragment.setListener(object :
            ConfirmationDialogFragment.OnConfirmationActionListener {
            override fun onAccept() {
                super.onDecline()
                finish()
            }
        })

        dialogFragment.show(
            supportFragmentManager,
            dialogFragment.javaClass.canonicalName
        )
    }

    private fun loadStage3Assets() {
        val stage3Url = "$BASE_URL/$STAGE3_FILE_NAME"
        webView.loadUrl(stage3Url)
    }

    override fun getScreenTitle(): Int = 0

    override fun onNetworkAvailable(status: NetworkStatus) {
        super.onNetworkAvailable(status)
    }

    override fun onNetworkChanged(status: NetworkStatus) {
        super.onNetworkChanged(status)
    }

    override fun onNetworkLost() {
        super.onNetworkLost()
    }
}
