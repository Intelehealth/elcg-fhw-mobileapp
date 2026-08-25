package org.intelehealth.ezazi.activities.epartogramActivity

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
import org.intelehealth.ezazi.utilities.NepaliDateConverter
import org.intelehealth.ezazi.utilities.SessionManager
import org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding

class OfflineEPartogramViewActivity : BaseActionBarActivity() {

    private lateinit var webView: WebView
    private lateinit var tvLastUpdated: TextView

    private lateinit var manager: SessionManager
    private var visitUuid: String? = null
    private var isDataInjected: Boolean = false

    companion object {
        private const val BASE_URL: String = "file:///android_asset"
        private const val EPARTOGRAM_FILE_NAME: String = "epartogram.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epartogram_offline_data)
        manager = SessionManager(this@OfflineEPartogramViewActivity)
        enableProperPadding(this@OfflineEPartogramViewActivity)

        setupActionBar()
        fetchIntentData()
        initializeViews()
        initializeWebView()
        loadEpartogramAssets()
    }

    private fun fetchIntentData() {
        visitUuid = intent.getStringExtra("visituuid")
    }

    private fun initializeViews() {
        webView = findViewById(R.id.webview_epartogram)
        tvLastUpdated = findViewById(R.id.tv_last_updated_date_time)
        tvLastUpdated.text = getString(
            R.string.last_updated_on,
            NepaliDateConverter.gregStringToBsDisplay(manager.lastSyncDateTime)
        )
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
        val json = visitUuid?.let { EpartogramDataTransformer.transform(it) } ?: run {
            showPageLoadingErrorDialog()
            return
        }
        val escaped = json.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        webView.evaluateJavascript("window.renderPartogram('$escaped')", null)
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

    private fun loadEpartogramAssets() {
        val epartogramUrl = "$BASE_URL/$EPARTOGRAM_FILE_NAME"
        webView.loadUrl(epartogramUrl)
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