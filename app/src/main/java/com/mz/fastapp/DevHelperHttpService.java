package com.mz.fastapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DevHelperHttpService extends Service {
    public DevHelperHttpService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Utils.isDevMode()) {
            throw new RuntimeException("DevHelperHttpService only run in dev mode");
        }
        
        String cmdBase64 = intent.getStringExtra("cmd");
        if (cmdBase64 == null) {
            throw new IllegalArgumentException("cmdBase64 is required");
        }

        String cmd = new String(Base64.decode(cmdBase64, Base64.DEFAULT));
        if (cmd == null || cmd.isEmpty()) {
            throw new IllegalArgumentException("cmd is required");
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    executeCommand(DevHelperHttpService.this, cmd);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        return START_NOT_STICKY;
    }


    public void executeCommand(Context ctx, String cmd) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);

        String filesDir = ctx.getFilesDir().getAbsolutePath();
        String rootfsDir = filesDir + "/system/rootfs";

        if (!new File(rootfsDir).exists()) {
            throw new Exception("rootfs not init");
        }

        String ppoboxFile = rootfsDir + "/usr/bin/ppobox";
        if (!new File(ppoboxFile).exists()) {
            throw new Exception("ppobox not found");
        }
        String nativeLibDir = ctx.getApplicationInfo().nativeLibraryDir;
        processBuilder.environment().put("PROOT_TMP_DIR", rootfsDir + "/tmp");
        processBuilder.environment().put("PROOT_LOADER", nativeLibDir + "/libproot-loader.so");
        processBuilder.environment().put("PROOT_LOADER_32", nativeLibDir + "/libproot-loader32.so");


        String pythonAppDir = ctx.getFilesDir().getAbsolutePath() + "/python_app";

        List<String> cmdParts = new ArrayList<>(){
            {
                add(nativeLibDir + "/libproot.so");
                add("--kill-on-exit");
                add("--link2symlink");
                add("-0");
                add("-r");
                add(rootfsDir);
                add("-w");
                add("/root/python_app");
                add("-b");
                add(pythonAppDir + ":/root/python_app");
            }
        };

        for (String part : cmd.split(" ")) {
            cmdParts.add(part);
        }

        processBuilder.command(
                cmdParts
                // nativeLibDir + "/libproot.so",
                // "--kill-on-exit",
                // "--link2symlink",
                // "-0",
                // "-r", rootfsDir, // Path to the
                // "-w", "/root/python_app",
                // "-b", pythonAppDir + ":/root/python_app",
                // "/usr/bin/ppobox", "file-summary-web", "--port", port, "--dir", "/root/python_app" 
        );

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            Log.d("DevHelperHttpService", line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("process exit with code " + exitCode);
        }
    }
}