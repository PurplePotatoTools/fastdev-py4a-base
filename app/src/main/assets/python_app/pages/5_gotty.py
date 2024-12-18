
import streamlit as st
import os
import socket
import py4a


def check_port_in_use(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex((host, port)) == 0


st.title("Gotty")

try:
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(('8.8.8.8', 80))
    ip = s.getsockname()[0]
finally:
    s.close()

st.markdown(
    f"Your Addr: `http://{ip}:31780/` click [here](http://{ip}:31780/) to open the web page")


if not check_port_in_use(ip, 31780):
    run_btn = st.button("Run")
    if run_btn:
        py4a.run_webtty_service(
            host=ip,
            port=31780
        )
        st.write("Done")
