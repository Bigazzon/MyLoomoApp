package com.example.myloomoapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;

/**
 * Created by rbigazzi on 2019/11/8.
 */

public class VisionActivity extends AppCompatActivity {

    private static final String TAG = "VisionActivity";

    private Vision mVision;
    private Head mHead;

    private SurfaceView mColorSurfaceView;
    private SurfaceView mDepthSurfaceView;

    boolean isBindV;
    boolean isBindH;

    private ServiceBinder.BindStateListener mServiceBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isBindV = true;
            start();
        }

        @Override
        public void onUnbind(String reason) {
            isBindV = false;
            stop();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vision);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Vision Module");
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.change_activity_v);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(VisionActivity.this, MainActivity.class);
                float passed = mHead.getWorldPitch().getAngle();
                myIntent.putExtra("passed_pitch_value_m", passed); //Optional parameters
                VisionActivity.this.startActivity(myIntent);
            }
        });
        View parentLayout = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(parentLayout, "Waiting for Vision Module to Start...", Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(Color.parseColor("#000000"));
        snackbar.show();
        // get Vision SDK instance
        mHead = Head.getInstance();
        mHead.bindService(this, new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                isBindH = true;
            }

            @Override
            public void onUnbind(String reason) {
                isBindH = false;
            }
        });
        mVision = Vision.getInstance();
        mVision.bindService(this, mServiceBindListener);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = this.getIntent();
        float passed = Objects.requireNonNull(intent.getExtras()).getFloat("passed_pitch_value_v");
        mHead.setWorldPitch(passed);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHead.unbindService();
        mVision.unbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHead.unbindService();
        mVision.unbindService();
        finish();
    }

    public synchronized void start() {
        // Get activated stream info from Vision Service. Streams are pre-config.
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            // Adjust image ratio for display
            float ratio = (float) info.getWidth() / info.getHeight();
            ViewGroup.LayoutParams layout;
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    // Adjust color surface view
                    mColorSurfaceView.getHolder().setFixedSize(info.getWidth(), info.getHeight());
                    layout = mColorSurfaceView.getLayoutParams();
                    layout.width = (int) (mColorSurfaceView.getHeight() * ratio);
                    mColorSurfaceView.setLayoutParams(layout);

                    // preview color stream
                    mVision.startPreview(StreamType.COLOR, mColorSurfaceView.getHolder().getSurface());
                    break;
                case StreamType.DEPTH:
                    // Adjust depth surface view
                    mDepthSurfaceView.getHolder().setFixedSize(info.getWidth(), info.getHeight());
                    layout = mDepthSurfaceView.getLayoutParams();
                    layout.width = (int) (mDepthSurfaceView.getHeight() * ratio);
                    mDepthSurfaceView.setLayoutParams(layout);

                    // preview depth stream
                    mVision.startPreview(StreamType.DEPTH, mDepthSurfaceView.getHolder().getSurface());
                    break;
                case StreamType.FISH_EYE:
                    break;
            }
        }

    }

    public synchronized void stop() {
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();

        for (StreamInfo info : streamInfos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    // Stop color preview
                    mVision.stopPreview(StreamType.COLOR);
                    break;
                case StreamType.DEPTH:
                    // Stop depth preview
                    mVision.stopPreview(StreamType.DEPTH);
                    break;
                case StreamType.FISH_EYE:
                    break;
            }
        }
    }

    public void init() {
        isBindV = false;
        isBindH = false;
        mColorSurfaceView = findViewById(R.id.color_view);
        mDepthSurfaceView = findViewById(R.id.depth_view);
    }

    /*
    private long startTimeColor = System.currentTimeMillis();

    private void saveColorToFile(final Bitmap bitmap) {
        if (System.currentTimeMillis() - startTimeColor < TIME_PERIOD) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                startTimeColor = System.currentTimeMillis();
                File f = new File(Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath() + "/C" + System.currentTimeMillis() + ".png");
                Log.d(TAG, "saveBitmapToFile(): " + f.getAbsolutePath());
                try {
                    FileOutputStream fOut = new FileOutputStream(f);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.flush();
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    */


    /*
    private static final int TIME_PERIOD = 5 * 1000;

    private long startTimeDepth = System.currentTimeMillis();

    private void saveDepthToFile(final Bitmap bitmap) {
        if (System.currentTimeMillis() - startTimeDepth < TIME_PERIOD) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                startTimeDepth = System.currentTimeMillis();
                File f = new File(Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath() + "/D" + System.currentTimeMillis() + ".png");
                Log.d(TAG, "saveBitmapToFile(): " + f.getAbsolutePath());
                try {
                    FileOutputStream fOut = new FileOutputStream(f);
                    Bitmap greyBitmap = depth2Grey(bitmap);
                    greyBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.flush();
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Bitmap depth2Grey(Bitmap img) {
        int width = img.getWidth();
        int height = img.getHeight();

        int[] pixels = new int[width * height];

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                //grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                grey = (red * 38 + green * 75 + blue * 15) >> 7;
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }
    */
}
