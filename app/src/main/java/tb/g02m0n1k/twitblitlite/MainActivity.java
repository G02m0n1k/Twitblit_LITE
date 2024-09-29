package tb.g02m0n1k.twitblitlite;

import static tb.g02m0n1k.twitblitlite.R.drawable.gradient_bar;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar loadingbar;
    private ValueCallback<Uri[]> filePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private static final String TB = "twitblit.ru"; // URL Twitblit
    private static final String TBL = "https://" + TB + "/login"; // URL Twitblit для входа
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

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        loadingbar = findViewById(R.id.loadingbar);
        bg = findViewById(R.id.bg);
        reloadButton = findViewById(R.id.reloadButton);
        tgchat = findViewById(R.id.tgchat);
        srccode = findViewById(R.id.sourcecode);

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
                webView.goBack();
                bg.setVisibility(View.GONE);
            } else {
                assert true;
            }
        });

        // Кнопка перехода в чат Twitblit LITE
        tgchat.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Twitblit_LITE"));
            startActivity(browserIntent);
        });

        // Кнопка перехода на исходный код в GitHub
        srccode.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/G02m0n1k/Twitblit_LITE"));
            startActivity(browserIntent);
        });

        WebViewClient webViewClient = new WebViewClient() {

            // Показатель загрузки
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                loadingbar.setVisibility(ProgressBar.VISIBLE); // Отобразить
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                loadingbar.setVisibility(ProgressBar.GONE); // Скрыть
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
                // Иначе, ссылка не предназначена для страницы на сайте, поэтому запустить другую Activity, которая работает с URL-адресами
                Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                startActivity(intent);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // Отобразить ошибку, если возникла ошибка интернет-соединения
                webView.loadUrl("about:blank");
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