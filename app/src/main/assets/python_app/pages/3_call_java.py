import io
import json
import os
import sys
import threading
import time
import numpy as np 
sys.dont_write_bytecode = True
try:    
    import streamlit as st
    from streamlit.web import cli as stcli
    from streamlit import runtime 
    from jni import JavaClass, JavaProxy

    st.set_page_config(page_title="java", page_icon="🚀")

    # 调用android中的弹出框
    st.write("show-dialog-1")
    st.markdown(
        """
        ```python
        
        AlertDialog = JavaClass("android.app.AlertDialog")
        Builder = JavaClass("android.app.AlertDialog$Builder") 
        GlobalVariable = JavaClass("com.mz.fastapp.GlobalVariable")
        context = GlobalVariable.mainContext

        class uiThreadRunnable:
            def run(self):
                builder = Builder(context)
                builder.setTitle("Hello World")
                builder.setMessage("Hello World")
                builder.setPositiveButton("OK", None)
                builder.setNegativeButton("Cancel", None)
                dialog = builder.create()
                dialog.show()
        
        uiThreadRunnableProxy = JavaProxy("java.lang.Runnable", uiThreadRunnable())
        context.runOnUiThread(uiThreadRunnableProxy) 
        ```
        """)
    
    show_dialog_btn = st.button("show-dialog-1")
    if show_dialog_btn:
        AlertDialog = JavaClass("android.app.AlertDialog")
        Builder = JavaClass("android.app.AlertDialog$Builder") 
        GlobalVariable = JavaClass("com.mz.fastapp.GlobalVariable")
        context = GlobalVariable.mainContext

        class uiThreadRunnable:
            def run(self):
                builder = Builder(context)
                builder.setTitle("Hello World")
                builder.setMessage("Hello World")
                builder.setPositiveButton("OK", None)
                builder.setNegativeButton("Cancel", None)
                dialog = builder.create()
                dialog.show()

        
        uiThreadRunnableProxy = JavaProxy("java.lang.Runnable", uiThreadRunnable())
        context.runOnUiThread(uiThreadRunnableProxy) 

    # 调用android, 扫描附近的蓝牙设备并打印
    st.write("scan-bluetooth")

    st.markdown("""
                
        ```python
        Utils = JavaClass("com.mz.fastapp.Utils")
        BluetoothAdapter = JavaClass("android.bluetooth.BluetoothAdapter")
        BluetoothDevice = JavaClass("android.bluetooth.BluetoothDevice")
        GlobalVariable = JavaClass("com.mz.fastapp.GlobalVariable") 
        context = GlobalVariable.mainContext

        locker = threading.Lock()
        class MyRunnable: 
            devices = None
            def run(self): 
                adapter = BluetoothAdapter.getDefaultAdapter()
                self.devices = adapter.getBondedDevices().toArray() 
                         
                if locker.locked():
                    locker.release()
                print("scan-bluetooth finished")

                
        myRunnable = MyRunnable()

        myRunnableProxy = JavaProxy("java.lang.Runnable", myRunnable)
        locker.acquire()
        context.startAccessPermission(["android.permission.BLUETOOTH_CONNECT","android.permission.BLUETOOTH_SCAN"], myRunnableProxy)
        locker.acquire()
        locker.release()
        
        for device in myRunnable.devices:
            st.write(f"device name: {device.getName()}")
            st.write(f"device address: {device.getAddress()}")
            st.write("------------------")

        ```
""")

    scan_bluetooth_btn = st.button("scan-bluetooth")

    if scan_bluetooth_btn:
        Utils = JavaClass("com.mz.fastapp.Utils")
        BluetoothAdapter = JavaClass("android.bluetooth.BluetoothAdapter")
        BluetoothDevice = JavaClass("android.bluetooth.BluetoothDevice")
        GlobalVariable = JavaClass("com.mz.fastapp.GlobalVariable") 
        context = GlobalVariable.mainContext

        locker = threading.Lock()
        class MyRunnable: 
            devices = None
            def run(self): 
                adapter = BluetoothAdapter.getDefaultAdapter()
                self.devices = adapter.getBondedDevices().toArray() 
                         
                if locker.locked():
                    locker.release()
                print("scan-bluetooth finished")

                
        myRunnable = MyRunnable()

        myRunnableProxy = JavaProxy("java.lang.Runnable", myRunnable)
        locker.acquire()
        context.startAccessPermission(["android.permission.BLUETOOTH_CONNECT","android.permission.BLUETOOTH_SCAN"], myRunnableProxy)
        locker.acquire()
        locker.release()
        
        for device in myRunnable.devices:
            st.write(f"device name: {device.getName()}")
            st.write(f"device address: {device.getAddress()}")
            st.write("------------------")



    # 调用android, 扫描附近的wifi并打印
    st.write("scan-wifi")

    st.markdown("""
        ```python
        Utils = JavaClass("com.mz.fastapp.Utils")
        WifiManager = JavaClass("android.net.wifi.WifiManager")
        WifiInfo = JavaClass("android.net.wifi.WifiInfo")
        GlobalVariable = JavaClass("com.mz.fastapp.GlobalVariable") 
        context = GlobalVariable.mainContext
        wifiManager = context.getSystemService("wifi") 

        locker = threading.Lock()
        class MyBroadcast:
            results = None
            def onReceive(self, context, intent): 
                results = wifiManager.getScanResults().toArray()
                self.results = results
                if locker.locked():
                    locker.release() 

        myBroadcast = MyBroadcast()
        myBroadcastProxy = JavaProxy("android.content.BroadcastReceiver", myBroadcast) 
        IntentFilter = JavaClass("android.content.IntentFilter")
        intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(myBroadcastProxy, intentFilter)
        success = wifiManager.startScan()
        if not success:
            st.write("startScan failed")
        else:
            st.write("startScan started")
            locker.acquire()
            locker.acquire()
            locker.release()

            for result in myBroadcast.results:
                st.write(f"ssid: {result.SSID}")
                st.write(f"bssid: {result.BSSID}")
                st.write(f"level: {result.level}")
                st.write("------------------")
        ```

""")

    scan_wifi_btn = st.button("scan-wifi")

    if scan_wifi_btn:
        Utils = JavaClass("com.mz.fastapp.Utils")
        WifiManager = JavaClass("android.net.wifi.WifiManager")
        WifiInfo = JavaClass("android.net.wifi.WifiInfo")
        GlobalVariable = JavaClass("com.mz.fastapp.GlobalVariable") 
        context = GlobalVariable.mainContext
        wifiManager = context.getSystemService("wifi") 

        locker = threading.Lock()
        class MyBroadcast:
            results = None
            def onReceive(self, context, intent): 
                results = wifiManager.getScanResults().toArray()
                self.results = results
                if locker.locked():
                    locker.release() 

        myBroadcast = MyBroadcast()
        myBroadcastProxy = JavaProxy("android.content.BroadcastReceiver", myBroadcast) 
        IntentFilter = JavaClass("android.content.IntentFilter")
        intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(myBroadcastProxy, intentFilter)
        success = wifiManager.startScan()
        if not success:
            st.write("startScan failed")
        else:
            st.write("startScan started")
            locker.acquire()
            locker.acquire()
            locker.release()

            for result in myBroadcast.results:
                st.write(f"ssid: {result.SSID}")
                st.write(f"bssid: {result.BSSID}")
                st.write(f"level: {result.level}")
                st.write("------------------")



        
except Exception as e:
    st.write("Error: ", e) 