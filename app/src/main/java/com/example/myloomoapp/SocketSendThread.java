package com.example.myloomoapp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.myloomoapp.Utils.SERVER_IP;
import static com.example.myloomoapp.Utils.S_SERVER_PORT;


public class SocketSendThread implements Runnable {

    private static final String TAG = "SocketSendThread";

    private DataOutputStream out;

    private Timer timerObj = new Timer();
    private TimerTask timerTaskObj = new TimerTask() {
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                //System.out.println("Not Connected------------------------------------------------");
                Socket socket = new Socket(serverAddr, S_SERVER_PORT);
                //System.out.println("Connected----------------------------------------------------");
                out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF("Test");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void run() {
        timerObj.schedule(timerTaskObj, 0, 1000);
    }
}