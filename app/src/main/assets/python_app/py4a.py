import json
import shutil
import socket
import time
import os
import subprocess
import sys
import threading


from jni import JavaClass, JavaProxy


def check_preinstalled():
    preinstalled_dir = "./whl_preinstalled"
    locker_file = os.path.join(
        os.path.dirname(__file__),
        "installed.lock",
    )

    if os.path.exists(preinstalled_dir):
        whl_files = os.listdir(preinstalled_dir)
        # sort by name
        whl_files.sort()

        installed_files = []
        if os.path.exists(locker_file):
            with open(locker_file, "r") as f:
                installed_files = f.read().split("\n")

        if installed_files == whl_files:
            print("preinstalled whl already installed")
            return

        abs_whl_files = [os.path.join(preinstalled_dir, f) for f in whl_files]
        abs_installed_files = [os.path.join(
            preinstalled_dir, f) for f in installed_files]
        for whl_file in abs_whl_files:
            if whl_file in abs_installed_files:
                print(f"skip {whl_file}")
                continue
            subprocess.run(["pip", "install", "--no-deps", whl_file])

        with open(locker_file, "w") as f:
            f.write("\n".join(whl_files))

        print("install preinstalled whl success")


def get_lan_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip


def install_vulkan():
    # /usr/lib/aarch64-linux-gnu/libxcb-keysyms.so.1
    if not os.path.exists("/usr/lib/aarch64-linux-gnu/libxcb-keysyms.so.1"):
        # debian12-bookworm
        os.system(
            'sed -i "s@http://\(deb\|security\).debian.org@https://mirrors.aliyun.com@g" /etc/apt/sources.list.d/debian.sources')
        # debian11-bullseye
        os.system(
            'sed -i "s@http://\(deb\|security\).debian.org@https://mirrors.aliyun.com@g" /etc/apt/sources.list')
        os.system("apt-get update")
        os.system("apt-get install libxcb-keysyms1 -y")

    # /usr/lib/aarch64-linux-gnu/libxcb-keysyms.so.1
    if not os.path.exists("/usr/lib/aarch64-linux-gnu/libxcb-keysyms.so.1"):
        os.system("apt-get install libxcb-keysyms1 -y")

    # /usr/lib/aarch64-linux-gnu/libvulkan.so.1
    if not os.path.exists("/usr/lib/aarch64-linux-gnu/libvulkan.so.1"):
        os.system("apt-get install vulkan-tools -y")

    # libxcb-xfixes.so.0
    if not os.path.exists("/usr/lib/aarch64-linux-gnu/libxcb-xfixes.so.0"):
        os.system("apt-get install libxcb-xfixes0 -y")

    # libxcb-shm.so.0
    if not os.path.exists("/usr/lib/aarch64-linux-gnu/libxcb-shm.so.0"):
        os.system("apt-get install libxcb-shm0 -y")

    try:
        import vulkan
    except ImportError:
        os.system("pip install vulkan -i https://mirrors.aliyun.com/pypi/simple")
    except:
        os.system("apt-get install vulkan-tools -y")
        os.system("apt-get install libxcb-keysyms1 -y")

    # cp /usr/local/share/icd.d/*.json /usr/share/vulkan/icd.d/
    if os.path.exists("/usr/share/vulkan/icd.d"):
        config_files = os.listdir("/usr/local/share/icd.d")
        for config_file in config_files:
            if config_file.endswith(".json"):
                shutil.copyfile(
                    f"/usr/local/share/icd.d/{config_file}",
                    f"/usr/share/vulkan/icd.d/{config_file}"
                )


def is_port_in_use(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex(("127.0.0.1", port)) == 0


def run_ssh_service(username="root", password="root", port=22, host=get_lan_ip()):
    if is_port_in_use(port):
        print(f"port {port} already in use")
        return

    os.system(
        f"ppobox easy-sshd --user {username} --password {password} --port {port} --host {host}")


def run_webtty_service(host=get_lan_ip(), port=31780):
    if is_port_in_use(port):
        print(f"port {port} already in use")
        return

    os.system(
        f"ppobox gotty --address {host} --port {port} --permit-write --reconnect /bin/bash --login")


# Calling Java from Python

def on_download_listener(func):
    that = func

    class OnDownloadListener:
        def onDownload(self, data):
            return that(json.loads(data))
    onDownloadListener = JavaProxy(
        "com.mz.fastapp.DownloadActivity$OnDownloadListener", OnDownloadListener())
    JavaClass("com.mz.fastapp.DownloadActivity").setOnDownloadListener(
        onDownloadListener)


def open_camera():
    global instanceCamera
    if instanceCamera is None:
        instanceCamera = CameraImpl()
    return instanceCamera


class CameraImpl:

    def __init__(self):

        global instanceCamera
        self.CameraAnalyzeActivity = JavaClass(
            "com.mz.fastapp.CameraAnalyzeActivity")
        if instanceCamera is None:
            instanceCamera = self

    def __open(self):
        self.CameraAnalyzeActivity.open(None)

    def open(self):
        threading.Thread(target=self.__open).start()
        time.sleep(0.5)
        return self

    def showFps(self, b=True):
        self.CameraAnalyzeActivity.paramsShowFps = b
        return self

    def setAnalyzeMaxSize(self, size):
        self.CameraAnalyzeActivity.paramsAnalyzeMaxSize = size
        return self

    def setPosition(self, x, y, w, h):
        self.CameraAnalyzeActivity.paramsPreviewViewWidth = w
        self.CameraAnalyzeActivity.paramsPreviewViewHeight = h
        self.CameraAnalyzeActivity.paramsPreviewViewX = x
        self.CameraAnalyzeActivity.paramsPreviewViewY = y
        return self

    def setWidth(self, w):
        self.CameraAnalyzeActivity.paramsPreviewViewWidth = w
        return self

    def setHeight(self, h):
        self.CameraAnalyzeActivity.paramsPreviewViewHeight = h
        return self

    def setX(self, x):
        self.CameraAnalyzeActivity.paramsPreviewViewX = x
        return self

    def setY(self, y):
        self.CameraAnalyzeActivity.paramsPreviewViewY = y
        return self

    def setJpegQuality(self, quality):
        self.CameraAnalyzeActivity.paramsJpegQuality = quality
        return self

    def setLensFacing(self, facing):
        self.CameraAnalyzeActivity.paramsLensFacing = facing
        return self

    def onlyPreview(self, c=True):
        self.CameraAnalyzeActivity.paramsOnlyPreview = c
        return self

    analyze_func = None

    def onAnalyzed(self, func):
        if self.analyze_func is None:
            self.analyze_func = func
            that = self

            class ImageAnalyzeCallback:
                def onAnalyze(self, buff):
                    return that.analyze_func(buff)
            imageAnalyzeCallback = JavaProxy(
                "com.mz.fastapp.CameraAnalyzeActivity$ImageAnalyzeCallback", ImageAnalyzeCallback())
            self.CameraAnalyzeActivity.setImageAnalyzeCallback(
                imageAnalyzeCallback)
        else:
            self.analyze_func = func
        return self

    def finish(self):
        self.CameraAnalyzeActivity.instance.finish()
        return self

    def takePicture(self):
        class TakePictureCallback:
            finished = False

            def onPictureTaken(self, buff):
                self.buffer = buff
                self.finished = True

            def onError(self, error):
                self.error = error
                self.finished = True

        tp_callback = TakePictureCallback()
        takePictureCallback = JavaProxy(
            "com.mz.fastapp.CameraAnalyzeActivity$TakePictureCallback", tp_callback)
        self.CameraAnalyzeActivity.takePicture(takePictureCallback)

        for _ in range(10):
            if tp_callback.finished:
                break
            time.sleep(1)
        if not tp_callback.finished:
            raise Exception("takePicture timeout")

        if hasattr(tp_callback, "error"):
            raise Exception(tp_callback.error)
        return tp_callback.buffer

    def setCoverImage(self, pngBuff):
        self.CameraAnalyzeActivity.setCoverImage(pngBuff)
        return self


instanceCamera = None
