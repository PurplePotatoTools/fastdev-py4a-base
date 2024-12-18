package com.mz.fastapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.InputStream;
import java.util.HashMap;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    public static SplashScreenActivity instance = null;

    public TextView logView;

    public void syncLog() {
        if (logView != null) {
            logView.setText(GlobalVariable.lastStartupLog);
        } 
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        GlobalVariable.mainContext = this;
    }

    @Override
    protected void onStart() {
        super.onStart();
        GlobalVariable.mainContext = this;
        syncLog();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        ScrollView scrollView = findViewById(R.id.logview_scrollview);
        
        // 自动滚动到底部
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });

        SplashScreenActivity.instance = this;
        logView = findViewById(R.id.logview);
        initGlobalConfig();
        // 接收type参数
        String startupType = getIntent().hasExtra("startupType") ? getIntent().getStringExtra("startupType") : "start"; 
        Intent pythonCoreServiceIntent = new Intent(this, PythonCoreService.class);
        pythonCoreServiceIntent.putExtra("startupType", startupType);
        startService(pythonCoreServiceIntent);
        if (!Utils.castToBoolean(GlobalVariable.params.get("empty_view"))) {
            Utils.waitLoadMainUrl(() -> {
                this.runOnUiThread(() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });

                SplashScreenActivity.instance = null;
            });
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    public void initGlobalConfig() {
        Gson gson = new Gson();
        // 读取配置文件
        String confPort = Utils.readFileString(this, "config/PY4A_PORT");
        if (confPort == null) {
            int port = Utils.getAvailableTcpPort();
            Log.d("SplashScreenActivity", "getAvailableTcpPort: " + port);
            Utils.writeFileString(this, "config/PY4A_PORT", String.valueOf(port));
            confPort = Utils.readFileString(this, "config/PY4A_PORT");
        }
        Log.d("SplashScreenActivity", "port: " + confPort);
        GlobalVariable.PY4A_PORT = confPort;

        GlobalVariable.mainContext = this;

        // 读取配置文件python_app/app.json 格式化到 GlobalConfig.params
        try {
            InputStream appJsonIs = getAssets().open(
                    "python_app/app.json");
            byte[] buffer = new byte[appJsonIs.available()];
            appJsonIs.read(buffer);
            appJsonIs.close();
            String appJson = new String(buffer, "UTF-8");
            Log.d("SplashScreenActivity", "appJson: " + appJson);
            GlobalVariable.params = gson.fromJson(appJson, HashMap.class);
            Log.d("SplashScreenActivity", "initGlobalConfig: " + GlobalVariable.params);
        } catch (Exception e) {
            Log.e("SplashScreenActivity", "initGlobalConfig: " + e);
        }
    }

    // overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}