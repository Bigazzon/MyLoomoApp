package com.example.myloomoapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.myloomoapp.Utils.MAGALLI;

public class SocketSendThread implements Runnable {

    private static final String TAG = "SocketSendThread";

    private MainActivity mActivity;

    private FileInputStream fis = null;

    private Timer timerObj = new Timer();
    private TimerTask timerTaskObj = new TimerTask() {
        public void run() {
            byte[] img_bytes;
            if (MAGALLI) {
                /* Image Handling */
                String img_path = Environment.getExternalStorageDirectory() + "/DCIM/test_magalli.jpg";
                File image_file = new File(img_path);
                try {
                    fis = new FileInputStream(image_file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Bitmap bm = BitmapFactory.decodeStream(fis);
                img_bytes = getBytesfromBitmap(bm);

                mActivity.send_captured(img_bytes);
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //Log.d(TAG, "Taking picture and sending it");
                        mActivity.takePicture();
                    }
                }).start();
            }
        }
    };

    SocketSendThread(MainActivity mainActivity) {
        mActivity = mainActivity;
    }

    @Override
    public void run() {
        timerObj.schedule(timerTaskObj, 5000, 400);
    }

    private byte[] getBytesfromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}