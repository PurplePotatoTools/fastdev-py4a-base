package com.mz.fastapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.config.SelectModeConfig;
import com.luck.picture.lib.engine.CompressFileEngine;
import com.luck.picture.lib.engine.ImageEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.interfaces.OnKeyValueResultCallbackListener;
import com.luck.picture.lib.interfaces.OnMediaEditInterceptListener;
import com.luck.picture.lib.interfaces.OnResultCallbackListener;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.luck.picture.lib.style.AlbumWindowStyle;
import com.luck.picture.lib.style.BottomNavBarStyle;
import com.luck.picture.lib.style.PictureSelectorStyle;
import com.luck.picture.lib.style.SelectMainStyle;
import com.luck.picture.lib.style.TitleBarStyle;
import com.mz.fastapp.third_party.GlideEngine;
import com.mz.fastapp.third_party.ImageCropEngine;
import com.mz.fastapp.third_party.MeOnMediaEditInterceptListener;
import com.mz.fastapp.third_party.MyCompressFileEngine;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;

import top.zibin.luban.Luban;
import top.zibin.luban.OnNewCompressListener;

public class FunctionalViewActivity extends AppCompatActivity {

    public boolean isWorking = false;

    private Runnable nextRunnable;
    public int resultCode = 0;

    public static int chooseFileMode = SelectMimeType.ofAll();
    public static int selectMode = SelectModeConfig.SINGLE;

    public ArrayList<LocalMedia> chooserFileResult;

    @SuppressLint("Range")
    public Uri getMediaUriFromPath(String path) {
        if (path.startsWith("content://")) {
            return Uri.parse(path);
        }
        return FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", new File(path));
    }


    public void startChooserFile(Runnable runnable) {
        if (this.isWorking) {
            return;
        }
        this.isWorking = true;

        chooserFileResult = null;
        FunctionalViewActivity that = this;
        PictureSelectorStyle selectorStyle = new PictureSelectorStyle();
        SelectMainStyle selectMainStyle = selectorStyle.getSelectMainStyle();
        selectMainStyle.setSelectTextColor(Color.parseColor("#FFFFFF"));
        selectorStyle.setSelectMainStyle(selectMainStyle);

        AlbumWindowStyle albumWindowStyle = new AlbumWindowStyle();
        albumWindowStyle.setAlbumAdapterItemBackground(R.color.very_dark_gray);
        albumWindowStyle.setAlbumAdapterItemTitleColor(Color.parseColor("#FFFFFF"));
        albumWindowStyle.setAlbumAdapterItemSelectStyle(R.color.white);

        BottomNavBarStyle bottomNavBarStyle = selectorStyle.getBottomBarStyle();
        bottomNavBarStyle.setBottomSelectNumResources(R.drawable.num_oval_black_def);
        bottomNavBarStyle.setBottomSelectNumTextColor(Color.parseColor("#FFFFFF"));
        bottomNavBarStyle.setBottomPreviewSelectTextColor(Color.parseColor("#FFFFFF"));
        bottomNavBarStyle.setBottomEditorTextColor(Color.parseColor("#FFFFFF"));
        selectorStyle.setBottomBarStyle(bottomNavBarStyle);


        selectorStyle.setAlbumWindowStyle(albumWindowStyle);
        ImageCropEngine.selectorStyle = selectorStyle;
        PictureSelector.create(this)
                .openGallery(chooseFileMode)
                .setImageEngine(GlideEngine.createGlideEngine())
                .setSelectorUIStyle(selectorStyle)
                .setSelectionMode(selectMode)
//                .setCropEngine(new ImageCropEngine())
                .setCompressEngine(new MyCompressFileEngine())

                .isOriginalControl(true).isDisplayTimeAxis(true).isGif(true).isWebp(true).isBmp(true)
                .forResult(new OnResultCallbackListener<LocalMedia>() {
                    @Override
                    public void onResult(ArrayList<LocalMedia> result) {
                        resultCode = RESULT_OK;
                        chooserFileResult = result;
                        Log.d("startChooserFile", "onResult getPath: " + result.get(0).getPath());
                        Log.d("startChooserFile", "onResult getAvailablePath: " + result.get(0).getAvailablePath());

                        that.isWorking = false;
                        runnable.run();
                    }

                    @Override
                    public void onCancel() {
                        resultCode = RESULT_CANCELED;
                        that.isWorking = false;
                        runnable.run();
                    }
                });

    }

    public int requestCode = 0;
    public Intent data;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.requestCode = requestCode;
        this.resultCode = resultCode;
        this.data = data;
        this.isWorking = false;
        if (this.nextRunnable != null) {
            this.nextRunnable.run();
        }
        this.nextRunnable = null;
        this.data = null;
        this.requestCode = 0;
        this.resultCode = 0;

    }

    public void startAccessPermission(String[] permissions, Runnable runnable) {
        this.isWorking = true;
        this.nextRunnable = runnable;

        for (String permission : permissions) {
            int permissionCheck = ActivityCompat.checkSelfPermission(this, permission);
//            Log.d("startAccessPermission", "permissionCheck: " + permissionCheck);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
//                Log.d("startAccessPermission", "requestPermissions1");
                ActivityCompat.requestPermissions(this, permissions, 0);
//                Log.d("startAccessPermission", "requestPermissions2");
                return;
            }
        }

        this.isWorking = false;
        runnable.run();

    }

    public String[] permissions;
    public int[] grantResults;

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        this.requestCode = requestCode;
        this.permissions = permissions;
        this.grantResults = grantResults;
        this.isWorking = false;
        if (this.nextRunnable != null) {
             this.nextRunnable.run();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}