package com.genzopia.addiction;



import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

public class PinInfoActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.noti_tile_info);

        webView = findViewById(R.id.pinInfoWebView);
        progressBar = findViewById(R.id.pinInfoProgress);

        // WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Keep navigation in this WebView
        webView.setWebViewClient(new WebViewClient());

        // Progress updates
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(ProgressBar.GONE);
                }
            }
        });

        // Load local HTML
        webView.loadUrl("file:///android_asset/how_to_pin_tile.html");
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        finish();

    }
}
