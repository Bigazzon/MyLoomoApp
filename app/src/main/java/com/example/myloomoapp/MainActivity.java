package com.example.myloomoapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.PoseVLS;
import com.segway.robot.algo.dts.BaseControlCommand;
import com.segway.robot.algo.dts.DTSPerson;
import com.segway.robot.algo.dts.PersonTrackingListener;
import com.segway.robot.algo.dts.PersonTrackingProfile;
import com.segway.robot.algo.dts.PersonTrackingWithPlannerListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.locomotion.sbv.StartVLSListener;
import com.segway.robot.sdk.vision.DTS;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.voice.Speaker;
import com.segway.robot.sdk.voice.VoiceException;
import com.segway.robot.sdk.voice.tts.TtsListener;
import com.segway.robot.support.control.HeadPIDController;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.myloomoapp.Utils.BASE_YAW_ANGLE;
import static com.example.myloomoapp.Utils.HEAD_PITCH_ANGLE;
import static com.example.myloomoapp.Utils.SERVER_IP;
import static com.example.myloomoapp.Utils.STEP_SIZE;
import static com.example.myloomoapp.Utils.S_SERVER_PORT;

/**
 * Created by rbigazzi on 2019/11/8.
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static int REQUEST_CAMERA_PERMISSION = 200;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    Head mHead;
    Base mBase;
    Speaker mSpeech;
    Vision mVision;
    TtsListener mTtsListener;
    PrintWriter printWriter;
    ProgressDialog dialog;
    TextView textStatus;
    FloatingActionButton fab;
    ImageView head_view;
    boolean isBindH = false;
    boolean isBindB = false;
    boolean isBindS = false;
    boolean isBindV = false;
    Thread send_thread;
    Thread receive_thread;
    byte[] img_bytes = null;
    private Socket socket;
    private CameraDevice mCameraDevice;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private String mCameraId;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };
    private Size imageDimension;
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private ImageReader imageReader;
    private ServiceBinder.BindStateListener mHeadServiceBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isBindH = true;
        }

        @Override
        public void onUnbind(String reason) {
            isBindH = false;
        }
    };
    private ServiceBinder.BindStateListener mBaseServiceBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isBindB = true;
        }

        @Override
        public void onUnbind(String reason) {
            isBindB = false;
        }
    };
    private ServiceBinder.BindStateListener mSpeechServiceBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isBindS = true;
        }

        @Override
        public void onUnbind(String reason) {
            isBindS = false;
        }
    };
    private ServiceBinder.BindStateListener mVisionServiceBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isBindV = true;
            //fab.show();
            if ((Integer) fab.getTag() == 2) {
                Objects.requireNonNull(getSupportActionBar()).setTitle("Vision Module (RGB, Depth and Fisheye)");
                VisionRealSenseFragment visionRealSenseFragment = new VisionRealSenseFragment(mVision);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame, visionRealSenseFragment).commit();
            }
        }

        @Override
        public void onUnbind(String reason) {
            isBindV = false;
        }
    };





    static int TIME_OUT = 5000;
    long startTime;
    PersonTrackingProfile mPersonTrackingProfile;
    private HeadPIDController mHeadPIDController = new HeadPIDController();
    DTS dts;
    static int SWITCH_MODE = 0;
    private Timer demoTimer = new Timer();
    TimerTask demoTask = new TimerTask() {
        public void run() {
            switch (SWITCH_MODE) {
                case 0:
                    mBase.setControlMode(Base.CONTROL_MODE_NAVIGATION);
                    // start VLS
                    mBase.startVLS(true, true, new StartVLSListener() {
                        @Override
                        public void onOpened() {
                            // set navigation data source
                            mBase.setNavigationDataSource(Base.NAVIGATION_SOURCE_TYPE_VLS);
                            mBase.cleanOriginalPoint();
                            PoseVLS poseVLS = mBase.getVLSPose(-1);
                            mBase.setOriginalPoint(poseVLS);

                            mBase.addCheckPoint(5f, 0f);
                            mBase.addCheckPoint(0f, 0f);
                            mBase.addCheckPoint(5f, 0f);
                            mBase.addCheckPoint(0f, 0f);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.d(TAG, "onError() called with: errorMessage = [" + errorMessage + "]");
                        }
                    });
                    break;
                case 1:
                    mBase.setControlMode(Base.CONTROL_MODE_RAW);
                    mBase.setLinearVelocity(1.0f);
                    //mBase.setAngularVelocity(0.15f);
                    break;
                case 2:
                    mBase.setControlMode(Base.CONTROL_MODE_FOLLOW_TARGET);
                    mPersonTrackingProfile = new PersonTrackingProfile(3, 1.0f);
                    // get the DTS instance
                    dts = mVision.getDTS();
                    // set video source
                    dts.setVideoSource(DTS.VideoSource.CAMERA);
                    // start dts module
                    dts.start();

                    actionInitiateTrack();
            }
        }
    };

    public void actionInitiateTrack() {
        startTime = System.currentTimeMillis();
        dts.startPlannerPersonTracking(null, mPersonTrackingProfile, 100 * 1000 * 1000, mPersonTrackingWithPlannerListener);
    }

    private PersonTrackingWithPlannerListener mPersonTrackingWithPlannerListener = new PersonTrackingWithPlannerListener() {
        @Override
        public void onPersonTrackingWithPlannerResult(DTSPerson person, BaseControlCommand baseControlCommand) {
            if (person == null) {
                if (System.currentTimeMillis() - startTime > TIME_OUT) {
                    mHead.resetOrientation();
                }
                return;
            }

            startTime = System.currentTimeMillis();
            mHead.setMode(Head.MODE_ORIENTATION_LOCK);
            mHeadPIDController.updateTarget(person.getTheta(), person.getDrawingRect(), 480);

            switch (baseControlCommand.getFollowState()) {
                case BaseControlCommand.State.NORMAL_FOLLOW:
                    setBaseVelocity(baseControlCommand.getLinearVelocity(), baseControlCommand.getAngularVelocity());
                    break;
                case BaseControlCommand.State.HEAD_FOLLOW_BASE:
                    mBase.setControlMode(Base.CONTROL_MODE_FOLLOW_TARGET);
                    mBase.updateTarget(0, person.getTheta());
                    break;
                case BaseControlCommand.State.SENSOR_ERROR:
                    setBaseVelocity(0, 0);
                    break;
            }
        }

        private void setBaseVelocity(float linearVelocity, float angularVelocity) {
            mBase.setControlMode(Base.CONTROL_MODE_RAW);
            mBase.setLinearVelocity(linearVelocity);
            mBase.setAngularVelocity(angularVelocity);
        }

        @Override
        public void onPersonTrackingWithPlannerError(int errorCode, String message) {
            try {
                mSpeech.speak("CHAW", mTtsListener);
                mSpeech.waitForSpeakFinish(10000);
            } catch (VoiceException e) {
                e.printStackTrace();
            }
        }
    };

    /*
    private void openCamera() {
        CameraManager manager = (CameraManager) Objects.requireNonNull(getActivity()).getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            assert manager != null;
            mCameraId = manager.getCameraIdList()[0];
            Log.d(TAG, manager.getCameraIdList()[0] + manager.getCameraIdList()[1]);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
                }
            }
            manager.openCamera(mCameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "ID Opened Camera: " + mCameraId);
    }
    */

    /*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_IMAGE | requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bitmap bp = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                head_view.setImageBitmap(bp);
                Log.d(TAG, "FOUND IMAGE");
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Error taking head camera image");
            }
        }
    }
    */
    private File file;
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };
    private TextureView textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Locomotion Module");
        setSupportActionBar(toolbar);
        fab = findViewById(R.id.change_activity);
        fab.setTag(0);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((Integer) fab.getTag() == 0) {
                    closeCamera();
                    fab.setTag(1);
                    Objects.requireNonNull(getSupportActionBar()).setTitle("Vision Module (Head)");
                    VisionCameraFragment visionCameraFragment = new VisionCameraFragment();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame, visionCameraFragment).commit();
                    Log.d(TAG, "SPEECH");
                    try {
                        mSpeech.setVolume(50);
                        mSpeech.speak("CHAW", mTtsListener);
                        mSpeech.waitForSpeakFinish(10000);
                    } catch (VoiceException e) {
                        e.printStackTrace();
                    }
                } else if ((Integer) fab.getTag() == 1) {
                    fab.setTag(2);
                    mVision.bindService(getApplicationContext(), mVisionServiceBindListener);
                    Snackbar.make(view, "Waiting for Real Sense Camera...", Snackbar.LENGTH_LONG).show();
                    Log.d(TAG, "SPEECH");
                    try {
                        mSpeech.setVolume(50);
                        mSpeech.speak("CHAW", mTtsListener);
                        mSpeech.waitForSpeakFinish(10000);
                    } catch (VoiceException e) {
                        e.printStackTrace();
                    }
                } else {
                    fab.setTag(0);
                    mVision.unbindService();
                    openCamera();
                    Objects.requireNonNull(getSupportActionBar()).setTitle("Locomotion Module");
                    LocomotionFragment locomotionFragment = new LocomotionFragment(mHead, mBase);
                    //mVision.bindService(MainActivity.this, mVisionServiceBindListener);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame, locomotionFragment).commit();
                    Log.d(TAG, "SPEECH");
                    try {
                        mSpeech.setVolume(50);
                        mSpeech.speak("CHAW", mTtsListener);
                        mSpeech.waitForSpeakFinish(10000);
                    } catch (VoiceException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        //fab.hide();
        mHead = Head.getInstance();
        mHead.bindService(getApplicationContext(), mHeadServiceBindListener);
        mBase = Base.getInstance();
        mBase.bindService(getApplicationContext(), mBaseServiceBindListener);
        mSpeech = Speaker.getInstance();
        mSpeech.bindService(getApplicationContext(), mSpeechServiceBindListener);
        mVision = Vision.getInstance();
        if(SWITCH_MODE==2) {
            mVision.bindService(this, mVisionServiceBindListener);
        }
        LocomotionFragment locomotionFragment = new LocomotionFragment(mHead, mBase);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame, locomotionFragment).commit();
        head_view = findViewById(R.id.head_view_main);
        head_view.setBackgroundColor(0X000000);
        mTtsListener = new TtsListener() {
            @Override
            public void onSpeechStarted(String s) {
                //s is speech content, callback this method when speech is starting.
                Log.d(TAG, "onSpeechStarted() called with: s = [" + s + "]");
            }

            @Override
            public void onSpeechFinished(String s) {
                //s is speech content, callback this method when speech is finish.
                Log.d(TAG, "onSpeechFinished() called with: s = [" + s + "]");
            }

            @Override
            public void onSpeechError(String s, String s1) {
                //s is speech content, callback this method when speech occurs error.
                Log.d(TAG, "onSpeechError() called with: s = [" + s + "], s1 = [" + s1 + "]");
            }
        };

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
        if(SWITCH_MODE>3) {
            send_thread = new Thread(new SocketSendThread(this));
            send_thread.start();
            receive_thread = new Thread(new SocketReceiveThread(this));
            receive_thread.start();
        }
        Log.e(TAG, "onResume");
        startBackgroundThread();
        //if (textureView.isAvailable()) {
        openCamera();
        //} else {
        //    textureView.setSurfaceTextureListener(textureListener);
        //}
        if(SWITCH_MODE<3) {
            demoTimer.schedule(demoTask, 10000);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mHead = Head.getInstance();
        mHead.bindService(this, mHeadServiceBindListener);
        mBase = Base.getInstance();
        mBase.bindService(this, mBaseServiceBindListener);
        mSpeech = Speaker.getInstance();
        mSpeech.bindService(this, mSpeechServiceBindListener);
        mVision = Vision.getInstance();
        mVision.bindService(this, mVisionServiceBindListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHead.unbindService();
        mBase.unbindService();
        mSpeech.unbindService();
        mVision.unbindService();
        send_thread.interrupt();
        receive_thread.interrupt();
        finish();
    }

    @Override
    protected void onDestroy() {
        mHead.unbindService();
        mBase.unbindService();
        mSpeech.unbindService();
        mVision.unbindService();
        super.onDestroy();
    }

    public void command(int type) throws VoiceException {
        switch (type) {
            case 1:
                Log.d(TAG, "ROTATION LEFT");
                mBase.cleanOriginalPoint();
                Pose2D left_pose2D = mBase.getOdometryPose(-1);
                mBase.setOriginalPoint(left_pose2D);
                mBase.addCheckPoint(0, 0, (float) (BASE_YAW_ANGLE * Math.PI / 180));
                // To move just the head
                //float left_value = mHead.getYawRespectBase().getAngle();
                //left_value += BASE_YAW_ANGLE*Math.PI/180;
                //mHead.setWorldYaw(left_value);
                mHead.setHeadLightMode(7);
                break;
            case 2:
                Log.d(TAG, "ROTATING RIGHT");
                mBase.cleanOriginalPoint();
                Pose2D right_pose2D = mBase.getOdometryPose(-1);
                mBase.setOriginalPoint(right_pose2D);
                mBase.addCheckPoint(0, 0, (float) (-BASE_YAW_ANGLE * Math.PI / 180));
                // To move just the head
                //float right_value = mHead.getYawRespectBase().getAngle();
                //right_value -= BASE_YAW_ANGLE*Math.PI/180;
                //mHead.setWorldYaw(right_value);
                mHead.setHeadLightMode(7);
                break;
            case 3:
                Log.d(TAG, "HEAD UP");
                float up_value = mHead.getWorldPitch().getAngle();
                up_value += HEAD_PITCH_ANGLE * Math.PI / 180;
                mHead.setWorldPitch(up_value);
                mHead.setHeadLightMode(8);
                break;
            case 4:
                Log.d(TAG, "HEAD DOWN");
                float down_value = mHead.getWorldPitch().getAngle();
                down_value -= HEAD_PITCH_ANGLE * Math.PI / 180;
                mHead.setWorldPitch(down_value);
                mHead.setHeadLightMode(8);
                break;
            case 5:
                Log.d(TAG, "MOVING AHEAD");
                mBase.cleanOriginalPoint();
                Pose2D ahead_pose2D = mBase.getOdometryPose(-1);
                mBase.setOriginalPoint(ahead_pose2D);
                mBase.addCheckPoint(STEP_SIZE, 0, 0);
                mHead.setHeadLightMode(5);
                break;
            case 6:
                Log.d(TAG, "HEAD RESET");
                mHead.resetOrientation();
                mBase.setAngularVelocity(0);
                mBase.setLinearVelocity(0);
                mHead.setHeadLightMode(0);
                break;
            case 7:
                Log.d(TAG, "SPEECH");
                mSpeech.setVolume(50);
                mSpeech.speak("CHAW", mTtsListener);
                mSpeech.waitForSpeakFinish(10000);
                mHead.setHeadLightMode(1);
                break;
            default:
                break;
        }
    }

    public byte[] getImg_bytes() {
        return img_bytes;
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {
        img_bytes = null;
        if (null == mCameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            /*
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            */
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            //outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/test_capture.jpg");
            //Log.d(TAG, "Taking picture");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try (Image image = reader.acquireLatestImage()) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        img_bytes = new byte[buffer.capacity()];
                        //Log.d(TAG, "Bytes length: " + img_bytes.length);
                        buffer.get(img_bytes);
                        //save(img_bytes);
                        //Log.d(TAG, "Saving picture");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                send_captured(img_bytes);
                            }
                        }).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    try (OutputStream output = new FileOutputStream(file)) {
                        output.write(bytes);
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    //createCameraPreview();
                }
            };
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    void send_captured(byte[] received_img) {
        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            Socket socket = new Socket(serverAddr, S_SERVER_PORT);

            //out = new DataOutputStream(socket.getOutputStream());
            //out.writeUTF("Test");

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            if (received_img != null) {
                out.writeInt(received_img.length);
                out.write(received_img);
                out.flush();
                Log.d(TAG, "Sending picture of " + received_img.length + " bytes");
            }
            socket.close();
            //Log.d(TAG, "IMAGE SENT");

        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("Server side receiving thread is not responding");
        }
    }

    protected void createCameraPreview() {
        try {
            //SurfaceTexture texture = textureView.getSurfaceTexture();
            //assert texture != null;
            //texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            //Surface surface = new Surface(texture);
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //captureRequestBuilder.addTarget(surface);
            /*
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
             */
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            mCameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(mCameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    /*
    protected void updatePreview() {
        if(null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    */

    private void closeCamera() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}
