import socket
import sys
import time
from datetime import datetime
 
#HOST = '127.0.0.1' #this is your localhost
#HOST = '172.20.10.5' #this is ip of Iphone Biga
#HOST = '100.93.45.7' #this is ip of Fastweb
HOST = '192.168.43.155' #ip bigabyte

PORT = 8889
 
def start():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    #socket.socket: must use to create a socket.
    #socket.AF_INET: Address Format, Internet = IP Addresses.
    #socket.SOCK_STREAM: two-way, connection-based byte streams.
    #print ("Sending Socket Created")
     
    #Bind socket to Host and Port
    try:
        s.bind((HOST, PORT))
    except socket.error as err:
        print ("Bind Failed, Error Code: " + str(err[0]) + ', Message: ' + str(err[1]))
        sys.exit()
     
    #print ("Socket Bind Success!")
     
    #listen(): This method sets up and start TCP listener.
    s.listen(10)
    print ("Sending Socket is Now Listening")

    while True:
        conn, addr = s.accept() 
        print ('Connected with ' + addr[0] + ':' + str(addr[1]))
        print ("Available actions:\n" + \
               "1) Rotate Left\n" + \
               "2) Rotate Right\n" + \
               "3) Head Up\n" + \
               "4) Head Down\n" + \
               "5) Move Ahead\n" + \
               "6) Reset Head\n" + \
               "7) Speak")
        while True:
            num = -1
            s = input("Select your action or enter string: ")
            if (is_int(s)):
                num = int(s)
                if num == 0:
                        print ("Available actions:\n" + \
                               "1) Rotate Left\n" + \
                               "2) Rotate Right\n" + \
                               "3) Head Up\n" + \
                               "4) Head Down\n" + \
                               "5) Move Ahead\n" + \
                               "6) Reset Head\n" + \
                               "7) Speak")    
                if not (num<1 or num>7):
                    msg = str(num) + "\n"
                    #print(msg + " sending")
                    conn.send(msg.encode('utf-8'))
                    #msg = 1;
                    #conn.send(msg)
            else:
                msg = s + "\n"
                conn.send(msg.encode('utf-8'))
                #print(msg + " sending") 
    s.close()

def is_int(s):
    try: 
        int(s)
        return True
    except ValueError:
        return False