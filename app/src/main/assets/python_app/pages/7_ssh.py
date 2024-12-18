 
import streamlit as st
import py4a
import os  
import socket
  
def check_port_in_use(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex((host, port)) == 0


st.title("ssh")

lan_ip = py4a.get_lan_ip()

if not check_port_in_use(lan_ip, 31722):
    run_btn = st.button("Run")
    if run_btn:
        py4a.run_ssh_service(
            port=31722
        )
        st.write("Done")
