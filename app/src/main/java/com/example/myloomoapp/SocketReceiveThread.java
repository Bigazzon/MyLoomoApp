package com.example.myloomoapp;

import android.util.Log;

import com.segway.robot.sdk.voice.VoiceException;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.example.myloomoapp.Utils.R_SERVER_PORT;
import static com.example.myloomoapp.Utils.SERVER_IP;

public class SocketReceiveThread implements Runnable {

    private static final String TAG = "SocketReceiveThread";
    private BufferedReader in;
    private MainActivity mActivity;
    Socket socket = null;

    SocketReceiveThread(MainActivity mainActivity) {
        mActivity = mainActivity;
    }

    @Override
    public void run() {
        while(true) {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                //System.out.println("Not Connected----------------------------------------------------");
                socket = new Socket(serverAddr, R_SERVER_PORT);
                //System.out.println("Connected--------------------------------------------------------");
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while (true) {
                    try {
                        int msg = in.read();
                        msg -= 48;
                        System.out.println(msg);
                        if(msg==-49) {
                            throw new Exception();
                        }
                        final int finalMsg = msg;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mActivity.command(finalMsg);
                                } catch (VoiceException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    } catch (Exception e) {
                        //e.printStackTrace();
                        //System.out.println("Server side sending thread is not responding");
                        break;
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
                System.out.println("Server side sending thread is not responding");
            }
        }
    }
}