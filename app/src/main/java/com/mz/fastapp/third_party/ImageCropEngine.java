package com.mz.fastapp.third_party;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.engine.CropEngine;
import com.luck.picture.lib.engine.CropFileEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.style.PictureSelectorStyle;
import com.luck.picture.lib.style.SelectMainStyle;
import com.luck.picture.lib.style.TitleBarStyle;
import com.luck.picture.lib.utils.DateUtils;
import com.luck.picture.lib.utils.StyleUtils;
import com.mz.fastapp.R;
import com.mz.fastapp.Utils;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropImageEngine;

import java.io.File;
import java.util.ArrayList;


public class ImageCropEngine implements CropFileEngine {

    @Override
    public void onStartCrop(Fragment fragment, Uri srcUri, Uri destinationUri, ArrayList<String> dataSource, int requestCode) {
        UCrop.Options options = buildOptions();
        UCrop uCrop = UCrop.of(srcUri, destinationUri, dataSource);
        uCrop.withOptions(options);
        uCrop.setImageEngine(new UCropImageEngine() {
            @Override
            public void loadImage(Context context, String url, ImageView imageView) {
                if (context == null) {
                    return;
                }
                Glide.with(context).load(url).into(imageView);
            }

            @Override
            public void loadImage(Context context, Uri url, int maxWidth, int maxHeight, OnCallbackListener<Bitmap> call) {
                Glide.with(context).asBitmap().load(url).override(maxWidth, maxHeight).into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (call != null) {
                            call.onCall(resource);
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        if (call != null) {
                            call.onCall(null);
                        }
                    }
                });
            }
        });
        uCrop.start(fragment.requireActivity(), fragment, requestCode);
    }


    public static PictureSelectorStyle selectorStyle;

    /**
     * 配制UCrop，可根据需求自我扩展
     *
     * @return
     */
    private UCrop.Options buildOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setHideBottomControls(true);
        options.setFreeStyleCropEnabled(true);
        options.setShowCropFrame(true);
        options.setShowCropGrid(true);
//        options.setCircleDimmedLayer(true);
        options.setCropOutputPathDir(Utils.getSandboxPath());
        options.isCropDragSmoothToCenter(false);
        options.setSkipCropMimeType("video", "audio");
        options.isForbidCropGifWebp(true);
        options.isForbidSkipMultipleCrop(true);
        options.setMaxScaleMultiplier(100);
        SelectMainStyle mainStyle = selectorStyle.getSelectMainStyle();
        boolean isDarkStatusBarBlack = mainStyle.isDarkStatusBarBlack();
        options.isDarkStatusBarBlack(isDarkStatusBarBlack);

        options.setStatusBarColor(Color.parseColor("#242424"));
        options.setToolbarColor(Color.parseColor("#242424"));
        options.setToolbarWidgetColor(Color.parseColor("#FFFFFF"));
        return options;
    }
}