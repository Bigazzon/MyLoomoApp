import socket
import sys
import struct
 
#HOST = '127.0.0.1' #this is your localhost
#HOST = '172.20.10.5' #this is ip of Iphone Biga
#HOST = '100.93.45.7' #this is ip of Fastweb
HOST = '192.168.43.155' #ip bigabyte
PORT = 8888
 
def start():
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

    while 1:
        conn, addr = s.accept() 
        #print ('Connected with ' + addr[0] + ':' + str(addr[1]))

        buf = bytearray()
        while len(buf)<4:
            buf += conn.recv(4-len(buf))
        size = struct.unpack('!i', buf)[0]
        #print("Receiving " + str(size) + " bytes")
        with open('/home/bigazzon/Downloads/test.jpg', 'wb') as f:
            while size > 0:
                data = conn.recv(1024)
                f.write(data)
                size -= len(data)
        #print('Image Saved')
        conn.close()


        #buf = conn.recv(64)
        #print (buf)
        
        #msg = "Prova"
        #conn.send(msg.encode())
        conn.close()
    s.close()
