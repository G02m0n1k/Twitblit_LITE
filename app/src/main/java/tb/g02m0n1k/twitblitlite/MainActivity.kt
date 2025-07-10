package tb.g02m0n1k.twitblitlite

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1
    private val TB = "twitblit.ru" // URL Twitblit
    private val TBF = "https://$TB" // Полный URL Twitblit
    private lateinit var reButton: ImageButton
    private lateinit var bg: View
    private val cookieManager = CookieManager.getInstance()

    @SuppressLint("SetJavaScriptEnabled", "MissingInflatedId", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        // Окрашивание строки состояния
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        window.setBackgroundDrawableResource(R.drawable.gradient_bar)

        // Показатель загрузки страницы (кружочек в ее верху)
        swipeRefreshLayout = findViewById(R.id.mainwv)
        swipeRefreshLayout.setColorSchemeResources(R.color.white, R.color.tb_pink, R.color.tb_light, R.color.tbl_blue)
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.tbl_dark)

        // Настойка WebView
        webView = findViewById(R.id.webView)
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT // Кэширование
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER // Антискролл у краев страницы
        webView.settings.javaScriptEnabled = true // Поддержка JS на сайтах
        webView.settings.domStorageEnabled = true // Поддержка LocalStorage
        webView.settings.allowFileAccess = true // Поддержка доступа к файлам
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = packageInfo.versionCode
        val appName = applicationInfo.loadLabel(packageManager).toString()
        val defaultUserAgent = webView.settings.userAgentString
        val customUserAgent = "$defaultUserAgent $appName/$versionName-$versionCode (Android)" // Изменение имени агента
        webView.settings.userAgentString = customUserAgent
        bg = findViewById(R.id.bg)
        reButton = findViewById(R.id.reButton)

        // Включение поддержки cookies
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Включение сохранения cookies между сеансами
        CookieManager.getInstance().flush()

        // Скачивание файлов в оригинальном разрешении
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            try {
                // Генерация имени файла (с защитой от пустых значений)
                var fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                if (fileName.isBlank()) fileName = "file_${System.currentTimeMillis()}"

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "Twitblit LITE")
                if (!appDir.exists()) appDir.mkdirs()

                // Проверяем, не существует ли уже файл
                val targetFile = File(appDir, fileName)
                if (targetFile.exists()) {
                    // Добавляем timestamp к имени
                    val newName = "${fileName.substringBeforeLast(".")}_${System.currentTimeMillis()}.${fileName.substringAfterLast(".", "")}"
                    fileName = newName
                }

                // Настройка запроса на загрузку
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setTitle(">>> ${fileName.take(18)}...") // Обрезка длинных имен
                    setDescription(url)
                    setMimeType(mimeType)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationUri(Uri.fromFile(File(appDir, fileName)))
                    allowScanningByMediaScanner()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setRequiresCharging(false)
                    }
                }

                // Запуск загрузки
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)

            } catch (e: Exception) {
                Log.e("DownloadError", "Ошибка загрузки: ${e.message}")
            }
        }

        // Получаем данные о странице из другого источника (intent)
        val intent = intent
        val action = intent.action
        val data = intent.dataString

        // Первоначальная загрузка страницы в TBF
        if (Intent.ACTION_VIEW == action && data != null) {
            webView.loadUrl(data) // Страница из Intent
        } else {
            webView.loadUrl(TBF) // Страница "по умолчанию" (https://twitblit.ru/)
        }

        // Кнопка перезагрузки страницы
        reButton.setOnClickListener {
            if (isNetworkAvailable()) {
                val currentUrl = webView.url
                if (currentUrl == null || currentUrl.matches("^$TB.*".toRegex())) {
                    webView.goBack()
                    webView.loadUrl(TBF)
                    bg.visibility = View.GONE
                } else {
                    webView.goBack()
                    bg.visibility = View.GONE
                }
            }
        }

        // Свайп вниз для обновления страницы
        swipeRefreshLayout.setOnRefreshListener { webView.reload() }

        // Установка обработчика JavaScript событий (окно "Поделиться")
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun shareLink(title: String, text: String, url: String) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_TEXT, "$text\n$url")
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
            }
        }, "Android")

        val webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                swipeRefreshLayout.isRefreshing = true
            }

            override fun onPageFinished(view: WebView, url: String) {
                loadJsFromGitHub(webView, "https://gitflic.ru/project/g02m0n1k/tbl/blob/raw?file=tbl.js") // ← Вот это заменит твой большой JS-код
                swipeRefreshLayout.isRefreshing = false
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val host = request.url.host
                if (host != null && host.matches("^$TB.*".toRegex())) {
                    return false
                }
                startActivity(Intent(Intent.ACTION_VIEW, request.url))
                return true
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                webView.loadData("<html><body style='background-color:black;'> </body></html>", "text/html", "UTF-8")
                bg.visibility = View.VISIBLE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(Intent.createChooser(intent, getString(R.string.choice)), FILECHOOSER_RESULTCODE)
                return true
            }
        }

        webView.webViewClient = webViewClient
    }

    private fun loadJsFromGitHub(webView: WebView, url: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("WebView", "JS load failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsCode ->
                    webView.post {
                        webView.evaluateJavascript(jsCode, null)
                    }
                }
            }
        })
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            filePathCallback?.onReceiveValue(arrayOf(data?.data ?: return))
            filePathCallback = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}