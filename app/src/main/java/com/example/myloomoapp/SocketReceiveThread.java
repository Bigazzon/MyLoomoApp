package com.example.myloomoapp;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

import static com.example.myloomoapp.Utils.R_SERVER_PORT;
import static com.example.myloomoapp.Utils.SERVER_IP;

public class SocketReceiveThread implements Runnable {

    private BufferedReader in;
    private MainActivity mActivity;

    SocketReceiveThread(MainActivity mainActivity) {
        mActivity = mainActivity;
    }

    @Override
    public void run() {
        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            //System.out.println("Not Connected----------------------------------------------------");
            Socket socket = new Socket(serverAddr, R_SERVER_PORT);
            //System.out.println("Connected--------------------------------------------------------");
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                try {
                    while (true) {
                        int msg = in.read();
                        msg -= 48;
                        System.out.println(msg);
                        final int finalMsg = msg;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mActivity.movement(finalMsg);
                            }
                        }).start();
                    }
                } catch (EOFException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}