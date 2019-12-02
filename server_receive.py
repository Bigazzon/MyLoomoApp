import socket
import sys
import struct
import io
import tkinter
import numpy as np
import matplotlib.pyplot as plt
from PIL import Image, ImageTk

#HOST = '127.0.0.1' #this is your localhost
#HOST = '172.20.10.5' #this is ip of Iphone Biga
#HOST = '100.93.45.7' #this is ip of Fastweb
HOST = '192.168.43.155' #ip bigabyte
PORT = 8888
 
def start():
    root = tkinter.Tk()
    root.geometry('+%d+%d' % (100,100))
    old_label_image = None

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    #socket.socket: must use to create a socket.
    #socket.AF_INET: Address Format, Internet = IP Addresses.
    #socket.SOCK_STREAM: two-way, connection-based byte streams.
    #print ("Receiving Socket Created")
     
    #Bind socket to Host and Port
    try:
        s.bind((HOST, PORT))
    except socket.error as err:
        print ("Bind Failed, Error Code: " + str(err[0]) + ', Message: ' + str(err[1]))
        sys.exit()
     
    #print ("Socket Bind Success!")
     
    #listen(): This method sets up and start TCP listener.
    s.listen(5)
    print ("Receiving Socket is Now Listening")
    
    while True:
        conn, addr = s.accept() 
        #print ('Connected with ' + addr[0] + ':' + str(addr[1]))

        buf = bytearray()
        full_data = bytearray()
        while len(buf)<4:
            buf += conn.recv(4-len(buf))
        size = struct.unpack('!i', buf)[0]
        #print("Receiving " + str(size) + " bytes")
        with open('/home/bigazzon/Downloads/test.jpg', 'wb') as f:
            while size > 0:
                data = conn.recv(1024)
                #f.write(data)
                size -= len(data)
                full_data += data
        #print('Image Saved')
        try:
            image = Image.open(io.BytesIO(full_data))
            root.geometry('%dx%d' % (image.size[0],image.size[1]))
            tkpi = ImageTk.PhotoImage(image)
            label_image = tkinter.Label(root, image=tkpi)
            label_image.place(x=0,y=0,width=image.size[0],height=image.size[1])
            root.title("Loomo Head Camera")
            if old_label_image is not None:
                old_label_image.destroy()
            old_label_image = label_image
            root.update_idletasks()
            root.update()
            #root.mainloop()
        except Exception as e:
            pass

        #buf = conn.recv(64)
        #print (buf)
        
        #msg = "Prova"
        #conn.send(msg.encode())
        conn.close()
    s.close()
