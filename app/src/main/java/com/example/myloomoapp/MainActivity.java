package com.example.myloomoapp;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.AngularVelocity;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.locomotion.sbv.LinearVelocity;

import java.util.Timer;
import java.util.TimerTask;

import static com.example.myloomoapp.Utils.BASE_YAW_ANGLE;
import static com.example.myloomoapp.Utils.HEAD_PITCH_ANGLE;
import static com.example.myloomoapp.Utils.STEP_SIZE;
import static com.example.myloomoapp.Utils.floatToString;

/**
 * Created by rbigazzi on 2019/11/8.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Head mHead;
    Base mBase;
    boolean isBindH = false;
    boolean isBindB = false;

    Button mBaseLeft;
    Button mBaseRight;
    Button mHeadUp;
    Button mHeadDown;
    Button mAhead;
    Button mResetAll;

    TextView mWorldYawValue;
    TextView mWorldPitchValue;
    TextView mBaseYawValue;
    TextView mBasePitchValue;
    TextView mAngularVelocity;
    TextView mLinearVelocity;

    //View mEditTextFocus;
    Timer mTimer = new Timer();
    TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // get robot head pitch value, the value is angle between head and base int the pitch direction.
                    mBasePitchValue.setText(String.format("Base Pitch: %s", floatToString(mHead.getPitchRespectBase().getAngle())));
                    // get robot head yaw value, the value is angle between head and base int the yaw direction.
                    mBaseYawValue.setText(String.format("Base Yaw: %s", floatToString(mHead.getYawRespectBase().getAngle())));
                    // get robot head yaw value, the value is angle between head and world int the yaw direction.
                    mWorldYawValue.setText(String.format("World Yaw: %s", floatToString(mHead.getWorldYaw().getAngle())));
                    // get robot head pitch value, the value is angle between head and world int the pitch direction.
                    mWorldPitchValue.setText(String.format("World Pitch: %s", floatToString(mHead.getWorldPitch().getAngle())));
                    final AngularVelocity av = mBase.getAngularVelocity();
                    final LinearVelocity lv = mBase.getLinearVelocity();
                    mAngularVelocity.setText(String.format("AngularVelocity: %s", av.getSpeed()));
                    mLinearVelocity.setText(String.format("LinearVelocity: %s", lv.getSpeed()));
                }
            });
        }
    };

    private ServiceBinder.BindStateListener mHeadServiceBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isBindH = true;
            mTimer.schedule(mTimerTask, 50, 50);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.change_activity);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, VisionActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                MainActivity.this.startActivity(myIntent);
            }
        });

        init();

        // get Head instance.
        mHead = Head.getInstance();
        // bindService, if not, all Head api will not work.
        mHead.bindService(getApplicationContext(), mHeadServiceBindListener);
        mBase = Base.getInstance();
        mBase.bindService(getApplicationContext(), mBaseServiceBindListener);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        init();
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // get robot head pitch value, the value is angle between head and base int the pitch direction.
                        mBasePitchValue.setText(String.format("Base Pitch: %s", floatToString(mHead.getPitchRespectBase().getAngle())));
                        // get robot head yaw value, the value is angle between head and base int the yaw direction.
                        mBaseYawValue.setText(String.format("Base Yaw: %s", floatToString(mHead.getYawRespectBase().getAngle())));
                        // get robot head yaw value, the value is angle between head and world int the yaw direction.
                        mWorldYawValue.setText(String.format("World Yaw: %s", floatToString(mHead.getWorldYaw().getAngle())));
                        // get robot head pitch value, the value is angle between head and world int the pitch direction.
                        mWorldPitchValue.setText(String.format("World Pitch: %s", floatToString(mHead.getWorldPitch().getAngle())));
                        final AngularVelocity av = mBase.getAngularVelocity();
                        final LinearVelocity lv = mBase.getLinearVelocity();
                        mAngularVelocity.setText(String.format("AngularVelocity: %s", av.getSpeed()));
                        mLinearVelocity.setText(String.format("LinearVelocity: %s", lv.getSpeed()));
                    }
                });
            }
        };
        // get Head instance.
        mHead = Head.getInstance();
        // bindService, if not, all Head api will not work.
        mHead.bindService(getApplicationContext(), mHeadServiceBindListener);
        mBase = Base.getInstance();
        mBase.bindService(getApplicationContext(), mBaseServiceBindListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBindH) {
            mHead.unbindService();
        }
        if (isBindB) {
            mBase.unbindService();
        }
        mTimerTask = null;
        mTimer.cancel();
        mTimer = null;
    }

    @Override
    protected void onDestroy() {
        mHead.unbindService();
        mBase.unbindService();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        super.onDestroy();
    }

    public void init() {
        mBaseLeft = findViewById(R.id.left);
        mBaseRight = findViewById(R.id.right);
        mHeadUp = findViewById(R.id.up);
        mHeadDown = findViewById(R.id.down);
        mAhead = findViewById(R.id.ahead);
        mResetAll = findViewById(R.id.reset_all);

        mHeadUp.setOnClickListener(this);
        mHeadDown.setOnClickListener(this);
        mBaseLeft.setOnClickListener(this);
        mBaseRight.setOnClickListener(this);
        mAhead.setOnClickListener(this);
        mResetAll.setOnClickListener(this);

        mBasePitchValue = findViewById(R.id.base_pitch);
        mWorldPitchValue = findViewById(R.id.world_pitch);
        mBaseYawValue = findViewById(R.id.base_yaw);
        mWorldYawValue = findViewById(R.id.world_yaw);
        mAngularVelocity = findViewById(R.id.angular_velocity);
        mLinearVelocity = findViewById(R.id.linear_velocity);
    }

    public void onClick(View view) {
        if (!isBindH) {
            return;
        }
        switch (view.getId()) {
            case R.id.reset_all:
                mHead.resetOrientation();
                mBase.setAngularVelocity(0);
                mBase.setLinearVelocity(0);
                mHead.setHeadLightMode(0);
                break;
            case R.id.left:
                mBase.cleanOriginalPoint();
                Pose2D left_pose2D = mBase.getOdometryPose(-1);
                mBase.setOriginalPoint(left_pose2D);
                mBase.addCheckPoint(0, 0, (float)(BASE_YAW_ANGLE*Math.PI/180));
                // To move just the head
                //float left_value = mHead.getYawRespectBase().getAngle();
                //left_value += BASE_YAW_ANGLE*Math.PI/180;
                //mHead.setWorldYaw(left_value);
                mHead.setHeadLightMode(1);
                break;
            case R.id.right:
                mBase.cleanOriginalPoint();
                Pose2D right_pose2D = mBase.getOdometryPose(-1);
                mBase.setOriginalPoint(right_pose2D);
                mBase.addCheckPoint(0, 0, (float)(-BASE_YAW_ANGLE*Math.PI/180));
                // To move just the head
                //float right_value = mHead.getYawRespectBase().getAngle();
                //right_value -= BASE_YAW_ANGLE*Math.PI/180;
                //mHead.setWorldYaw(right_value);
                mHead.setHeadLightMode(2);
                break;
            case R.id.up:
                float up_value = mHead.getWorldPitch().getAngle();
                up_value += HEAD_PITCH_ANGLE*Math.PI/180;
                mHead.setWorldPitch(up_value);
                mHead.setHeadLightMode(3);
                break;
            case R.id.down:
                float down_value = mHead.getWorldPitch().getAngle();
                down_value -= HEAD_PITCH_ANGLE*Math.PI/180;
                mHead.setWorldPitch(down_value);
                mHead.setHeadLightMode(4);
                break;
            case R.id.ahead:
                mBase.cleanOriginalPoint();
                Pose2D ahead_pose2D = mBase.getOdometryPose(-1);
                mBase.setOriginalPoint(ahead_pose2D);
                mBase.addCheckPoint(STEP_SIZE, 0, 0);
                mHead.setHeadLightMode(5);
                break;
            default:
                break;
        }
    }
}
