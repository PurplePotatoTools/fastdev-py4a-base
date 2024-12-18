from py4a import open_camera
import io
import os
import sys
sys.dont_write_bytecode = True
import time
import PIL
from PIL import Image, ImageFont, ImageDraw
pil_version = PIL.__version__
if pil_version >= "10.0.0":
    def textsize(self, text, font):
        left, top, right, bottom = self.textbbox((0, 0), text, font)
        return right - left, bottom - top
    ImageDraw.ImageDraw.textsize = textsize

def frame(jpegBuff):
    # 全透明层
    newPng = Image.new(
        "RGBA",
        Image.open(io.BytesIO(jpegBuff)).size,
        (0, 0, 0, 0),
    )
    draw = ImageDraw.Draw(newPng)
    font = ImageFont.load_default()
    text = "Hello World"
    textsize = draw.textsize(text, font)
    draw.text((320 - textsize[0] // 2, 240 - textsize[1] // 2),
              text, font=font, fill=(0, 0, 0))
    png_bytes = io.BytesIO()
    newPng.save(png_bytes, format="PNG")
    open_camera().setCoverImage(png_bytes.getvalue())
    return jpegBuff 


try: 
    import streamlit as st
        
    from streamlit.web import cli as stcli
    from streamlit import runtime   


    st.set_page_config(page_title="camera", page_icon="📸")

    # 测试camera
    st.write("open camera")
    showfps = st.checkbox("showfps")
    open_camera().showFps(showfps)

    # 选择摄像头,前置或后置
    lensFacing = st.selectbox("lensFacing", ["back", "front"])
    open_camera().setLensFacing(0 if lensFacing == "front" else 1)

    # 测试帧分析
    st.write("analyze frame")
    analyzeFrame = st.checkbox("analyze frame")
    if analyzeFrame:
        open_camera().onAnalyzed(frame)
    else:
        open_camera().onAnalyzed(None)

    # 帧分析模式, 只预览
    onlyPreview = st.checkbox("only preview")
    open_camera().onlyPreview(onlyPreview)

    # 打开摄像头
    st.write("open camera")
    openCamera = st.button("open camera")
    if openCamera:
        open_camera().open()
except Exception:
    st.write("Error") 