import threading
import server_receive
import server_send

class Receive_Thread(threading.Thread):
    def __init__(self):
        super(Receive_Thread, self).__init__(name="Receive thread")
        print("Receiving Thread Started!")

    def run(self):
        server_receive.start()

"""
class Send_Thread(threading.Thread):
    def __init__(self):
        super(Send_Thread, self).__init__(name="Send thread")
        print("Sending Thread Started!")

    def run(self):
"""

t1 = Receive_Thread()
t1.start()

server_send.start()
