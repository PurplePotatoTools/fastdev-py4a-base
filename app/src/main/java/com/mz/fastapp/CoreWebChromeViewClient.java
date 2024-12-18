package com.mz.fastapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.config.SelectModeConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoreWebChromeViewClient extends WebChromeClient {

    FunctionalViewActivity mainActivity;

    public CoreWebChromeViewClient(FunctionalViewActivity mainActivity) {
        this.mainActivity = mainActivity;
    }


    @Override
    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {
        // WebView.HitTestResult result = view.getHitTestResult();

        // switch (result.getType()) {
        //     case WebView.HitTestResult.SRC_ANCHOR_TYPE:
        //         Message href = view.getHandler().obtainMessage();
        //         Log.d("CoreWebChromeViewClient", "onCreateWindow1: " + href.getData());
        //         view.requestFocusNodeHref(href);
        //         Log.d("CoreWebChromeViewClient", "onCreateWindow2: " + href.getData());
        //         String url = href.getData().getString("url");
        //         Intent intent = new Intent(mainActivity, SubWebViewActivity.class);
        //         intent.putExtra("url", url);
        //         mainActivity.startActivity(intent);
        //         return false;
        //     case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
        //         break;
        //     case WebView.HitTestResult.IMAGE_TYPE:
        //         break;
        //     case WebView.HitTestResult.EDIT_TEXT_TYPE:
        //         break;
        //     case WebView.HitTestResult.PHONE_TYPE:
        //         break;
        //     case WebView.HitTestResult.EMAIL_TYPE:
        //         break;
        //     case WebView.HitTestResult.GEO_TYPE:
        //         break;
        //     case WebView.HitTestResult.UNKNOWN_TYPE:
        //         break;
        // }
        return true;
    }


    @Override
    public void onPermissionRequest(PermissionRequest request) {

        String[] resources = request.getResources();
        for (String resource : resources) {
            if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                this.mainActivity.startAccessPermission(new String[]{
                        "android.permission.CAMERA",
                }, () -> {
                    Log.d("CoreWebChromeViewClient", "onPermissionRequest: " + request.getResources()[0]);
                    request.grant(request.getResources());
                });
            } else if (resource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                this.mainActivity.startAccessPermission(new String[]{
                        "android.permission.RECORD_AUDIO",
                }, () -> {
                    Log.d("CoreWebChromeViewClient", "onPermissionRequest: " + request.getResources()[0]);
                    request.grant(request.getResources());
                });
            }
        }

    }

    // For Android > 5.0
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMsg,
                                     WebChromeClient.FileChooserParams fileChooserParams) {

        // Log.d("CoreWebChromeViewClient", "onShowFileChooser: getAcceptTypes " + new Gson().toJson(fileChooserParams.getAcceptTypes()));
        // Log.d("CoreWebChromeViewClient", "onShowFileChooser: getMode " + fileChooserParams.getMode());
        // Log.d("CoreWebChromeViewClient", "onShowFileChooser: getFilenameHint " + fileChooserParams.getFilenameHint());
        // Log.d("CoreWebChromeViewClient", "onShowFileChooser: getTitle " + fileChooserParams.getTitle());
        // Log.d("CoreWebChromeViewClient", "onShowFileChooser: isCaptureEnabled " + fileChooserParams.isCaptureEnabled());
        String[] acceptTypes = fileChooserParams.getAcceptTypes();
        // 检测前缀 image/ = SelectMimeType.ofImage() 和 video/，= SelectMimeType.ofVideo() 如果都存在则是SelectMimeType.ofAll()

        Map<String, Boolean> mimeTypeMap = new HashMap<>();
        for (String acceptType : acceptTypes) {

            if (acceptType.startsWith("image/") || acceptType.endsWith(".jpg") || acceptType.endsWith(".jpeg") || acceptType.endsWith(".png") || acceptType.endsWith(".gif") || acceptType.endsWith(".bmp") || acceptType.endsWith(".webp")) {
                // image/*,.jpg,.jpeg,.png,.gif,.bmp,.webp
                mimeTypeMap.put("image", true);
            } else if (acceptType.startsWith("video/") || acceptType.endsWith(".3gp") || acceptType.endsWith(".mp4") || acceptType.endsWith(".mkv") || acceptType.endsWith(".avi") || acceptType.endsWith(".flv") || acceptType.endsWith(".wmv") || acceptType.endsWith(".rm") || acceptType.endsWith(".rmvb") || acceptType.endsWith(".mov") || acceptType.endsWith(".ts") || acceptType.endsWith(".m3u8")) {
                // video/*,.3gp,.mp4,.mkv,.avi,.flv,.wmv,.rm,.rmvb,.mov,.ts,.m3u8
                mimeTypeMap.put("video", true);
            }
        }

        FunctionalViewActivity.chooseFileMode = SelectMimeType.ofAll();
        if (mimeTypeMap.containsKey("image") && mimeTypeMap.containsKey("video")) {
            FunctionalViewActivity.chooseFileMode = SelectMimeType.ofAll();
        } else {
            if (mimeTypeMap.containsKey("image")) {
                FunctionalViewActivity.chooseFileMode = SelectMimeType.ofImage();
            }
            if (mimeTypeMap.containsKey("video")) {
                FunctionalViewActivity.chooseFileMode = SelectMimeType.ofVideo();
            }
        }

        if (fileChooserParams.getMode() == 0) {
            FunctionalViewActivity.selectMode = SelectModeConfig.SINGLE;
        } else {
            FunctionalViewActivity.selectMode = SelectModeConfig.MULTIPLE;
        }

        mainActivity.startChooserFile(() -> {
            switch (mainActivity.resultCode) {
                case FunctionalViewActivity.RESULT_CANCELED:
                    uploadMsg.onReceiveValue(null);
                    break;
                case FunctionalViewActivity.RESULT_OK:
                    Log.d("CoreWebChromeViewClient", "onShowFileChooser: " + mainActivity.chooserFileResult);
                    Uri[] uriArray = new Uri[mainActivity.chooserFileResult.size()];
                    for (int i = 0; i < mainActivity.chooserFileResult.size(); i++) {
                        uriArray[i] = mainActivity.getMediaUriFromPath(mainActivity.chooserFileResult.get(i).getAvailablePath());
                        Log.d("CoreWebChromeViewClient", "onShowFileChooser: " + uriArray[i]);
                    }
                    uploadMsg.onReceiveValue(uriArray);
                    break;
                default:
                    uploadMsg.onReceiveValue(null);
                    break;
            }

        });
        return true;
    }

}
