package jp.co.cgm.medical

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progressBar = findViewById(R.id.progress_bar)

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowContentAccess = true
        webView.settings.allowFileAccess = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        webView.webViewClient = webViewClient
        webView.webChromeClient = webChromeClient
        webView.loadUrl("https://www.cgmmedical.jp")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url: String = request?.url.toString()
            Log.d("aaa", "shouldOverrideUrlLoading url : $url")

            if (url.startsWith("weixin://")) {
                return try {
                    val intent = Intent()
                    val cmp = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
                    intent.action = Intent.ACTION_MAIN
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.component = cmp
                    startActivity(intent)
                    true
                } catch (e : Exception) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("提示")
                        .setMessage("未检测到微信客户端，请安装后重试")
                        .setPositiveButton("确定", null)
                        .create().show()
                    false
                }
            }

            if (url.startsWith("alipays://")) {
                return try {
                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                    true
                } catch (e : Exception) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("提示")
                        .setMessage("未检测到支付宝客户端，请安装后重试")
                        .setPositiveButton("确定", null)
                        .create().show()
                    false
                }
            }

            if (url.endsWith(".apk")) {
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.data = Uri.parse(url)
                startActivity(intent)
                return true
            }

            view?.loadUrl(url)
            return true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d("aaa", "onPageFinished url : $url")
            super.onPageFinished(view, url)
            if (url != null) {
                if (url.startsWith("weixin://") || url.startsWith("alipays://")) {
                    return
                }
            }
            val js = "javascript:(function() {document.getElementsByClassName('head_download_hbotitem')[0].style.display='none'})();"
            view?.loadUrl(js)
        }
    }

    private val webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (newProgress == 100) {
                progressBar.visibility = View.GONE
                progressBar.setProgress(0, false)
            } else {
                progressBar.visibility = View.VISIBLE
                progressBar.setProgress(newProgress, true)
            }
        }
    }
}
