package com.example.myloomoapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.AngularVelocity;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.locomotion.sbv.LinearVelocity;

import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.myloomoapp.Utils.BASE_YAW_ANGLE;
import static com.example.myloomoapp.Utils.HEAD_PITCH_ANGLE;
import static com.example.myloomoapp.Utils.STEP_SIZE;

public class LocomotionFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "LocomotionFragment";
    DecimalFormat df = new DecimalFormat("#0.000");
    private Head fHead;
    private Base fBase;

    private TextView mYawValue;
    private TextView mWorldPitchValue;
    private TextView mOdometryValue;
    private TextView mBasePitchValue;
    private TextView mBlank;
    private TextView mVelocity;

    private View view;
    private Timer mTimer = new Timer();
    private TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // get robot head pitch value, the value is angle between head and base int the pitch direction.
                    mBasePitchValue.setText(String.format("Base Pitch: %s", df.format(fHead.getPitchRespectBase().getAngle())));
                    // get robot head yaw value, the value is angle between head and base int the yaw direction.
                    Pose2D pose = fBase.getOdometryPose(-1);
                    mOdometryValue.setText(String.format("Odo: x: %s, y: %s, θ: %s", df.format(pose.getX()), df.format(pose.getY()), df.format(pose.getTheta())));
                    // get robot head yaw value, the value is angle between head and world int the yaw direction.
                    mYawValue.setText(String.format("Base / World Yaw: %s, %s", df.format(fHead.getYawRespectBase().getAngle()), df.format(fHead.getWorldYaw().getAngle())));
                    // get robot head pitch value, the value is angle between head and world int the pitch direction.
                    mWorldPitchValue.setText(String.format("World Pitch: %s", df.format(fHead.getWorldPitch().getAngle())));
                    final LinearVelocity lv = fBase.getLinearVelocity();
                    final AngularVelocity av = fBase.getAngularVelocity();
                    mVelocity.setText(String.format("Lin / Ang Vel: %s, %s", df.format(lv.getSpeed()), df.format(av.getSpeed())));
                    mBlank.setText("");
                }
            });
        }
    };
    LocomotionFragment(Head head, Base base) {
        this.fHead = head;
        this.fBase = base;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.locomotion_fragment, container, false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        init();
        mTimer.schedule(mTimerTask, 50, 50);
    }

    @Override
    public void onStop() {
        super.onStop();
        mTimerTask = null;
        mTimer.cancel();
        mTimer = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.left:
                Log.d(TAG, "ROTATING LEFT");
                fBase.cleanOriginalPoint();
                Pose2D left_pose2D = fBase.getOdometryPose(-1);
                fBase.setOriginalPoint(left_pose2D);
                fBase.addCheckPoint(0, 0, (float) (BASE_YAW_ANGLE * Math.PI / 180));
                // To move just the head
                //float left_value = fHead.getYawRespectBase().getAngle();
                //left_value += BASE_YAW_ANGLE*Math.PI/180;
                //fHead.setWorldYaw(left_value);
                fHead.setHeadLightMode(7);
                break;
            case R.id.right:
                Log.d(TAG, "ROTATING RIGHT");
                fBase.cleanOriginalPoint();
                Pose2D right_pose2D = fBase.getOdometryPose(-1);
                fBase.setOriginalPoint(right_pose2D);
                fBase.addCheckPoint(0, 0, (float) (-BASE_YAW_ANGLE * Math.PI / 180));
                // To move just the head
                //float right_value = fHead.getYawRespectBase().getAngle();
                //right_value -= BASE_YAW_ANGLE*Math.PI/180;
                //fHead.setWorldYaw(right_value);
                fHead.setHeadLightMode(7);
                break;
            case R.id.up:
                Log.d(TAG, "HEAD UP");
                float up_value = fHead.getWorldPitch().getAngle();
                up_value += HEAD_PITCH_ANGLE * Math.PI / 180;
                fHead.setWorldPitch(up_value);
                fHead.setHeadLightMode(8);
                break;
            case R.id.down:
                Log.d(TAG, "HEAD DOWN");
                float down_value = fHead.getWorldPitch().getAngle();
                down_value -= HEAD_PITCH_ANGLE * Math.PI / 180;
                fHead.setWorldPitch(down_value);
                fHead.setHeadLightMode(8);
                break;
            case R.id.ahead:
                Log.d(TAG, "MOVING AHEAD");
                fBase.cleanOriginalPoint();
                Pose2D ahead_pose2D = fBase.getOdometryPose(-1);
                fBase.setOriginalPoint(ahead_pose2D);
                fBase.addCheckPoint(STEP_SIZE, 0, 0);
                fHead.setHeadLightMode(5);
                break;
            case R.id.reset_all:
                Log.d(TAG, "HEAD RESET");
                fHead.resetOrientation();
                fBase.setAngularVelocity(0);
                fBase.setLinearVelocity(0);
                fHead.setHeadLightMode(0);
                break;
            default:
                break;
        }
    }

    private void init() {
        Button mHeadUp = view.findViewById(R.id.up);
        Button mHeadDown = view.findViewById(R.id.down);
        Button mBaseLeft = view.findViewById(R.id.left);
        Button mBaseRight = view.findViewById(R.id.right);
        Button mAhead = view.findViewById(R.id.ahead);
        Button mResetAll = view.findViewById(R.id.reset_all);

        mHeadUp.setOnClickListener(this);
        mHeadDown.setOnClickListener(this);
        mBaseLeft.setOnClickListener(this);
        mBaseRight.setOnClickListener(this);
        mAhead.setOnClickListener(this);
        mResetAll.setOnClickListener(this);

        mBasePitchValue = view.findViewById(R.id.base_pitch);
        mWorldPitchValue = view.findViewById(R.id.world_pitch);
        mOdometryValue = view.findViewById(R.id.odometry);
        mYawValue = view.findViewById(R.id.wb_yaw);
        mVelocity = view.findViewById(R.id.velocity);
        mBlank = view.findViewById(R.id.blank);
    }
}
