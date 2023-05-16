class MainActivity : AppCompatActivity() {

    private lateinit var webView : WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview);
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowContentAccess = true
        webView.settings.allowFileAccess = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        webView.webViewClient = webViewClient
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl("https://medicine.cgmmedical.jp/index.html")

        onBackPressedDispatcher.addCallback(this, object :OnBackPressedCallback(true) {
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
            if (url.startsWith("http:") || url.startsWith("https:")) {
                var urlStr = url
                if (url.endsWith("pdf")) {
                    urlStr = "file:///android_asset/index.html?$url"
                }
                view?.loadUrl(urlStr)
                return false
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            return true
        }
    }
}