package com.mz.fastapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.mz.fastapp.third_party.AndroidBug5497Workaround;

import java.io.InputStream;
import java.util.HashMap;

public class MainActivity extends FunctionalViewActivity {


    public CoreWebViewClient coreWebViewClient;

    public CoreWebChromeViewClient coreWebChromeViewClient;

    public WebView webView;
    String indexUrl;

    @Override
    public void onStart() {
        super.onStart();
        new Thread(() -> {
            Utils.checkServiceLife(() -> {
//                Log.d("MainActivity", "reloading...");
                if (webView != null) {
                    webView.loadUrl(indexUrl);
//                    Log.d("MainActivity", "reloaded");
                }
            });
        }).start();
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        indexUrl = "http://127.0.0.1:" + GlobalVariable.PY4A_PORT + "/";

        Log.d("MainActivity", "onCreate : ");
        EdgeToEdge.enable(this);



        GlobalVariable.mainActivityInstance = this;

        initGlobalConfig();
        boolean emptyView = false;
        try {
            emptyView = Utils.castToBoolean(GlobalVariable.params.get("empty_view"));
        } catch (Exception e) {
            Log.e("MainActivity", "empty_view Error: " + e.getMessage());
        }

        if (!emptyView) {
            startWebMode();
        }

        GlobalVariable.functionalViewActivityInstance = this;
    }


    public void initGlobalConfig() {
        Gson gson = new Gson();
        // 读取配置文件
        String confPort = Utils.readFileString(this, "config/PY4A_PORT");
        if (confPort == null) {
            int port = Utils.getAvailableTcpPort();
            Log.d("MainActivity", "getAvailableTcpPort: " + port);
            Utils.writeFileString(this, "config/PY4A_PORT", String.valueOf(port));
            confPort = Utils.readFileString(this, "config/PY4A_PORT");
        }
        Log.d("MainActivity", "port: " + confPort);
        GlobalVariable.PY4A_PORT = confPort;
        // 读取配置文件python_app/app.json 格式化到 GlobalConfig.params
        try {
            InputStream appJsonIs = getAssets().open(
                    "python_app/app.json");
            byte[] buffer = new byte[appJsonIs.available()];
            appJsonIs.read(buffer);
            appJsonIs.close();
            String appJson = new String(buffer, "UTF-8");
            Log.d("MainActivity", "appJson: " + appJson);
            GlobalVariable.params = gson.fromJson(appJson, HashMap.class);
            Log.d("MainActivity", "initGlobalConfig: " + GlobalVariable.params);

            InputStream f404 = getAssets().open("other_pages/404.html");
            byte[] f404buffer = new byte[f404.available()];
            f404.read(f404buffer);
            f404.close();
            GlobalVariable.errorUrlContentBase64 = Base64.encodeToString(f404buffer, Base64.NO_PADDING);
        } catch (Exception e) {
            Log.e("MainActivity", "initGlobalConfig: " + e);
        }
        GlobalVariable.dexmakerCacheDir = this.getDir("dexmaker", Context.MODE_PRIVATE);

    }


    public void startWebMode() {
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        AndroidBug5497Workaround.assistActivity(this); 

        
        coreWebViewClient = new CoreWebViewClient(this);
        coreWebChromeViewClient = new CoreWebChromeViewClient(this);

        webView = findViewById(R.id.coreWebview);
        webView.setWebViewClient(coreWebViewClient);
        // webView.getSettings().setSupportMultipleWindows(true);
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
        GlobalVariable.mainActivityInstance = this;
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