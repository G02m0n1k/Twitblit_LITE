package tb.g02m0n1k.twitblitlite;

import static tb.g02m0n1k.twitblitlite.R.drawable.gradient_bar;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
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
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ValueCallback<Uri[]> filePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private static final String TB = "twitblit.ru"; // URL Twitblit
    private static final String TBF = "https://" + TB; // Полный URL Twitblit
    Button reButton, info;
    View bg;

    @SuppressLint({"SetJavaScriptEnabled", "MissingInflatedId", "ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Окрашивание строки состояния
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        getWindow().setBackgroundDrawableResource(gradient_bar);

        // Показатель загрузки страницы (кружочек в ее верху)
        swipeRefreshLayout = findViewById(R.id.mainwv);
        swipeRefreshLayout.setColorSchemeResources(R.color.white, R.color.tb_pink, R.color.tb_light, R.color.tb_pink);
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.tb_dark);

        // Настойка WebView
        webView = findViewById(R.id.webView);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); // Кэширование
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);  // Антискролл у краев страницы
        webView.getSettings().setJavaScriptEnabled(true);  // Поддержка JS на сайтах
        webView.getSettings().setDomStorageEnabled(true);  // Поддержка LocalStorage
        webView.getSettings().setAllowFileAccess(true);    // Поддержка доступа к файлам
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        bg = findViewById(R.id.bg);
        reButton = findViewById(R.id.reButton);
        info = findViewById(R.id.info);
        // = = = = = = = = = =

        // Скачивание ТОЛЬКО ДЛЯ .ЕРАСК ФАЙЛОВ (в будущем улучшить)
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {

            // Название файла состоят из номера (ID) треда, который получается из URL и пишется в его новое имя
            String penultimateDirectory = "";
            String[] parts = url.split("/");
            if (parts.length > 2) {
                penultimateDirectory = parts[parts.length - 2];
            }

            // Грузим .ЕРАСК файл и пишем об этом пользователю в уведомлении
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            request.setTitle("Thread-" + penultimateDirectory + ".epack");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // Директория загрузки (в папке Downloads создает папку "TwitblitEPACK" и туда сохраняет файлы)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "TwitblitEPACK/Thread-" + penultimateDirectory + ".epack");

            // Менеджер загрузки
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
        });
        // =========================================================

        // Получаем данные о странице из другого источника (intent)
        Intent intent = getIntent();
        String action = intent.getAction();
        String data = intent.getDataString();

        // Первоначальная загрузка страницы в TBF
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            webView.loadUrl(data); // Страница из Intent
        } else {
            webView.loadUrl(TBF);  // Страница "по умолчанию" (https://twitblit.ru/)
        }

        // Кнопка перезагрузки страницы
        reButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {

                String currentUrl = webView.getUrl();

                // Проверяем, если URL при запуске нет(окно ошибки), то грузим страницу входа
                if (currentUrl == null || currentUrl.matches("^" + TB + ".*")) {
                    // Загружаем страницу "по умолчанию"
                    webView.loadUrl(TBF);
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

        // Вызов окна информации
        info.setOnClickListener(v -> About());
        
        // Свайп вниз для обновления страницы
        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        // Установка обработчика JavaScript событий (окно "Поделиться")
        final Context context = this;
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void shareLink(String title, String text, String url) {  // Если shareLink гарит как неактивное, то ничего страшного. Тут вызов с сайта.
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TITLE, title);
                shareIntent.putExtra(Intent.EXTRA_TEXT, text + "\n" + url);
                context.startActivity(Intent.createChooser(shareIntent, getString(R.string.share))); // Метод для вызова окна "Поделиться"
            }
        }, "Android");


        WebViewClient webViewClient = new WebViewClient() {

            // Загрузка страницы
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // info.setVisibility(View.GONE); // Если нужно, то на время загрузки страницы можно скрывать кнопку
                swipeRefreshLayout.setRefreshing(true); // Показывает кружочек
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                info.setVisibility(View.VISIBLE);
                swipeRefreshLayout.setRefreshing(false); // Скрывает кружочек
            }
            // ====================

            // Просто грузит сайт
            @SuppressWarnings("deprecation") @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            // Фильтр который любые ссылки помимо twitblit.ru открывает за пределами приложения (в браузере)
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

            // Отображение ошибки, если пропал интернет
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                String html = "<html><body style='background-color:black;'> </body></html>"; // Черная страница (чтобы не ослепило, когда интернет отвалится)
                webView.loadData(html, "text/html", "UTF-8"); // Отрисовка самой ошибки
                bg.setVisibility(View.VISIBLE);
            }
        };

        // Установка WebChromeClient для обработки выбора файлов
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, getString(R.string.choice)), FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        webView.setWebViewClient(webViewClient);
    }


    // Для выбора файлов
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (filePathCallback != null) {
                Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                if (result != null) {
                    filePathCallback.onReceiveValue(new Uri[]{result}); // Передаем массив с одним элементом
                } else {
                    filePathCallback.onReceiveValue(null); // Если небыло выбора, то возвращаем null
                }
                filePathCallback = null;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Проверка состояния интернета
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

    @SuppressLint("MissingInflatedId")
    private void About() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.about, null);
        builder.setView(dialogView);

        dialogView.findViewById(R.id.icon);
        dialogView.findViewById(R.id.app_name);
        dialogView.findViewById(R.id.inf);

        TextView web1 = dialogView.findViewById(R.id.site);
        TextView web2 = dialogView.findViewById(R.id.tgchat);
        TextView web3 = dialogView.findViewById(R.id.gh);

        // Открытие сайта
        web1.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tbl.catkrakenstudio.ru/"));
            startActivity(browserIntent);
        });

        // Открытие Telegram чата
        web2.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Twitblit_LITE"));
            startActivity(browserIntent);
        });

        // Открытие GitHub
        web3.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/G02m0n1k/Twitblit_LITE"));
            startActivity(browserIntent);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}



// Приложение написано в 2024 году с разрешения CEO Twitblit.ru
// Автор - Daniil (G02m0n1k) Shmotkin
// В текущем виде код одобрен на публикацию ТОЛЬКО для "G02m0n1k" и "CatKrakenStudio"