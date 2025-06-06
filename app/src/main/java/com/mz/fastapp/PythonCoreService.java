package com.mz.fastapp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLES20;
import android.os.IBinder;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;


public class PythonCoreService extends Service {

    static {
        System.loadLibrary("fastapp");
    }


    public native void startTFRPCServer(String path);
    public native void logInit();

    int state = 0;

    public PythonCoreService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.foregroundServiceNotification(this);
        this.logInit();
        
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        GlobalVariable.tfliteSocketPath = this.getFilesDir().getAbsolutePath() + "/tflite_socket";
        Log.d("PythonCoreService", "startTFRPCServer: " + GlobalVariable.tfliteSocketPath);
        this.startTFRPCServer(GlobalVariable.tfliteSocketPath);
    }


    static SocketIPC socketIPC;
    static Thread runPythonFileThread;
    static boolean isRunning = false;

    public static void startPythonCoreService(Context ctx) {
        if (PythonCoreService.isRunning) {
            return;
        }
        PythonCoreService.isRunning = true;



        GlobalVariable.controlSocketPath = ctx.getFilesDir().getAbsolutePath() + "/ipc_socket/control";
        if (new File(GlobalVariable.controlSocketPath).exists()) {
            new File(GlobalVariable.controlSocketPath).delete();
        }
        GlobalVariable.notifySocketPath = ctx.getFilesDir().getAbsolutePath() + "/ipc_socket/notify";
        if (new File(GlobalVariable.notifySocketPath).exists()) {
            new File(GlobalVariable.notifySocketPath).delete();
        }
        GlobalVariable.lastStartupLog = "";
        socketIPC = new SocketIPC(GlobalVariable.controlSocketPath);
        socketIPC.listen();
        // 获取type参数
        runPythonFileThread = new Thread(() -> {
            try {
                PythonCoreService.runPythonFile(ctx);
            } catch (Exception e) {
                Utils.debug("runPythonFileThread", Utils.getStackTraceString(e));
            } finally {
                PythonCoreService.isRunning = false;
            }
        });
        runPythonFileThread.start();
    }


    static String startupType = "";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (state == 1) {
            Log.d("PythonCoreService", "PythonCoreService is already running");
            return START_STICKY;
        }

        state = 1;
        startupType = intent.getStringExtra("startupType");

        startPythonCoreService(this);
        return START_STICKY;
    }

    public static void runPythonFile(Context ctx) {
        // 判断config/initialized文件是否存在
        String rootfsInitialized = Utils.readFileString(ctx, "config/rootfsInitialized");
        if (rootfsInitialized == null || !rootfsInitialized.equals("1") || startupType.equals("reinit") || startupType.equals("reinitRootfs")) {
            try {
                // 初始化rootfs
                initRootfs(ctx);
                Utils.writeFileString(ctx, "config/rootfsInitialized", "1");
            } catch (Exception e) {
                Utils.debug("init", Utils.getStackTraceString(e));
            }
        } else {
            Utils.debug("init", "already rootfsInitialized");
        }
        String appInitialized = Utils.readFileString(ctx, "config/appInitialized");
        if (appInitialized == null || !appInitialized.equals("1") || startupType.equals("reinit") || startupType.equals("reinitApp")) {
            try {
                // 初始化app
                initApp(ctx);
                Utils.writeFileString(ctx, "config/appInitialized", "1");
            } catch (Exception e) {
                Utils.debug("init", Utils.getStackTraceString(e));
            }
        } else {
            Utils.debug("init", "already appInitialized");
        }

        try {
            executeCommand(ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public void onDestroy() {
        Log.e("PythonCoreService", "PythonCoreServiceOnDestroyStop");
        runPythonFileThread.interrupt();
        super.onDestroy();
        socketIPC.release();
        state = 0;
    }


    public static void initRootfs(Context ctx) throws Exception {
        Utils.debug("rootfs", "start initializing rootfs");
        // 初始化rootfs

        Utils.debug("rootfs", "copy system files...");
        Utils.copyAssets2PrivateDirMultithreading(ctx, "system", null);
        String rootfsDir = ctx.getFilesDir().getAbsolutePath() + "/system/rootfs";
        String tarPath = ctx.getFilesDir().getAbsolutePath() + "/system/rootfs.tar";
        try {
            if (new File(rootfsDir).exists()) {
                Utils.debug("rootfs", "delete old rootfs...");
                Utils.deleteDir(rootfsDir);
            }
            Utils.debug("rootfs", "start unpacking rootfs...");
            Utils.tarUnpack(tarPath, rootfsDir);
            Utils.debug("rootfs", "unpacking rootfs completed");
        } catch (Exception e) {
            throw e;
        }
        Utils.debug("rootfs", "set permissions...");
        Utils.recursionSetPermissions(rootfsDir);
        Utils.debug("rootfs", "rootfs initialization completed");
    }

    public static void initApp(Context ctx) throws Exception {
        Utils.debug("app", "start initializing app");
        Utils.debug("app", "copy python libs to runnable directory...");
        Utils.copyAssets2PrivateDirMultithreading(ctx, "python_libs", null);
        Utils.debug("app", "copy python libs to runnable directory completed");
        Utils.debug("app", "copy python files to runnable directory...");
        Utils.copyAssets2PrivateDirMultithreading(ctx, "python_app", null);
        Utils.debug("app", "copy python files to runnable directory completed");
    }

    public static void executeCommand(Context ctx) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);

        String filesDir = ctx.getFilesDir().getAbsolutePath();
        String rootfsDir = filesDir + "/system/rootfs";
        String nativeLibDir = ctx.getApplicationInfo().nativeLibraryDir;

        if (!new File(rootfsDir + "/tmp").exists()) {
            new File(rootfsDir + "/tmp").mkdirs();
        }

        // Setup environment for the PRoot process
        processBuilder.environment().put("PROOT_TMP_DIR", rootfsDir + "/tmp");
        processBuilder.environment().put("PROOT_LOADER", nativeLibDir + "/libproot-loader.so");
        processBuilder.environment().put("PROOT_LOADER_32", nativeLibDir + "/libproot-loader32.so");

        processBuilder.environment().put("HOME", "/root");
        processBuilder.environment().put("USER", "root");
        processBuilder.environment().put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin");
        processBuilder.environment().put("TERM", "xterm-256color");
        processBuilder.environment().put("TMPDIR", "/tmp");
        processBuilder.environment().put("SHELL", "/bin/sh");
        processBuilder.environment().put("PY4A_PORT", GlobalVariable.PY4A_PORT);
        processBuilder.environment().put("PYTHONPATH", "/root/python_libs");
        processBuilder.environment().put("PYTHONUNBUFFERED", "1");
        processBuilder.environment().put("JAVA_CONTROL_SOCKET", GlobalVariable.controlSocketPath);
        processBuilder.environment().put("JAVA_NOTIFY_SOCKET", GlobalVariable.notifySocketPath);


//        Log.d("Build.HARDWARE", Build.HARDWARE);
//        processBuilder.environment().put("Build_HARDWARE", Build.HARDWARE);
//        // find qcom
//        if (Build.HARDWARE.contains("qcom")) {
//            processBuilder.environment().put("VK_ICD_FILENAMES", "/usr/local/share/icd.d/freedreno_icd.aarch64.json");
//        }


        HashMap<String, Object> envs = Utils.getSystemEnvs();
        for (String key : envs.keySet()) {
            processBuilder.environment().put(key, envs.get(key).toString());
        }

        String pythonAppDir = ctx.getFilesDir().getAbsolutePath() + "/python_app";
        String pythonLibsDir = ctx.getFilesDir().getAbsolutePath() + "/python_libs";
        String rootfsDirPythonApp = rootfsDir + "/root/python_app";
        if (!new File(rootfsDirPythonApp).exists()) {
            new File(rootfsDirPythonApp).mkdirs();
        }

        String[] dnsArray = Utils.getSystemDNS();
        if (dnsArray != null && dnsArray.length > 0) {
            // Write DNS servers to /etc/resolv.conf
            Utils.debug("dns", "Writing DNS servers to /etc/resolv.conf");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(rootfsDir + "/etc/resolv.conf"))) {
                for (String dns : dnsArray) {
                    writer.write("nameserver " + dns);
                    writer.newLine();
                    Utils.debug("dns", "nameserver " + dns);
                }
            } catch (Exception e) {
                Utils.debug("dns", Utils.getStackTraceString(e));
            } finally {
                Utils.debug("dns", "Writing DNS servers to /etc/resolv.conf completed");
            }
        }

        String[] hostsArray = Utils.getSystemHosts();
        if (hostsArray != null && hostsArray.length > 0) {
            // Write hosts to /etc/hosts
            Utils.debug("hosts", "Writing hosts to /etc/hosts");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(rootfsDir + "/etc/hosts"))) {
                for (String host : hostsArray) {
                    writer.write(host);
                    writer.newLine();
                    Utils.debug("hosts", host);
                }
            } catch (Exception e) {
                Utils.debug("hosts", Utils.getStackTraceString(e));
            } finally {
                Utils.debug("hosts", "Writing hosts to /etc/hosts completed");
            }
        }

        // android system su ln -S
//        String androidSystemLnkDir =  this.getFilesDir().getAbsolutePath() + "/android";
//        if (new File(androidSystemLnkDir).exists()) {
//            new File(androidSystemLnkDir).delete();
//        }
//        new File(androidSystemLnkDir).mkdirs();
//        new ProcessBuilder().command("su", "-c", "ln -s /system " + androidSystemLnkDir + "/system").start().waitFor();


        // Example PRoot command; replace 'libproot.so' and other paths as needed

        String defaultShellPath = "/bin/bash";
        // 优先使用bash,如果不存在,则使用sh
        if (!new File(rootfsDir + defaultShellPath).exists()) {
            defaultShellPath = "/bin/sh";
        }

        String[] systemVolume = Utils.getSystemVolumeMapping();

        String[] prootCommand = {
                nativeLibDir + "/libproot.so", // PRoot binary path
                "--kill-on-exit",
                "--link2symlink",
                "-0",
                "-r", rootfsDir, // Path to the
                "-w", "/root/python_app",
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-b", "/storage",
                "-b", "/data",
                "-b", pythonAppDir + ":/root/python_app",
                "-b", pythonLibsDir + ":/root/python_libs",
        };

        for (String volume : systemVolume) {
            prootCommand = Utils.concat(prootCommand, new String[]{"-b", volume});
        }
        String[] fullCommand = Utils.getStartupCommand(defaultShellPath);
        // add shellPath, "--login", "-c", userCommand
        prootCommand = Utils.concat(prootCommand, fullCommand);

        Utils.debug("run", String.join(" ", fullCommand));
        Utils.debug("run", String.join(" ", prootCommand));

        processBuilder.command(prootCommand);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                Utils.debug("python.stdout", line);
            }
            reader.close();
        } catch (Exception e) {
            Utils.debug("python.stdout", e.getMessage());
            e.printStackTrace();
        }

        // Wait for the process to finish
        int exitValue = process.waitFor();

        // Log the exit value of the process
        Utils.debug("run", "exitValue :" + String.valueOf(exitValue));
        process.destroy();
    }
}