package com.mz.fastapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mz.fastapp.third_party.AndroidBug5497Workaround;

import java.util.Objects;

public class SubWebViewActivity extends FunctionalViewActivity {

    public WebView webView;
    public CoreWebViewClient coreWebViewClient;

    public int currentDepth;
    public CoreWebChromeViewClient coreWebChromeViewClient;

    String indexUrl;

    @Override
    public void onStart() {
        super.onStart();
        new Thread(() -> {
            Utils.checkServiceLife(() -> {
//                Log.d("SubWebViewActivity", "reloading...");
                if (webView != null) {
                    webView.loadUrl(indexUrl);
//                    Log.d("SubWebViewActivity", "reloaded");
                }
            });
        }).start();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        AndroidBug5497Workaround.assistActivity(this);

        this.currentDepth = getIntent().getIntExtra("currentDepth", 0);
 
        coreWebViewClient = new CoreWebViewClient(this);
        coreWebChromeViewClient = new CoreWebChromeViewClient(this);


        // id webview_core
        webView = findViewById(R.id.coreWebview);
        webView.setWebViewClient(coreWebViewClient);
        webView.setWebChromeClient(coreWebChromeViewClient);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);

        webView.setDownloadListener(new WebViewDownLoadListener(this));

        WebView.setWebContentsDebuggingEnabled(true);

        String url = getIntent().getStringExtra("url");
//        Log.d("SubWebViewActivity", "url: " + url);
        indexUrl = url;

        Activity that = this;
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void openNewWindow(String url) {
                Log.d("openNewWindow",url);
                Intent intent = new Intent(that, SubWebViewActivity.class);
                intent.putExtra("url", url);
                that.startActivity(intent);
            }
        }, "__py4a");
        webView.loadUrl(indexUrl);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }

        GlobalVariable.functionalViewActivityInstance = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (webView != null) {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

}
