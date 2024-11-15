package jp.co.cgm.medical

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var wbFilePathCallback: ValueCallback<Array<Uri>>? = null

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
        webView.loadUrl("https://www.cgm-medical.jp/")

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
                } catch (e: Exception) {
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
                } catch (e: Exception) {
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
            val js =
                "javascript:(function() {document.getElementsByClassName('head_download_hbotitem')[0].style.display='none'})();"
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

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            wbFilePathCallback = filePathCallback
            if (checkAndRequestPermissions()) {
                showFileChooser()
            }
            return true
        }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val data = it.data?.data
                if (data == null) {
                    val photo: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        it.data?.extras?.getParcelable("data", Bitmap::class.java)
                    } else {
                        it.data?.extras?.get("data") as Bitmap
                    }
                    Thread {
                        val file = File(cacheDir, "dummy.jpg")
                        file.createNewFile()
                        val bos = ByteArrayOutputStream()
                        photo?.compress(Bitmap.CompressFormat.JPEG, 0, bos)
                        val bitmapData = bos.toByteArray()
                        val fos = FileOutputStream(file)
                        fos.write(bitmapData)
                        fos.flush()
                        fos.close()

                        val photoFilePath = Uri.fromFile(file)
                        wbFilePathCallback?.onReceiveValue(arrayOf(photoFilePath))
                    }.start()
                } else {
                    wbFilePathCallback?.onReceiveValue(arrayOf(data))
                }
            } else {
                wbFilePathCallback?.onReceiveValue(null)
            }
        }

    private fun showFileChooser() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "image/*"

        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "")
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            arrayOf(galleryIntent, takePictureIntent)
        )
        launcher.launch(chooserIntent)
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 999)
            return false
        }

        return true
    }

    private fun showManualOpenPermissionsDialog() {
        AlertDialog.Builder(this)
            .setMessage("请前往设定开启相关权限")
            .setPositiveButton("关闭", null)
            .create().show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 999)
            return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (checkAndRequestPermissions()) {
                showFileChooser()
            }
        } else {
            showManualOpenPermissionsDialog()
            wbFilePathCallback?.onReceiveValue(null)
        }
    }
}
