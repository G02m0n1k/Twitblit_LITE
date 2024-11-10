package tb.g02m0n1k.twitblitlite;

import static tb.g02m0n1k.twitblitlite.R.drawable.gradient_bar;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ValueCallback<Uri[]> filePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private static final String TB = "twitblit.ru"; // URL Twitblit
    private static final String TBL = "https://twitblit.ru/login"; // URL Twitblit с полем входа
    Button tgchat, srccode, reloadButton;
    View bg;

    @SuppressLint({"SetJavaScriptEnabled", "MissingInflatedId", "ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Окрашивание строки состояния в градиент
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        getWindow().setBackgroundDrawableResource(gradient_bar);

        swipeRefreshLayout = findViewById(R.id.mainwv);
        swipeRefreshLayout.setColorSchemeResources(R.color.tb_light, R.color.white);
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.tb_dark);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);  // Поддержка JS на сайтах
        webView.getSettings().setDomStorageEnabled(true);  // Поддержка LocalStorage
        webView.getSettings().setAllowFileAccess(true);    // Поддержка доступа к файлам
        //webView.getSettings().setDatabaseEnabled(true);  // Поддержка БД... на тот случай, если Владислав снова что-то напакостит в работе tb :D
        bg = findViewById(R.id.bg);
        reloadButton = findViewById(R.id.reloadButton);
        tgchat = findViewById(R.id.tgchat);
        srccode = findViewById(R.id.sourcecode);

        // Скачивание файлов (в будущем улучшить)
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String penultimateDirectory = "";
            String[] parts = url.split("/");
            if (parts.length > 2) {
                penultimateDirectory = parts[parts.length - 2];
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            request.setTitle("Thread-" + penultimateDirectory + ".epack");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // Укажите директорию загрузки
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "TwitblitEPACK/Thread-" + penultimateDirectory + ".epack");

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
        });

        // Получаем данные из Intent
        Intent intent = getIntent();
        String action = intent.getAction();
        String data = intent.getDataString();

        // Первоначальная загрузка страницы TBL
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            webView.loadUrl(data);
        } else {
            webView.loadUrl(TBL); // Загружать по умолчанию
        }

        // Кнопка перезапуска страницы
        reloadButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {

                String currentUrl = webView.getUrl();

                // Проверяем, если URL при запуске нет(окно ошибки), то грузим страницу входа
                if (currentUrl == null || currentUrl.matches("^" + TB + ".*")) {
                    // Загружаем страницу входа
                    webView.loadUrl(TBL);
                    bg.setVisibility(View.GONE);
                } else {
                    // Перезагружаем последнюю страницу
                    webView.goBack();
                    bg.setVisibility(View.GONE);
                }
            } else {
                assert true;
            }
        });

        // Кнопка перехода в telegram чат Twitblit LITE
        tgchat.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Twitblit_LITE"));
            startActivity(browserIntent);
        });

        // Кнопка перехода к исходному коду на GitHub
        srccode.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/G02m0n1k/Twitblit_LITE"));
            startActivity(browserIntent);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        WebViewClient webViewClient = new WebViewClient() {

            // Показатель загрузки
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                swipeRefreshLayout.setRefreshing(true); // Скрыть кружочек
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false); // Скрыть кружочек
            }
            //

            @SuppressWarnings("deprecation") @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @SuppressLint("ObsoleteSdkInt")
            @TargetApi(Build.VERSION_CODES.N) @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if (host != null && host.matches("^" + TB + ".*")) {
                    // Этот сайт не переопределять! Пусть WebView загрузит его
                    return false;
                }
                // Если ссылка НЕ предназначена для страницы на сайте, то запустить другую Activity, которая работает с URL-адресами
                Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                startActivity(intent);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // Отобразить ошибку, если возникла ошибка интернет-соединения
                String html = "<html><body style='background-color:black;'> </body></html>";
                webView.loadData(html, "text/html", "UTF-8");
                bg.setVisibility(View.VISIBLE);
            }
        };

        // Устанавливаем WebChromeClient для обработки выбора файлов
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, "Choose a File"), FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        webView.setWebViewClient(webViewClient);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (filePathCallback != null) {
                Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                if (result != null) {
                    filePathCallback.onReceiveValue(new Uri[]{result}); // Передаем массив с одним элементом
                } else {
                    filePathCallback.onReceiveValue(null); // Если не было выбора, возвращаем null
                }
                filePathCallback = null;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Проверка наличия интернета
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Действие при нажатии кнопки "Назад" на панели управления смартфона
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack(); // Возврат к предыдущей странице
        } else {
            super.onBackPressed(); // Закрытие приложения
        }
    }

}