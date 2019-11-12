package com.example.myloomoapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by rbigazzi on 2019/11/8.
 */

public class VisionActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "VisionSampleActivity";

    private Vision mVision;

    private SurfaceView mColorSurfaceView;
    private SurfaceView mDepthSurfaceView;

    private Switch mSwitch;

    private ServiceBinder.BindStateListener mServiceBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind() called");
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "onUnbind() called with: reason = [" + reason + "]");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vision);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.change_activity_v);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(VisionActivity.this, MainActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                VisionActivity.this.startActivity(myIntent);
            }
        });
        // get Vision SDK instance
        mVision = Vision.getInstance();
        mVision.bindService(this, mServiceBindListener);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVision.unbindService();
        finish();
    }

    public synchronized void start() {
        Log.d(TAG, "start() called");

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
            }
        }

    }

    public synchronized void stop() {
        Log.d(TAG, "stop() called");
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
            }
        }
    }

    public void init() {
        mSwitch = findViewById(R.id.switch_vision);
        mSwitch.setOnCheckedChangeListener(this);
        mColorSurfaceView = findViewById(R.id.color_view);
        mDepthSurfaceView = findViewById(R.id.depth_view);
    }

    private long startTimeColor = System.currentTimeMillis();

    private void saveColorToFile(final Bitmap bitmap) {
        if (System.currentTimeMillis() - startTimeColor < TIME_PERIOD) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                startTimeColor = System.currentTimeMillis();
                File f = new File(getExternalFilesDir(null).getAbsolutePath() + "/C" + System.currentTimeMillis() + ".png");
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
                File f = new File(getExternalFilesDir(null).getAbsolutePath() + "/D" + System.currentTimeMillis() + ".png");
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.switch_vision) {
            if (isChecked) {
                start();
            } else {
                stop();
            }
        }
    }
}
