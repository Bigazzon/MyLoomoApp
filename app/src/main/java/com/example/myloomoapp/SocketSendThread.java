package com.example.myloomoapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    private String img_path = Environment.getExternalStorageDirectory()+ "/Pictures/test.jpg";
    private File image_file = new File(img_path);
    private FileInputStream fis = null;
    private Bitmap bm;
    private byte[] img_bytes;

    private Timer timerObj = new Timer();
    private TimerTask timerTaskObj = new TimerTask() {
        public void run() {
            /* Image Handling */
            try {
                fis = new FileInputStream(image_file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            bm = BitmapFactory.decodeStream(fis);
            img_bytes = getBytesfromBitmap(bm);

            /* Connection Handling */
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                Socket socket = new Socket(serverAddr, S_SERVER_PORT);

                //out = new DataOutputStream(socket.getOutputStream());
                //out.writeUTF("Test");

                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeInt(img_bytes.length);
                out.write(img_bytes);
                out.flush();
                Log.d(TAG, "IMAGE SENT");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void run() {
        timerObj.schedule(timerTaskObj, 0, 1000);
    }

    private byte[] getBytesfromBitmap(Bitmap bitmap){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}