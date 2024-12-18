package com.mz.fastapp;

import android.annotation.SuppressLint;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SocketIPC {
    static boolean isDebug = true;

    public static void LogD(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
    }

    public static void LogE(String tag, String msg) {
        if (isDebug) {
            Log.e(tag, msg);
        }
    }

    LocalServerSocket server;
    LocalSocket localSocket;

    public SocketIPC(String socketPath) {
        try {
            File socketFile = new File(socketPath);
            if (socketFile.getParentFile() != null && !socketFile.getParentFile().exists()) {
                socketFile.getParentFile().mkdirs();
            }
            localSocket = new LocalSocket();
            localSocket.bind(new LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM));
            LogD("SocketIPC", "SocketIPC: " + socketPath);
        } catch (IOException e) {
            LogD("SocketIPC", "SocketIPC: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void release() {
        try {
            LogD("SocketIPC", "release");
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        try {
            this.server = new LocalServerSocket(localSocket.getFileDescriptor());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Thread(() -> {
            for (; ; ) {
                try {
                    LocalSocket receiver = server.accept();
                    receiverStart(receiver);
                    receiver.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }

            LogD("SocketIPC", "accept end");
        }).start();
    }

    public static int BufferSize = 50 * 1024 * 1024;

    public static void receiverStart(LocalSocket receiver) {
        try {
            InputStream is = receiver.getInputStream();
            byte[] buffer = Utils.wrapperReadAllBytes(is);
            byte[] result = Py2Java.execute(buffer);
            OutputStream os = receiver.getOutputStream();
            Utils.wrapperWriteAllBytes(os, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void receiverStart(InputStream is, OutputStream os) throws Exception {
        byte[] buffer = Utils.wrapperReadAllBytes(is);
        byte[] result = Py2Java.execute(buffer);
        Utils.wrapperWriteAllBytes(os, result);
    }

    public static byte[] sendNotifyMessage(byte[] message) {
        try {
            LocalSocket localSocket = new LocalSocket();
            localSocket.connect(
                    new LocalSocketAddress(GlobalVariable.notifySocketPath, LocalSocketAddress.Namespace.FILESYSTEM));

            OutputStream os = localSocket.getOutputStream();
            Utils.wrapperWriteAllBytes(os, message);
            InputStream is = localSocket.getInputStream();
            byte[] data = Utils.wrapperReadAllBytes(is);
            localSocket.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
