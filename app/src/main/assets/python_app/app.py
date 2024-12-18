
import signal
import sys
import threading
import time
sys.dont_write_bytecode = True
import os
 
import py4a

# try:
#     import cv2
# except ImportError:
#     os.system(
#         "pip install opencv-python-headless -i https://mirrors.aliyun.com/pypi/simple")
#     import cv2

# try:
#     import PIL
# except ImportError:
#     os.system("pip install pillow -i https://mirrors.aliyun.com/pypi/simple")
#     import PIL

try:
    import streamlit as st
except ImportError:
    os.system("pip install streamlit -i https://mirrors.aliyun.com/pypi/simple")
    import streamlit as st

from streamlit.web import cli as stcli


# try:
#     import onnxruntime
# except ImportError:
#     os.system("pip install onnxruntime -i https://mirrors.aliyun.com/pypi/simple")
#     import onnxruntime


# try:
#     # 强制要求numpy==1.19.5
#     import numpy as np
#     if np.__version__ != "1.26.4":
#         raise ImportError
# except ImportError:
#     os.system("pip install numpy==1.26.4 -i https://mirrors.aliyun.com/pypi/simple")


# py4a.install_vulkan()

if __name__ == '__main__':
    port = os.environ.get('PY4A_PORT')
    sys.argv = ["streamlit", "run", os.path.join(
        os.path.dirname(__file__), 'index.py'),
        "--server.port", port,
        "--server.address", "127.0.0.1",
        "--global.developmentMode", "false",
        "--server.runOnSave", "false",
        "--server.allowRunOnSave", "false",
        "--server.enableCORS", "false",
        "--server.enableXsrfProtection", "false",
        "--client.toolbarMode", "minimal",
    ]
    print(sys.argv)

    # 20s后自动退出
    # def exit():
    #     time.sleep(8)
    #     print("exit")
    #     os._exit(0)
    # threading.Thread(target=exit).start()

    def signal_handler(signal, frame):
        print('You pressed Ctrl+C!')
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    sys.exit(stcli.main())
