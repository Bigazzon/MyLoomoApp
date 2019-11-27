package com.example.myloomoapp;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class VisionRealSenseFragment extends Fragment {

    private static final String TAG = "VisionRealSenseFragment";

    private Vision fVision;

    private View view;

    private StreamInfo mColorInfo;
    private StreamInfo mDepthInfo;
    private StreamInfo mFishInfo;
    private ImageView mColorImageView;
    private ImageView mDepthImageView;
    private ImageView mFishImageView;

    //private SurfaceView mColorSurfaceView;
    //private SurfaceView mDepthSurfaceView;

    VisionRealSenseFragment(Vision vision) {
        fVision = vision;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.vision_fragment1, container, false);
        init();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        start();
    }

    @Override
    public void onStop() {
        super.onStop();
        stop();
    }

    /*
    public synchronized void start() {
        // Get activated stream info from Vision Service. Streams are pre-config.
        StreamInfo[] streamInfos = fVision.getActivatedStreamInfo();
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
                    fVision.startPreview(StreamType.COLOR, mColorSurfaceView.getHolder().getSurface());
                    break;
                case StreamType.DEPTH:
                    // Adjust depth surface view
                    mDepthSurfaceView.getHolder().setFixedSize(info.getWidth(), info.getHeight());
                    layout = mDepthSurfaceView.getLayoutParams();
                    layout.width = (int) (mDepthSurfaceView.getHeight() * ratio);
                    mDepthSurfaceView.setLayoutParams(layout);

                    // preview depth stream
                    fVision.startPreview(StreamType.DEPTH, mDepthSurfaceView.getHolder().getSurface());
                    break;
                case StreamType.FISH_EYE:
                    break;
            }
        }

    }

    public synchronized void stop() {
        StreamInfo[] streamInfos = fVision.getActivatedStreamInfo();

        for (StreamInfo info : streamInfos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    // Stop color preview
                    fVision.stopPreview(StreamType.COLOR);
                    break;
                case StreamType.DEPTH:
                    // Stop depth preview
                    fVision.stopPreview(StreamType.DEPTH);
                    break;
                case StreamType.FISH_EYE:
                    break;
            }
        }
    }
    */

    private void init(){
        mColorImageView = view.findViewById(R.id.color_view);
        mDepthImageView = view.findViewById(R.id.depth_view);
        mFishImageView = view.findViewById(R.id.fish_view);
        //mColorSurfaceView = view.findViewById(R.id.color_view);
        //mColorSurfaceView.setBackgroundColor(0XFFFFFFFF);
        //mDepthSurfaceView = view.findViewById(R.id.depth_view);
        //mDepthSurfaceView.setBackgroundColor(0XFFFFFFFF);
    }


    private synchronized void start() {
        Log.d(TAG, "start() called");
        StreamInfo[] streamInfos = fVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    mColorInfo = info;
                    fVision.startListenFrame(StreamType.COLOR, mFrameListener);
                    break;
                case StreamType.DEPTH:
                    mDepthInfo = info;
                    fVision.startListenFrame(StreamType.DEPTH, mFrameListener);
                    break;
                case StreamType.FISH_EYE:
                    mFishInfo = info;
                    fVision.startListenFrame(StreamType.FISH_EYE, mFrameListener);
                    break;
            }
        }
    }

    private synchronized void stop() {
        Log.d(TAG, "stop() called");
        fVision.stopListenFrame(StreamType.COLOR);
        fVision.stopListenFrame(StreamType.DEPTH);
        fVision.stopListenFrame(StreamType.FISH_EYE);
    }

    private Vision.FrameListener mFrameListener = new Vision.FrameListener() {
        @Override
        public void onNewFrame(int streamType, Frame frame) {
            Bitmap mColorBitmap = Bitmap.createBitmap(mColorInfo.getWidth(), mColorInfo.getHeight(), Bitmap.Config.ARGB_8888);
            Bitmap mDepthBitmap = Bitmap.createBitmap(mDepthInfo.getWidth(), mDepthInfo.getHeight(), Bitmap.Config.RGB_565);
            Bitmap mFisheyeBitmap = Bitmap.createBitmap(mFishInfo.getWidth(), mFishInfo.getHeight(), Bitmap.Config.ALPHA_8);
            switch (streamType) {
                case StreamType.COLOR:
                    // draw color image to bitmap and display
                    mColorBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
                    mIImageState.updateImage(StreamType.COLOR, mColorBitmap);
                    break;
                case StreamType.DEPTH:
                    // draw depth image to bitmap and display
                    mDepthBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
                    mIImageState.updateImage(StreamType.DEPTH, mDepthBitmap);
                    break;
                case StreamType.FISH_EYE:
                    // draw fisheye image to bitmap and display
                    mFisheyeBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
                    mIImageState.updateImage(StreamType.FISH_EYE, mFisheyeBitmap);
                    break;            }
        }
    };

    private long startTimeColor = System.currentTimeMillis();

    private void saveColorToFile(final Bitmap bitmap) {
        if (System.currentTimeMillis() - startTimeColor < TIME_PERIOD) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                startTimeColor = System.currentTimeMillis();
                File f = new File(Objects.requireNonNull(Objects.requireNonNull(getActivity()).getExternalFilesDir(null)).getAbsolutePath() + "/C" + System.currentTimeMillis() + ".png");
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

    private IImageState mIImageState = new IImageState() {

        Runnable mRunnable;

        @Override
        public void updateImage(int type, final Bitmap bitmap) {
            switch (type) {
                case StreamType.COLOR:
                    mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mColorImageView.setImageBitmap(bitmap);
                        }
                    };
                    //saveColorToFile(bitmap);
                    break;
                case StreamType.DEPTH:
                    mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mDepthImageView.setImageBitmap(bitmap);
                        }
                    };
                    //saveDepthToFile(bitmap);
                    break;
                case StreamType.FISH_EYE:
                    mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mFishImageView.setImageBitmap(bitmap);
                        }
                    };
                    break;            }

            if (mRunnable != null) {
                Objects.requireNonNull(getActivity()).runOnUiThread(mRunnable);
            }
        }
    };

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
                File f = new File(Objects.requireNonNull(Objects.requireNonNull(getActivity()).getExternalFilesDir(null)).getAbsolutePath() + "/D" + System.currentTimeMillis() + ".png");
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
}
