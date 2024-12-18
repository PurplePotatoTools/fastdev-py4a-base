import datetime
import sys
try:
    import streamlit as st
    from streamlit import runtime
    import PIL
    pil_version = PIL.__version__

    st.set_page_config(
        page_title="index",
        page_icon="👋",
    )

    st.write("Python version:", sys.version)
    st.write("Pillow version:", pil_version)
    st.write("Streamlit version:", st.__version__)

    import numpy as np
    st.write("Numpy version:", np.__version__)

    import cv2
    st.write("OpenCV version:", cv2.__version__)

    import onnxruntime
    st.write("onnxruntime version:", onnxruntime.__version__)

    import vulkan
    st.write("vulkan version:", vulkan.__version__)
    from vulkan import VK_API_VERSION, VK_MAKE_VERSION, VkApplicationInfo, vkCreateInstance, VkInstanceCreateInfo, vkEnumeratePhysicalDevices, vkGetPhysicalDeviceProperties, vkEnumerateInstanceExtensionProperties
    appInfo = VkApplicationInfo(pApplicationName='Python VK', applicationVersion=VK_MAKE_VERSION(
        1, 0, 0), pEngineName='pyvulkan', engineVersion=VK_MAKE_VERSION(1, 0, 0), apiVersion=VK_API_VERSION)
    # extenstions = [e.extensionName for e in vkEnumerateInstanceExtensionProperties(None)]
    instanceInfo = VkInstanceCreateInfo(
        pApplicationInfo=appInfo,
        enabledLayerCount=0,
        # enabledExtensionCount=len(extenstions),
        # ppEnabledExtensionNames=extenstions
    )
    instance = vkCreateInstance(instanceInfo, None)
    physicalDevices = vkEnumeratePhysicalDevices(instance)
    st.write(f"Physical devices count: {len(physicalDevices)}")
    for device in physicalDevices:
        properties = vkGetPhysicalDeviceProperties(device)
        st.write(f"Device name: {properties.deviceName}")


    # 当前页面跳转到百度
    st.markdown(
        "<a href='https://www.baidu.com' target='_self'>跳转到百度</a>", unsafe_allow_html=True)
    
    # 打开新页面
    st.markdown(
        "<a href='https://www.baidu.com' target='_blank'>打开新页面百度</a>", unsafe_allow_html=True)  

except Exception:
    st.write("Error")
    raise
