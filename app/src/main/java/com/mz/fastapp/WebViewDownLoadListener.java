package com.mz.fastapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.webkit.DownloadListener;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;

import java.util.HashMap;


public class WebViewDownLoadListener implements DownloadListener {

    // 接口类, 用于处理下载请求
    public interface OnDownloadListener {
        void onDownload(String data);
    }

    private OnDownloadListener onDownloadListener;

    public void setOnDownloadListener(OnDownloadListener onDownloadListener) {
        this.onDownloadListener = onDownloadListener;
    }

    Context currentContext;

    public WebViewDownLoadListener(Context context) {
        currentContext = context;
    }


    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        if (onDownloadListener != null) {
            Gson gson = new Gson();
            String data = gson.toJson(new HashMap<String, String>() {{
                put("url", url);
                put("user_agent", userAgent);
                put("content_disposition", contentDisposition);
                put("mimetype", mimetype);
                put("content_length", String.valueOf(contentLength));
            }});
            onDownloadListener.onDownload(data);
            return;
        }

        Uri uri = Uri.parse(url);
        // 其他请求, 弹出确认框, 是否跳转
        AlertDialog.Builder builder = new AlertDialog.Builder(currentContext);
        builder.setTitle(Resources.getSystem().getString(android.R.string.dialog_alert_title));
        builder.setMessage(Utils.getString(R.string.whether_to_jump_to_an_external_browser_for_downloading) + uri.toString());
        builder.setPositiveButton(Resources.getSystem().getString(
                android.R.string.ok
        ), (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            currentContext.startActivity(intent);
        });

        builder.setNegativeButton(Resources.getSystem().getString(
                android.R.string.cancel
        ), (dialog, which) -> {

        });

        builder.show();


    }
}
