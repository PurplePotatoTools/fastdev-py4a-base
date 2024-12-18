
import io
import os
import sys
sys.dont_write_bytecode = True
try:    
    import streamlit as st
    from streamlit.web import cli as stcli
    from streamlit import runtime 
# 测试上传文件
    st.write("upload all")
    file = st.file_uploader("upload file")
    if file:
        st.write("file size:", len(file.getvalue()))
        st.image(file.getvalue())

    st.write("upload jpeg image")
    image = st.file_uploader("upload image", type=["jpg", "jpeg"])
    if image:
        st.write("file size:", len(image.getvalue()))
        st.image(image.getvalue())

    st.write("upload mp4 video")
    video = st.file_uploader("upload video", type=["mp4"])
    if video:
        st.write("file size:", len(video.getvalue()))
        st.video(video.getvalue())

    st.write("upload multiple files")
    files = st.file_uploader("upload files", accept_multiple_files=True)
    if files:
        for file in files:
            st.write("file size:", len(file.getvalue()))
            st.image(file.getvalue())

    
except Exception:
    st.write("Error") 