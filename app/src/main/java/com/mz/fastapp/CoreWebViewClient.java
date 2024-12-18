package com.mz.fastapp;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoreWebViewClient extends WebViewClient {
    Activity mainActivity;

    public CoreWebViewClient(Activity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public static HashMap<String, Boolean> noMorePopups = new HashMap<String, Boolean>();

    public boolean isPageFinished;

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

        Uri url = request.getUrl();
        Log.d("shouldOverrideUr lLoading_isPageFinished", String.valueOf(isPageFinished));
        Log.d("shouldOverrideUrlLoading_url", url + " url.getScheme():" + url.getScheme());
        Log.d("shouldOverrideUrlLoading_url", "request.isForMainFrame()" + String.valueOf(request.isForMainFrame()));
        Log.d("shouldOverrideUrlLoading_url", "request.hasGesture()" + String.valueOf(request.hasGesture()));
        Log.d("shouldOverrideUrlLoading_url", "request.isRedirect()" + String.valueOf(request.isRedirect()));

         // 可跳转的所有协议
         String[] schemes = {"http", "https", "file"};
         boolean isJump = false;
         for (String scheme : schemes) {
             if (url.getScheme().equals(scheme)) {
                 isJump = true;
                 break;
             }
         }

         // 判断是否是http或者https请求
         if (isJump) {

             // 判断是否是302跳转
             if (request.isRedirect() || !request.hasGesture()) {
                 view.loadUrl(url.toString());
                 return true;
             }

             if (!isPageFinished || !request.isForMainFrame()) {
                 return true;
             }


             // Intent intent = new Intent(mainActivity, SubWebViewActivity.class);
             // intent.putExtra("url", url.toString());
             // mainActivity.startActivity(intent);
             // return true;


             view.loadUrl(request.getUrl().toString());
             return true;
         } else {
             if (noMorePopups.get(request.getUrl().getScheme()) != null && noMorePopups.get(request.getUrl().getScheme())) {
                 // 不弹出确认框
                 return true;
             }
             AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
             builder.setTitle(Resources.getSystem().getString(android.R.string.dialog_alert_title));
             builder.setMessage(Utils.getString(R.string.whether_to_jump_to_an_external_application) + request.getUrl().getScheme() + " ?");
             AtomicBoolean finalIsJump = new AtomicBoolean(isJump);
             builder.setPositiveButton(Resources.getSystem().getString(
                     android.R.string.ok
             ), (dialog, which) -> {
                 finalIsJump.set(true);
             });

             builder.setNegativeButton(Resources.getSystem().getString(
                     android.R.string.cancel
             ), (dialog, which) -> {
                 finalIsJump.set(false);
             });

             builder.setNeutralButton(
                     Utils.getString(R.string.no_more_popups),
                     (dialog, which) -> {
                         noMorePopups.put(request.getUrl().getScheme(), true);
                     }
             );

             builder.show();

             return !finalIsJump.get();
         }


    }

    @Override
    public void onPageFinished(WebView view, String url) {
        isPageFinished = true;
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP
                    && view.canGoBack()) {
                view.goBack();
                Log.d("CoreWebViewClient", "canGoBack");
                return true;
            }
            return false;
        });



        

        // 对所有a标签, 判断是否target="_blank" 并且是网址, 如果是, 则调用window.open方法打开新窗口, 否则, 按原来的方式打开
        view.evaluateJavascript(
                "(function() { " +
                    "window.addEventListener('click', function(e) {" +
                        "var target = e.target;" + 
                        // "console.log('target innerHtml: ' + target.innerHtml);" +
                        // "console.log('target tagName: ' + target.tagName);" +
                        // "console.log('target target: ' + target.target);" +
                        // "console.log('target href: ' + target.href);" +
                        // "console.log('target html: ' + target.outerHTML);" +
                        "if (e.target.tagName === 'A' && e.target.target === '_blank' && e.target.href && (e.target.href.indexOf('http://') === 0 || e.target.href.indexOf('https://') === 0)) {" +
                            "e.preventDefault();" +
                            "window.open(e.target.href);" +
                        "}else{" +
                            "var parent = e.target.parentElement;" +
                            "while(parent){" +
                                "if(parent.tagName === 'A' && parent.target === '_blank' && parent.href && (parent.href.indexOf('http://') === 0 || parent.href.indexOf('https://') === 0)){" +
                                    "e.preventDefault();" +
                                    "window.open(parent.href);" +
                                    "break;" +
                                "}" +
                                "parent = parent.parentElement;" +
                            "}" +
                        "}" +
                    "});" +
                    "window.open = function(url) {" +
                        "__py4a.openNewWindow(url);" +
                    "};" +
                "})();",
                null
        );


        view.evaluateJavascript(
                "(function() { return window.getComputedStyle(document.body, null).getPropertyValue('background-color'); })();",
                color -> {
                    try {
                        // 设置MainActivity R.layout.main的背景颜色
                        int colorInt = parseColor(color);
                        mainActivity.findViewById(R.id.main).setBackgroundColor(colorInt);
                        // 修改状态栏字体颜色，用AndroidX官方兼容API
                        WindowInsetsControllerCompat wic = ViewCompat.getWindowInsetsController(mainActivity.getWindow().getDecorView());
                        if (wic != null) {
                            // 判断颜色深浅，浅色背景显示黑色字体，深色背景显示白色字体
                            if (Color.red(colorInt) * 0.299 + Color.green(colorInt) * 0.587 + Color.blue(colorInt) * 0.114 > 186) {
                                wic.setAppearanceLightStatusBars(true);
                            } else {
                                wic.setAppearanceLightStatusBars(false);
                            }
                        }
                    } catch (Exception e) {
//                        Utils.showAlert("Status bar color parsing failed: " + e.getMessage());
                        Log.e("CoreWebViewClient", "Status bar color parsing failed: " + e.getMessage());
                    }
                });
    }

    public int parseColor(String color) {
        // 适配 rgb(255, 255, 255) rgba(255, 255, 255, 0.5)

        // 1. 去掉空格
        color = color.replaceAll(" ", "");
        color = color.replaceAll("\"", "");
        // 2. 转换为小写
        color = color.toLowerCase();
        if (color.startsWith("rgba")) {
            // 3. 去掉rgba( )
            color = color.replace("rgba(", "").replace(")", "");

            // 4. 逗号分隔
            String[] colors = color.split(",");
            // 透明度不为1时, 默认背景色为白色
            if (Float.parseFloat(colors[3]) != 1) {
                color = "#" + String.format("%02x", 255 - Integer.parseInt(colors[0]) * Integer.parseInt(colors[3]))
                        + String.format("%02x", 255 - Integer.parseInt(colors[1]) * Integer.parseInt(colors[3]))
                        + String.format("%02x", 255 - Integer.parseInt(colors[2]) * Integer.parseInt(colors[3]));
            } else {
                // 5. 转换为16进制, 占位符 %02x
                color = "#" + String.format("%02x", Integer.parseInt(colors[0]))
                        + String.format("%02x", Integer.parseInt(colors[1]))
                        + String.format("%02x", Integer.parseInt(colors[2]));
            }
            // showAlert("ss:" + color);
        } else if (color.startsWith("rgb")) {
            // 3. 去掉rgb(和)
            color = color.substring(4, color.length() - 1);
            // 4. 逗号分隔
            String[] colors = color.split(",");
            // 5. 转换为16进制
            color = "#" + String.format("%02x", Integer.parseInt(colors[0]))
                    + String.format("%02x", Integer.parseInt(colors[1]))
                    + String.format("%02x", Integer.parseInt(colors[2]));
        }

        return Color.parseColor(color); // 返回颜色值
    }


    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                    WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);
        if (request.isForMainFrame()) {
            String errorUrl = "file:///android_asset/other_pages/404.html";
            // 这个方法在6.0才出现
            int statusCode = errorResponse.getStatusCode();
            Log.d("CoreWebViewClient", "onReceivedHttpError code = " + statusCode);
//            view.loadUrl("about:blank");
            view.loadData(GlobalVariable.errorUrlContentBase64, "text/html", "base64");

        }

    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Log.d("CoreWebViewClient", "onReceivedError code = " + errorCode);
//        view.loadUrl("about:blank");
        view.loadData(GlobalVariable.errorUrlContentBase64, "text/html", "base64");
    }
}
