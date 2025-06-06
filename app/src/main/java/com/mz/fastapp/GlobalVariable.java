package com.mz.fastapp;


import android.app.Activity;

import java.io.File;
import java.util.HashMap;

public class GlobalVariable {
    public static String PY4A_PORT = "-1";
    public static String controlSocketPath = null;
    public static String notifySocketPath = null;

    public static String tfliteSocketPath = null;
    
    public static Activity mainActivityInstance;
    
    public static FunctionalViewActivity functionalViewActivityInstance;

    public static HashMap params = new HashMap<>();
    public static String lastStartupLog = "";

    
    public static String errorUrlContentBase64;

    public static File dexmakerCacheDir;


}
