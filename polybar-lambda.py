#!/bin/env python3

import socket
from psutil import process_iter
from signal import SIGTERM


s = socket.socket()
port = 46721

try:
  s.bind(("127.0.0.1", port))
  s.listen(1)
except OSError:
  print("port waiting...")

# https://stackoverflow.com/questions/20691258/is-it-possible-in-python-to-kill-process-that-is-listening-on-specific-port-for
  for proc in process_iter():
      for conns in proc.connections(kind='inet'):
          if conns.laddr.port == port:
              proc.send_signal(SIGTERM)

while True:
  conn, addr = s.accept()
  data = conn.recv(1024)  # Receive Data
  data = data.decode()  # Decode the byte representation to a string
  print(data[2:])  # there is 2 header bytes
  conn.close()
  s.close()
  break
