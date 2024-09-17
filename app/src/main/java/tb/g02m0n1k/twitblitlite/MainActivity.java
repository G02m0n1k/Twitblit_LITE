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
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private static final String TB = "twitblit.ru"; // URL Twitblit
    private static final String TBL = "https://" + TB + "/login"; // URL Twitblit для входа
    Button reloadButton;
    TextView errpage;
    View bg;
    TextView textViewLink;

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
        bg = findViewById(R.id.bg);
        errpage = findViewById(R.id.errpage);
        reloadButton = findViewById(R.id.reloadButton);
        textViewLink = findViewById(R.id.sourcecode);

        // Первоначальная загрузка страницы TBL
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(TBL);

        // Кнопка перезапуска страницы
        reloadButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                webView.goBack();
                errpage.setVisibility(View.GONE);
                reloadButton.setVisibility(View.GONE);
                textViewLink.setVisibility(View.GONE);
                bg.setVisibility(View.GONE);
            } else {
                assert true;
            }
        });

        textViewLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/G02m0n1k/Twitblit_LITE"));
            startActivity(browserIntent);
        });

        WebViewClient webViewClient = new WebViewClient() {
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
                errpage.setVisibility(View.VISIBLE);
                reloadButton.setVisibility(View.VISIBLE);
                textViewLink.setVisibility(View.VISIBLE);
            }
        };
        webView.setWebViewClient(webViewClient);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack(); // Возврат к предыдущей странице
        } else {
            super.onBackPressed(); // Закрытие приложения
        }
    }

}