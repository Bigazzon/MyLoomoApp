package com.example.myloomoapp;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.vision.Vision;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

import static com.example.myloomoapp.Utils.BASE_YAW_ANGLE;
import static com.example.myloomoapp.Utils.HEAD_PITCH_ANGLE;
import static com.example.myloomoapp.Utils.STEP_SIZE;

/**
 * Created by rbigazzi on 2019/11/8.
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    Head mHead;
    Base mBase;
    Vision mVision;

    private Socket socket;
    PrintWriter printWriter;

    TextView textStatus;

    FloatingActionButton fab;

    ImageView head_view;

    boolean isBindH = false;
    boolean isBindB = false;
    boolean isBindV = false;

    Thread send_thread;
    Thread receive_thread;

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
    private ServiceBinder.BindStateListener mVisionServiceBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isBindV = true;
            fab.show();
        }
        @Override
        public void onUnbind(String reason) {
            isBindV = false;
        }
    };


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
                if((Integer)fab.getTag()==0) {
                    fab.setTag(1);
                    Objects.requireNonNull(getSupportActionBar()).setTitle("Vision Module (RGB, Depth and Fisheye)");
                    VisionFragment1 visionFragment1 = new VisionFragment1(mVision);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame, visionFragment1).commit();
                }
                else if((Integer)fab.getTag()==1) {
                    fab.setTag(2);
                    mVision.unbindService();
                    Objects.requireNonNull(getSupportActionBar()).setTitle("Vision Module (Head)");
                    VisionFragment2 visionFragment2 = new VisionFragment2();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame, visionFragment2).commit();
                }
                else {
                    fab.setTag(0);
                    Objects.requireNonNull(getSupportActionBar()).setTitle("Locomotion Module");
                    LocomotionFragment locomotionFragment = new LocomotionFragment(mHead, mBase);
                    fab.hide();
                    mVision.bindService(MainActivity.this, mVisionServiceBindListener);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame, locomotionFragment).commit();

                }
            }
        });
        fab.hide();
        mHead = Head.getInstance();
        mHead.bindService(getApplicationContext(), mHeadServiceBindListener);
        mBase = Base.getInstance();
        mBase.bindService(getApplicationContext(), mBaseServiceBindListener);
        mVision = Vision.getInstance();
        mVision.bindService(this, mVisionServiceBindListener);
        LocomotionFragment locomotionFragment = new LocomotionFragment(mHead, mBase);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame, locomotionFragment).commit();
        head_view = findViewById(R.id.head_view_main);
        head_view.setBackgroundColor(0X000000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
        send_thread = new Thread(new SocketSendThread());
        send_thread.start();
        receive_thread = new Thread(new SocketReceiveThread(this));
        receive_thread.start();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mHead = Head.getInstance();
        mHead.bindService(this, mHeadServiceBindListener);
        mBase = Base.getInstance();
        mBase.bindService(this, mBaseServiceBindListener);
        mVision = Vision.getInstance();
        mVision.bindService(this, mVisionServiceBindListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHead.unbindService();
        mBase.unbindService();
        mVision.unbindService();
        send_thread.interrupt();
        receive_thread.interrupt();
        finish();
    }

    @Override
    protected void onDestroy() {
        mHead.unbindService();
        mBase.unbindService();
        mVision.unbindService();
        super.onDestroy();
    }

    public void movement(int type) {
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
            default:
                break;
        }
    }

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
}
