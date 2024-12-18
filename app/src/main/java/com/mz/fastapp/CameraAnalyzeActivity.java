package com.mz.fastapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MirrorMode;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.graphics.Matrix;
import android.widget.ImageView;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class CameraRender implements GLSurfaceView.Renderer, Preview.SurfaceProvider {

    private int[] textures = new int[1];
    private float[] mvpMatrix = new float[16];
    private int program;
    private int positionHandle;
    private int textureHandle;
    private int mvpMatrixHandle;

    private final float[] vertices = {
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f
    };

    private final float[] textureCoords = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    private final short[] indices = {0, 1, 2, 0, 2, 3};

    private Canvas canvas;
    private Bitmap transformedBitmap;

    public CameraRender() {
    }

    private Bitmap bitmap;

    public void pushNextFrame(Bitmap mbitmap) {
        this.bitmap = mbitmap;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glGenTextures(1, textures, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        Utils.setMatrixIdentityM(mvpMatrix, 0);

        String vertexShaderCode = "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec2 aTexCoord;" +
                "varying vec2 vTexCoord;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "  vTexCoord = aTexCoord;" +
                "}";

        String fragmentShaderCode = "precision mediump float;" +
                "uniform sampler2D uTexture;" +
                "varying vec2 vTexCoord;" +
                "void main() {" +
                "  gl_FragColor = texture2D(uTexture, vTexCoord);" +
                "}";

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        positionHandle = GLES30.glGetAttribLocation(program, "vPosition");
        textureHandle = GLES30.glGetAttribLocation(program, "aTexCoord");
        mvpMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
    }

    long lastTimestamp = 0;

    @Override
    public void onDrawFrame(GL10 gl) {
        if (bitmap != null) {
            if (CameraAnalyzeActivity.paramsShowFps) {
                if (lastTimestamp > 0) {
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setTextSize(30);
                    Canvas tmpcanvas = new Canvas(Bitmap.createBitmap(
                            bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig()));
                    tmpcanvas.setBitmap(bitmap);
                    tmpcanvas.drawText("fps: " + 1000 / (System.currentTimeMillis() - lastTimestamp), 50, 200, paint);
                }
                lastTimestamp = System.currentTimeMillis();
            }
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
            bitmap = null;
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(program);

        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, createFloatBuffer(vertices));
        GLES30.glEnableVertexAttribArray(positionHandle);

        GLES30.glVertexAttribPointer(textureHandle, 2, GLES30.GL_FLOAT, false, 0, createFloatBuffer(textureCoords));
        GLES30.glEnableVertexAttribArray(textureHandle);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indices.length, GLES30.GL_UNSIGNED_SHORT,
                createShortBuffer(indices));
    }

    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
        // 处理 SurfaceRequest
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    private java.nio.FloatBuffer createFloatBuffer(float[] data) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(data.length * 4);
        bb.order(java.nio.ByteOrder.nativeOrder());
        java.nio.FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    private java.nio.ShortBuffer createShortBuffer(short[] data) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(data.length * 2);
        bb.order(java.nio.ByteOrder.nativeOrder());
        java.nio.ShortBuffer sb = bb.asShortBuffer();
        sb.put(data);
        sb.position(0);
        return sb;
    }

}

public class CameraAnalyzeActivity extends FunctionalViewActivity {

    public static CameraAnalyzeActivity instance = null;

    public PreviewView mainPreviewView;
    public GLSurfaceView glSurfaceView;
    public CameraRender cameraRender;

    private static ImageView coverImageView;

    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;

    public static boolean paramsOnlyPreview = false;
    public static int paramsPreviewViewWidth = -1;
    public static int paramsPreviewViewHeight = -1;
    public static int paramsPreviewViewX = 0;
    public static int paramsPreviewViewY = 0;
    public static int paramsJpegQuality = 95;
    public static int paramsAnalyzeMaxSize = 0;
    public static int paramsLensFacing = CameraSelector.LENS_FACING_FRONT;
    public static boolean paramsShowFps = false;

    public static void open(Context context) {
        if (context == null) {
            context = GlobalVariable.mainContext;
        }
        Intent intent = new Intent(context, CameraAnalyzeActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera_analyze);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        instance = this;

        ViewGroup rootView = findViewById(R.id.main);
        if (paramsPreviewViewWidth == -1 || paramsPreviewViewHeight == -1) {
            paramsPreviewViewWidth = ViewGroup.LayoutParams.MATCH_PARENT;
            paramsPreviewViewHeight = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        if (paramsOnlyPreview) {
            mainPreviewView = new PreviewView(this);
            rootView.addView(mainPreviewView);
            mainPreviewView.getLayoutParams().width = paramsPreviewViewWidth;
            mainPreviewView.getLayoutParams().height = paramsPreviewViewHeight;
            mainPreviewView.setX(paramsPreviewViewX);
            mainPreviewView.setY(paramsPreviewViewY);
        } else {
            glSurfaceView = new GLSurfaceView(this);
            glSurfaceView.setEGLContextClientVersion(3);
            cameraRender = new CameraRender();
            glSurfaceView.setRenderer(cameraRender);
            rootView.addView(glSurfaceView);
            glSurfaceView.getLayoutParams().width = paramsPreviewViewWidth;
            glSurfaceView.getLayoutParams().height = paramsPreviewViewHeight;
            glSurfaceView.setX(paramsPreviewViewX);
            glSurfaceView.setY(paramsPreviewViewY);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        }

        coverImageView = new ImageView(this);
        rootView.addView(coverImageView);
        coverImageView.getLayoutParams().width = paramsPreviewViewWidth;
        coverImageView.getLayoutParams().height = paramsPreviewViewHeight;
        coverImageView.setX(paramsPreviewViewX);
        coverImageView.setY(paramsPreviewViewY);
        // 透明遮盖
        coverImageView.setBackgroundColor(Color.parseColor("#00000000"));

        cameraExecutor = Executors.newSingleThreadExecutor();

        startAccessPermission(
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                },
                () -> {
                    startCamera();
                });

    }

    public static void setCoverImage(Bitmap bitmap) {
        // 设置遮盖图片并按比例缩放
        if (bitmap != null) {
            instance.runOnUiThread(()->{
                coverImageView.setImageBitmap(bitmap);
                coverImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            });
        }
    }

    public static void setCoverImage(byte[] imageBytes) {
        // 设置遮盖图片并按比例缩放 
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        setCoverImage(bitmap);
    }

    public interface ImageAnalyzeCallback {
        byte[] onAnalyze(byte[] imageBytes);
    }

    private static ImageAnalyzeCallback imageAnalyzeCallback = null;

    public static void setImageAnalyzeCallback(ImageAnalyzeCallback callback) {
        imageAnalyzeCallback = callback;
    }

    public interface TakePictureCallback {
        void onPictureTaken(byte[] imageBytes);
        void onError(Exception e);
    }

    public static void takePicture(TakePictureCallback callback) {
        if (callback == null) {
            throw new RuntimeException("TakePictureCallback is not set");
        }
        
        if (instance.imageCapture != null) {
            instance.imageCapture.takePicture(instance.cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    // 处理拍照成功逻辑
                    int rotation = image.getImageInfo().getRotationDegrees();
                    Bitmap bitmap = Utils.imageToBitmap(image, rotation, false);
                    if (bitmap != null) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, paramsJpegQuality, out);
                        byte[] imageBytes = out.toByteArray();
                        callback.onPictureTaken(imageBytes);
                    }
                    image.close();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    // 处理拍照失败逻辑
                    callback.onError(exception);
                }
            });
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image) {
//                        long occurrenceTime = System.currentTimeMillis();
                        // 处理图像分析逻辑
                        int rotation = image.getImageInfo().getRotationDegrees();

                        Bitmap bitmap = Utils.imageToBitmap(image, rotation, paramsLensFacing == CameraSelector.LENS_FACING_FRONT);
                        if (bitmap != null) {
                            if (paramsAnalyzeMaxSize > 0) {
                                bitmap = Utils.resizeMaxSize(bitmap, paramsAnalyzeMaxSize);
                            }
                            if (imageAnalyzeCallback != null) {
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, paramsJpegQuality, out);
                                byte[] imageBytes = out.toByteArray();
                                try {
                                    imageBytes = imageAnalyzeCallback.onAnalyze(imageBytes);
                                } catch (Exception e) {
                                    Log.e("ImageAnalysis", "Exception: " + Utils.getStackTraceString(e));
                                    return;
                                }
                                if (imageBytes != null) {
                                    bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                }
                            }

                            if (cameraRender != null) {

                                bitmap = Utils.resizeCenterCrop(bitmap, glSurfaceView.getWidth(),
                                        glSurfaceView.getHeight());

                                cameraRender.pushNextFrame(bitmap);
                            }

                        }
                        image.close();
                    }

                });

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(paramsLensFacing)
                        .build();

                cameraProvider.unbindAll();

                imageCapture = new ImageCapture.Builder().build();

                if (mainPreviewView != null) {
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(mainPreviewView.getSurfaceProvider());
                    cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, imageCapture, preview);
                } else {
                    cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, imageCapture);
                }

            } catch (ExecutionException | InterruptedException e) {
                // 处理异常

                Log.e("ImageAnalysis", "Exception: " + Utils.getStackTraceString(e));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}