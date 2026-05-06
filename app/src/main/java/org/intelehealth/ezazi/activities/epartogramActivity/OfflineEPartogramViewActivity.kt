package org.intelehealth.ezazi.activities.epartogramActivity

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.optimized_sync.network.NetworkStatus
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity
import org.intelehealth.ezazi.utilities.SessionManager
import org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding

class OfflineEPartogramViewActivity : BaseActionBarActivity() {

    private lateinit var webView: WebView
    private lateinit var tvLastUpdated: TextView

    private lateinit var manager: SessionManager

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

        initializeViews()
        initializeWebView()
        loadEpartogramAssets()
    }

    private fun initializeViews() {
        webView = findViewById(R.id.webview_epartogram)
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
                view?.let { webView.evaluateJavascript("window.renderPartogram('{}')", null) }
            }
        }
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