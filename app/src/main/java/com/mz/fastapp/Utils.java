package com.mz.fastapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class Utils {

    public static void debug(String tag, String msg) {
        if (tag.equals("io")) {
            return;
        }
        Log.d(tag, msg);
        if (tag.length() > 6) {
            GlobalVariable.lastStartupLog += msg + "\n";
        } else {
            GlobalVariable.lastStartupLog += String.format("%-6s", tag) + " > " + msg + "\n";
        }

        if (SplashScreenActivity.instance != null) {
            SplashScreenActivity.instance.runOnUiThread(() -> {
                if (SplashScreenActivity.instance != null) {
                    SplashScreenActivity.instance.syncLog();
                }
            });
        }

        popLoadingSyncLog();
    }

    // 写入到私有内部存储
    public static void writeFile(Context context, String filePath, byte[] buffer) {
        try {
            // 判断文件夹是否存在，不存在则创建
            File file = new File(context.getFilesDir().getAbsolutePath() + File.separator + filePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                Utils.debug("io", "mkdir " + file.getParentFile().getAbsolutePath());
                file.getParentFile().mkdirs();
            }

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 读取私有内部存储文件
    public static byte[] readFile(Context context, String filePath) {
        try {
            String absolutePath = context.getFilesDir().getAbsolutePath();
            File file = new File(absolutePath + File.separator + filePath);
            byte[] buffer = new byte[(int) file.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            fis.read(buffer);
            fis.close();
            return buffer;
        } catch (Exception e) {
            Log.d("readFile", "error: " + e);
            return null;
        }
    }

    // 写入字符串到私有内部存储
    public static void writeFileString(Context context, String filePath, String content) {
        writeFile(context, filePath, content.getBytes());
    }

    // 读取私有内部存储文件为字符串
    public static String readFileString(Context context, String filePath) {
        byte[] buffer = readFile(context, filePath);
        if (buffer == null) {
            return null;
        }
        return new String(buffer);
    }

    // assets所有文件拷贝到私有内部存储
    public static void copyAssets2PrivateDir(Context context, String dirPath) {
        try {
            String[] files = context.getAssets().list(dirPath);

            for (String file : files) {
                String currentPath = dirPath;
                // 末尾不是/则加上/ 且不为空
                if (!currentPath.endsWith(File.separator) && !currentPath.equals("")) {
                    currentPath += File.separator;
                }
                currentPath += file;

                Log.d("copyAssets2PrivateDir", "file: " + currentPath);

                // 如果是目录则递归
                boolean isDir = false;
                try {
                    isDir = context.getAssets().list(currentPath).length > 0;
                } catch (Exception e) {
                    Log.d("copyAssets2PrivateDir", "error: " + e);
                }
                if (isDir) {
                    copyAssets2PrivateDir(context, currentPath);
                } else {
                    try {
                        byte[] buffer = new byte[context.getAssets().open(currentPath).available()];
                        context.getAssets().open(currentPath).read(buffer);
                        writeFile(context, currentPath, buffer);
                    } catch (Exception e) {
                        Log.d("copyAssets2PrivateDir", "error: " + e);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取私有内部存储文件路径
    public static String getPrivateFilePath(Context context, String filePath) {
        return context.getFilesDir().getAbsolutePath() + File.separator + filePath;
    }

    public static int getAvailableTcpPort() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            int localPort = serverSocket.getLocalPort();
            serverSocket.close();
            return localPort;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void showAlert(Activity context, String title, String message, boolean cancelable) {
        context.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setPositiveButton(Resources.getSystem().getString(
                    android.R.string.ok), (dialog, which) -> dialog.dismiss());
            builder.setCancelable(cancelable);
            builder.show();
        });
    }

    public static void runOnUiThread(Runnable runnable) {
        Activity context = GlobalVariable.mainActivityInstance;
        context.runOnUiThread(runnable);
    }

    public static String getString(int id) {
        Activity context = GlobalVariable.mainActivityInstance;
        return context.getResources().getString(id);
    }

    public static void showAlert(Activity context, String message) {
        showAlert(
                context,
                Resources.getSystem().getString(android.R.string.dialog_alert_title),
                message,
                false);
    }

    public static void showAlert(String message) {
        Activity context = GlobalVariable.mainActivityInstance;
        showAlert(
                context,
                Resources.getSystem().getString(android.R.string.dialog_alert_title),
                message,
                false);
    }

    public static void mkdirnexist(String p) {
        // Log.d("mkdirnexist: ", p);
        try {

            File file = new File(p);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createSymbolicLink(String realFilePath, String linkFilePath) {
        mkdirnexist(linkFilePath.substring(0, linkFilePath.lastIndexOf(File.separator)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.createSymbolicLink(Paths.get(linkFilePath), Paths.get(realFilePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Runtime.getRuntime().exec("ln -s " + realFilePath + " " + linkFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void showDialog(final String msg, final Context context,
                                  String[] permissions) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                (dialog, which) -> ActivityCompat.requestPermissions((Activity) context,
                        permissions,
                        123));
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    public static int castToInt(Object obj) {
        if (obj == null) {
            return 0;
        }

        // 如果是Double类型，转换为int
        if (obj instanceof Double) {
            return ((Double) obj).intValue();
        }

        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
        }

        try {
            Float f = Float.parseFloat(obj.toString());
            return f.intValue();
        } catch (Exception e) {
        }

        return 0;
    }

    public static float castToFloat(Object obj) {
        if (obj == null) {
            return 0;
        }

        try {
            return Float.parseFloat(obj.toString());
        } catch (Exception e) {
        }

        return 0;
    }

    public static boolean castToBoolean(Object obj) {
        if (obj == null) {
            return false;
        }

        try {
            return Boolean.parseBoolean(obj.toString());
        } catch (Exception e) {
        }

        try {
            return Integer.parseInt(obj.toString()) != 0;
        } catch (Exception e) {
        }

        return false;
    }

    public static boolean openActivity(Activity context, String activityName) {
        try {
            Class<?> cls = Class.forName(activityName);
            Intent intent = new Intent(context, cls);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void openActivity(String activityName) {
        Activity context = GlobalVariable.mainActivityInstance;
        openActivity(context, activityName);
    }

    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    public static Map<String, Object>[] getCamerasInfo(Activity context) {
        List<Map<String, Object>> cameras = new ArrayList<>();

        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                Map<String, Object> camera = new HashMap<>();
                camera.put("cameraId", cameraId);
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                camera.put("characteristics", characteristics);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        camera.put("facingType", "back");
                    } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        camera.put("facingType", "front");
                    } else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                        camera.put("facingType", "external");
                    }
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] outputSizes = map.getOutputSizes(ImageReader.class);
                List<Map<String, Object>> outputSizesList = new ArrayList<>();
                for (Size size : outputSizes) {
                    Map<String, Object> sizeMap = new HashMap<>();
                    sizeMap.put("width", size.getWidth());
                    sizeMap.put("height", size.getHeight());
                    outputSizesList.add(sizeMap);
                }
                camera.put("outputSizes", outputSizesList);

                cameras.add(camera);
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
        Log.d("getCamerasInfo", "cameras: " + cameras);
        return cameras.toArray(new Map[0]);
    }

    public static void recursionSetPermissions(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    recursionSetPermissions(f);
                }
            }
        }
        file.setExecutable(true, false);
        file.setReadable(true, false);
        file.setWritable(true, false);
    }

    public static void recursionSetPermissions(String path) {
        File file = new File(path);
        recursionSetPermissions(file);
    }

    public static void copyAssets2PrivateDirMultithreading(Context context,
                                                           String dirPath, ExecutorService executorService) throws Exception {
        boolean isRoot = false;
        try {
            if (executorService == null) {
                isRoot = true;
                executorService = Executors.newFixedThreadPool(8);
            }

            String[] files = context.getAssets().list(dirPath);

            for (String file : files) {
                String currentPath = dirPath;

                if (!currentPath.endsWith(File.separator) && !currentPath.equals("")) {
                    currentPath += File.separator;
                }
                currentPath += file;

                // 如果是目录则递归
                boolean isDir = false;
                try {
                    isDir = context.getAssets().list(currentPath).length > 0;
                } catch (Exception e) {
                    Log.d("copyAssets2PrivateDirMultithreading error: ", e.toString());
                }
                String finalCurrentPath = currentPath;
                if (isDir) {
                    copyAssets2PrivateDirMultithreading(context, finalCurrentPath, executorService);
                } else {
                    Future<Integer> future = executorService.submit(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            File outFile = new File(context.getFilesDir(), finalCurrentPath);
                            Utils.debug("io", "copy " + finalCurrentPath + " to "
                                    + outFile.getAbsolutePath());
                            if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                                outFile.getParentFile().mkdirs();
                            }

                            InputStream inputStream = context.getAssets().open(finalCurrentPath);
                            OutputStream outputStream = new FileOutputStream(
                                    new File(context.getFilesDir(), finalCurrentPath));
                            byte[] buffer = new byte[4096];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }
                            inputStream.close();
                            outputStream.close();
                            return 0;
                        }
                    });
                    future.get();
                }
            }
        } catch (Exception e) {
            Log.e("copyAssets2PrivateDirMultithreading error: ", Utils.getStackTraceString(e));
            throw e;
        }
        if (isRoot) {
            // 等待线程池任务完成
            executorService.shutdown();
            try {
                executorService.awaitTermination(999999999, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                Log.e("copyAssets2PrivateDirMultithreading error: ", Utils.getStackTraceString(e));
                throw e;
            }
        }
    }

    @SuppressLint("NewApi")
    public static void tarUnpack(String tarFilePath, String targetDirPath) throws Exception {

        File destFolder = new File(targetDirPath);
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }

        InputStream is = new FileInputStream(tarFilePath);
        TarArchiveInputStream tarIn = new TarArchiveInputStream(is);

        TarArchiveEntry entry;
        while ((entry = tarIn.getNextTarEntry()) != null) {
            String entryName = entry.getName();

            entryName = entryName.replace("./", "");
            File destPath = new File(destFolder, entryName);

            if (entry.isDirectory()) {
                if (!destPath.exists()) {
                    Utils.debug("io", "mkdir " + destPath.getAbsolutePath());
                    destPath.mkdirs();
                }
            } else if (entry.isSymbolicLink()) {
                if (destPath.getParentFile() != null && !destPath.getParentFile().exists()) {
                    Utils.debug("io", "mkdir " + destPath.getParentFile().getAbsolutePath());
                    destPath.getParentFile().mkdirs();
                }
                Path link = Paths.get(destPath.getAbsolutePath());
                Path target = Paths.get(entry.getLinkName());
                try {
                    Files.createSymbolicLink(link, target);
                } catch (FileAlreadyExistsException ignored) {
                    Log.w("tarUnpack: ", "symbolic link already exists: " + link);
                } catch (Exception e) {
                    throw e;
                }
            } else {
                // destPath.getParentFile().mkdirs();
                if (destPath.getParentFile() != null && !destPath.getParentFile().exists()) {
                    Utils.debug("io", "mkdir " + destPath.getParentFile().getAbsolutePath());
                    destPath.getParentFile().mkdirs();
                }

                // 如果已经存在并且大小相同，直接跳过
                if (destPath.exists() && destPath.length() == entry.getSize()) {
                    Utils.debug("io", "exist skip " + destPath.getAbsolutePath());
                    continue;
                }

                // Log.d("tarUnpack: ", "file :" + destPath.getAbsolutePath());
                try {
                    Utils.debug("io", "copy " + entryName + " to " + destPath.getAbsolutePath());
                    OutputStream os = new FileOutputStream(destPath);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = tarIn.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                    os.close();

                } catch (Exception e) {
                    Log.e("tarUnpackMultithreading error: ", Utils.getStackTraceString(e));
                    throw e;
                }
            }
        }

    }

    public static void waitLoadMainUrl(Runnable r) {
        new Thread(() -> {
            for (int i = 0; ; i++) {
                // Log.d("Utils", "Check network connection a number: " + i);
                if (Utils.checkCoreHttp()) {
                    r.run();
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.d("Utils", "wait_load_url: " + e);
                }
            }
        }).start();
    }

    public static boolean checkCoreHttp() {
        String host = "127.0.0.1";
        int port = castToInt(GlobalVariable.PY4A_PORT);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 400);
            return true;
        } catch (IOException e) {
            // Log.d("Utils", "checkTcp: " + e);
        }
        return false;
    }

    public static void unZipFolder(String zipPath, String outPath) {
        Log.d("unZipFolder", "zipPath: " + zipPath);
        if (outPath.endsWith(File.separator)) {
            outPath = outPath.substring(0, outPath.length() - 1);
        }

        // targetPath输出文件路径
        File targetFile = new File(outPath);
        // 如果目录不存在，则创建
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }
        // sourcePath压缩包文件路径
        try (ZipFile zipFile = new ZipFile(new File(zipPath))) {
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                try (BufferedInputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry))) {
                    // 需要判断文件所在的目录是否存在，处理压缩包里面有文件夹的情况
                    String outName = outPath + File.separator + name;
                    File outFile = new File(outName);
                    // 如果已经存在并且大小相同，直接跳过
                    if (outFile.exists() && outFile.length() == entry.getSize()) {
                        continue;
                    }
                    Log.d("unZipFolder", "outName: " + outName);
                    if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                        outFile.getParentFile().mkdirs();
                    }
                    try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outFile))) {
                        int len;
                        byte[] buffer = new byte[1024];
                        while ((len = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, len);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean isDevMode() {
        return Utils.castToBoolean(GlobalVariable.params.get("dev_mode"));
    }

    public static void copyDir(String src, String dest) {
        File srcDir = new File(src);
        File destDir = new File(dest);
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            Utils.debug("copy", "srcDir not exists or not directory: " + src);
            return;
        }
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File[] files = srcDir.listFiles();
        if (files == null) {
            Utils.debug("copy", "srcDir is empty: " + src);
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                copyDir(file.getAbsolutePath(), dest + "/" + file.getName());
            } else {
                copyFile(file.getAbsolutePath(), dest + "/" + file.getName());
            }
        }
    }

    public static void copyFile(String src, String dest) {
        try {
            File srcFile = new File(src);
            File destFile = new File(dest);
            if (destFile.exists()) {
                destFile.delete();
            }
            destFile.createNewFile();
            try (FileInputStream fis = new FileInputStream(srcFile);
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getStackTraceString(Exception e) {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(buff);
        e.printStackTrace(ps);
        return buff.toString();
    }

    public static String bytesToBase64(byte[] bytes) {
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
    }

    public static void setMatrixIdentityM(float[] transform, int i) {
        android.opengl.Matrix.setIdentityM(transform, i);
    }

    public static Bitmap resizeMaxSize(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    public static Bitmap resizeCenterCrop(Bitmap bitmap, int newWidth, int newHeight) {
        Matrix centerCropMatrix = new Matrix();
        // 计算缩放比例
        float scale = Math.max((float) newWidth / bitmap.getWidth(),
                (float) newHeight / bitmap.getHeight());
        float dx = (newWidth - bitmap.getWidth() * scale) / 2;
        float dy = (newHeight - bitmap.getHeight() * scale) / 2;

        centerCropMatrix.postScale(scale, scale);
        centerCropMatrix.postTranslate(dx, dy);
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(bitmap, centerCropMatrix, null);
        return newBitmap;
    }

    public static Bitmap imageToBitmap(ImageProxy image, int rotation, boolean isMirror) {
        Bitmap bitmap = image.toBitmap();
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            if (isMirror) {
                matrix.postScale(-1, 1);
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        }
        return bitmap;
    }

    private static byte[] imagetToNV21(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ImageProxy.PlaneProxy y = planes[0];
        ImageProxy.PlaneProxy u = planes[1];
        ImageProxy.PlaneProxy v = planes[2];
        ByteBuffer yBuffer = y.getBuffer();
        ByteBuffer uBuffer = u.getBuffer();
        ByteBuffer vBuffer = v.getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        return nv21;
    }

    @SuppressLint({"NewApi", "LocalSuppress", "ForegroundServiceType"})
    public static void foregroundServiceNotification(Service context) {
        Map<String, Object> foregroundService = (Map<String, Object>) GlobalVariable.params.get("foreground_service");
        if (foregroundService == null) {
            return;
        }
        boolean enabled = Utils.castToBoolean(foregroundService.get("enabled"));
        if (!enabled) {
            return;
        }
        Map<String, Object> notification = (Map<String, Object>) foregroundService.get("notification");
        if (notification == null) {
            return;
        }
        int id = Utils.castToInt(notification.get("id"));
        String channelId = (String) notification.get("channel_id");
        String channelName = (String) notification.get("channel_name");
        String title = (String) notification.get("title");
        String content = (String) notification.get("content");

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(channelId, channelName,
                NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(context, channelId);
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        Notification n = builder.build();
        context.startForeground(id, n);
    }

    public static void exit() {
        System.exit(0);
    }

    public static void openCameraX(Context context) {
        CameraAnalyzeActivity.open(context);
    }

    public static String[] getStartupCommand(String defaultShellPath) {
        HashMap<String, Object> params = GlobalVariable.params;
        Map<String, Object> system = (Map<String, Object>) params.get("system");
        if (system == null) {
            return new String[]{defaultShellPath, "--login", "-c", "python ./app.py"};
        }
        List<String> startupCommand = (List<String>) system.get("startup_command");
        if (startupCommand == null || startupCommand.size() == 0) {
            return new String[]{defaultShellPath, "--login", "-c", "python ./app.py"};
        }
        return startupCommand.toArray(new String[0]);
    }

    public static String[] concat(String[] a, String[] b) {
        int aLen = a.length;
        int bLen = b.length;
        String[] c = new String[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public static String[] getSystemVolumeMapping() {
        HashMap<String, Object> params = GlobalVariable.params;
        Map<String, Object> system = (Map<String, Object>) params.get("system");
        if (system == null) {
            return new String[]{};
        }
        List<String> volume = (List<String>) system.get("volume");
        return volume.toArray(new String[0]);
    }

    public static HashMap<String, Object> getSystemEnvs() {
        HashMap<String, Object> params = GlobalVariable.params;
        Map<String, Object> system = (Map<String, Object>) params.get("system");
        if (system == null) {
            return new HashMap<>();
        }
        Map<String, Object> envs = (Map<String, Object>) system.get("envs");
        if (envs == null) {
            return new HashMap<>();
        }
        return new HashMap<>(envs);
    }

    public static String[] getSystemDNS() {
        HashMap<String, Object> params = GlobalVariable.params;
        Map<String, Object> system = (Map<String, Object>) params.get("system");
        if (system == null) {
            return new String[]{};
        }
        List<String> dnsList = (List<String>) system.get("dns");
        if (dnsList == null) {
            return new String[]{};
        }
        return dnsList.toArray(new String[0]);
    }

    public static String[] getSystemHosts() {
        HashMap<String, Object> params = GlobalVariable.params;
        Map<String, Object> system = (Map<String, Object>) params.get("system");
        if (system == null) {
            return new String[]{
                    "127.0.0.1 localhost"
            };
        }
        List<String> hostsList = (List<String>) system.get("hosts");
        if (hostsList == null) {
            return new String[]{
                    "127.0.0.1 localhost"
            };
        }

        boolean includeLocalhost = false;
        for (int i = 0; i < hostsList.size(); i++) {
            String host = hostsList.get(i);
            if (host.startsWith("127.0.0.1")) {
                includeLocalhost = true;
                break;
            }
        }

        if (!includeLocalhost) {
            // "127.0.0.1 localhost" 加到最前面
            hostsList.add(0, "127.0.0.1 localhost");
        }

        return hostsList.toArray(new String[0]);
    }

    public static int bytesToInt(byte[] bytes) {
        // LITTLE_ENDIAN
        return ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static byte[] intToBytes(int value) {
        // LITTLE_ENDIAN
        return ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    public static byte[] wrapperReadAllBytes(InputStream is) throws IOException {
        byte[] nByte = new byte[4];
        nByte[0] = (byte) is.read();
        nByte[1] = (byte) is.read();
        nByte[2] = (byte) is.read();
        nByte[3] = (byte) is.read();
        int bufferLength = Utils.bytesToInt(nByte);
        if (bufferLength < 0) {
            throw new IOException("Unexpected end of stream");
        }

        byte[] buffer = new byte[bufferLength];
        int totalBytesRead = 0;
        while (totalBytesRead < bufferLength) {
            int read = is.read(buffer, totalBytesRead, bufferLength - totalBytesRead);
            if (read == -1) {
                throw new IOException("Unexpected end of stream");
            }
            totalBytesRead += read;
        }
        return buffer;
    }

    public static void wrapperWriteAllBytes(OutputStream os, byte[] buffer) throws IOException {
        byte[] nByte = Utils.intToBytes(buffer.length);
        os.write(nByte);
        os.write(buffer);
    }

    @SuppressLint("Range")
    public static Uri getMediaUriFromPath(Context context, String path) {
        if (path.startsWith("content://")) {
            return Uri.parse(path);
        }
        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        if (GlobalVariable.mainActivityInstance == null) {
            Log.e("getMediaUriFromPath", "GlobalVariable.mainContext is null");
        }
        Cursor cursor = context.getContentResolver().query(mediaUri,
                null,
                MediaStore.Images.Media.DISPLAY_NAME + "= ?",
                new String[]{path.substring(path.lastIndexOf("/") + 1)},
                null);

        Uri uri = null;
        if (cursor.moveToFirst()) {
            uri = ContentUris.withAppendedId(mediaUri,
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
        }
        cursor.close();
        return uri;
    }


    /**
     * 创建自定义输出目录
     *
     * @return
     */
    public static String getSandboxPath() {
        File externalFilesDir = GlobalVariable.mainActivityInstance.getExternalFilesDir("");
        File customFile = new File(externalFilesDir.getAbsolutePath(), "Sandbox");
        if (!customFile.exists()) {
            customFile.mkdirs();
        }
        return customFile.getAbsolutePath() + File.separator;
    }


    public static String toJson(Object obj) {
        return new Gson().toJson(obj);
    }

    public static Object fromJson(String json, String className) {
        try {
            Class<?> cls = Class.forName(className);
            return new Gson().fromJson(json, cls);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteDir(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDir(file.getAbsolutePath());
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    public static void checkServiceLife(Runnable r) {
        if (Utils.checkCoreHttp()) {
            return;
        }
        if (GlobalVariable.mainActivityInstance == null) {
            Log.e("checkServiceLife","GlobalVariable.mainContext == null");
            return;
        }
        GlobalVariable.mainActivityInstance.runOnUiThread(() -> {
            GlobalVariable.lastStartupLog = "";
            GlobalVariable.mainActivityInstance.startService(new Intent(GlobalVariable.mainActivityInstance, PythonCoreService.class));
            AlertDialog dialog = popLoading();
            Utils.waitLoadMainUrl(() -> {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (dialog != null){
                    GlobalVariable.mainActivityInstance.runOnUiThread(() -> {
                        r.run();
                        dialog.dismiss();
                    });
                }
            });
        });
    }

    public static void popLoadingSyncLog() {
        if (popLoadingLogTextView == null){
            return;
        }
        GlobalVariable.mainActivityInstance.runOnUiThread(() -> {
            popLoadingLogTextView.setText(GlobalVariable.lastStartupLog);
        });
    }
    static TextView popLoadingLogTextView;
    public static AlertDialog popLoading() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GlobalVariable.mainActivityInstance);
        final AlertDialog[] result = new AlertDialog[1];
        GlobalVariable.mainActivityInstance.runOnUiThread(() -> {
            builder.setTitle("server is reloading...");
            View dialogView = LayoutInflater.from(GlobalVariable.mainActivityInstance).inflate(R.layout.alert_log_view, null, false);
            builder.setView(dialogView);
            builder.setCancelable(false);
            result[0] = builder.show();

            popLoadingLogTextView = dialogView.findViewById(R.id.logview);


            ScrollView scrollView = dialogView.findViewById(R.id.logview_scrollview);

            // 自动滚动到底部
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            });


        });
        return result[0];
    }

}
