package com.mz.fastapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class SurfaceHolderActivity extends FunctionalViewActivity {

    private Surface surfaceViewLayout = null;
    public static SurfaceHolderActivity instance = null;

    public static String cameraId = "0";
    public static int outputWidth = 1920;
    public static int outputHeight = 1080;
    public ImageReader imageReader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_surface_holder);
        instance = this;
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        // 根据outputWidth和outputHeight设置SurfaceView的宽高
        surfaceView.getLayoutParams().width = outputWidth;
        surfaceView.getLayoutParams().height = outputHeight;


        SurfaceHolder holder = surfaceView.getHolder();


        imageReader = ImageReader.newInstance(outputWidth, outputHeight, ImageFormat.JPEG, 1);

        Log.d("SurfaceHolderActivity", "all savedInstanceState = " + savedInstanceState);

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surfaceViewLayout = holder.getSurface();
                startAccessPermission(
                        new String[]{Manifest.permission.CAMERA},
                        new Runnable() {
                            @Override
                            public void run() {
                                Log.d("SurfaceHolderActivity", "startAccessPermission");
                                initView();
                            }
                        }
                );
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                // 获取图
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

    CameraDevice myCameraDevice;
    CameraCaptureSession captureSession;


    public static void setOnImageAvailableListener(ImageReader.OnImageAvailableListener listener) {
//        instance.runOnUiThread(() -> {
//             instance.imageReader.setOnImageAvailableListener(listener, null);
//        });
    }

    @SuppressLint("MissingPermission")
    public void initView() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.d("SurfaceHolderActivity", "initView");
        Log.d("SurfaceHolderActivity", "cameraManager = " + cameraManager);
        try {
            //准备拍照捕获请求的目标
            Surface surfaceImageReader = imageReader.getSurface();

            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //获取最新的一帧图像
                    //Image image = reader.acquireLatestImage();
                    //处理图像
                    //image.close();
                    Log.d("SurfaceHolderActivity", "onImageAvailable");
                }
            }, null);


            CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    Log.d("SurfaceHolderActivity", "onOpened");
                    //相机设备打开
                    myCameraDevice = cameraDevice;
                    //捕获请求建造者
                    CaptureRequest.Builder captureRequestBuilder_preview;
                    try {
                        // (2) 创建捕获请求建设者，参数：模板_预览
                        captureRequestBuilder_preview = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        //等待SurfaceView创建完成并回调后将它的Surface设为目标
                        while (true) {
                            if (surfaceViewLayout != null) {
                                //添加目标
                                captureRequestBuilder_preview.addTarget(surfaceViewLayout);
                                captureRequestBuilder_preview.addTarget(surfaceImageReader);
                                break;
                            }
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            // (3) 创建目标合集
                            List<Surface> list = new ArrayList();
                            list.add(surfaceImageReader);
                            list.add(surfaceViewLayout);
                            // (4) 创建捕获通道 - 捕获通道需要在相机启动后再创建
                            cameraDevice.createCaptureSession(list, new CameraCaptureSession.StateCallback() {
                                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                    //捕获通道配置完成
                                    captureSession = cameraCaptureSession;
                                    // (5) 配置完成，可以开始预览 - 设置循环请求
                                    try {
                                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder_preview.build(), null, null);
                                    } catch (CameraAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                    //配置失败
                                    Log.d("SurfaceHolderActivity", "onConfigureFailed" + cameraCaptureSession);
                                }
                            }, null);
                        } catch (CameraAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }


                }

                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Log.d("SurfaceHolderActivity", "onDisconnected");
                    //相机设备断开
                    if (myCameraDevice != null) {
                        myCameraDevice.close();
                        myCameraDevice = null;
                    }
                }

                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Log.d("SurfaceHolderActivity", "onError");
                    //相机设备错误
                    if (myCameraDevice != null) {
                        myCameraDevice.close();
                        myCameraDevice = null;
                    }
                }

                public void onClosed(@NonNull CameraDevice camera) {
                    Log.d("SurfaceHolderActivity", "onClosed");
                    //相机设备关闭
                }
            };


            cameraManager.openCamera(cameraId, stateCallback, null);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


}