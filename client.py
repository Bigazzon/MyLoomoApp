import socket
import sys
 
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(('172.20.10.3', 8888)) #IP is the server IP
 
for args in sys.argv:
    if args == "":
        args = 'no args'
    else:
        msg = "CIAO"
        s.send(msg.encode())
 
print ("Goodbye!")
