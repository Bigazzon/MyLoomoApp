package com.example.myloomoapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.AngularVelocity;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.locomotion.sbv.LinearVelocity;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.myloomoapp.Utils.BASE_YAW_ANGLE;
import static com.example.myloomoapp.Utils.HEAD_PITCH_ANGLE;
import static com.example.myloomoapp.Utils.STEP_SIZE;
import static com.example.myloomoapp.Utils.floatToString;

public class LocomotionFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "LocomotionFragment";

    LocomotionFragment(Head head, Base base){
        this.fHead = head;
        this.fBase = base;
    }

    private Head fHead;
    private Base fBase;

    private TextView mWorldYawValue;
    private TextView mWorldPitchValue;
    private TextView mBaseYawValue;
    private TextView mBasePitchValue;
    private TextView mAngularVelocity;
    private TextView mLinearVelocity;

    private View view;

    private Timer mTimer = new Timer();
    private TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // get robot head pitch value, the value is angle between head and base int the pitch direction.
                    mBasePitchValue.setText(String.format("Base Pitch: %s", floatToString(fHead.getPitchRespectBase().getAngle())));
                    // get robot head yaw value, the value is angle between head and base int the yaw direction.
                    mBaseYawValue.setText(String.format("Base Yaw: %s", floatToString(fHead.getYawRespectBase().getAngle())));
                    // get robot head yaw value, the value is angle between head and world int the yaw direction.
                    mWorldYawValue.setText(String.format("World Yaw: %s", floatToString(fHead.getWorldYaw().getAngle())));
                    // get robot head pitch value, the value is angle between head and world int the pitch direction.
                    mWorldPitchValue.setText(String.format("World Pitch: %s", floatToString(fHead.getWorldPitch().getAngle())));
                    final AngularVelocity av = fBase.getAngularVelocity();
                    final LinearVelocity lv = fBase.getLinearVelocity();
                    mAngularVelocity.setText(String.format("AngularVelocity: %s", av.getSpeed()));
                    mLinearVelocity.setText(String.format("LinearVelocity: %s", lv.getSpeed()));
                }
            });
        }
    };

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
                fBase.cleanOriginalPoint();
                Pose2D left_pose2D = fBase.getOdometryPose(-1);
                fBase.setOriginalPoint(left_pose2D);
                fBase.addCheckPoint(0, 0, (float)(BASE_YAW_ANGLE*Math.PI/180));
                // To move just the head
                //float left_value = fHead.getYawRespectBase().getAngle();
                //left_value += BASE_YAW_ANGLE*Math.PI/180;
                //fHead.setWorldYaw(left_value);
                fHead.setHeadLightMode(7);
                break;
            case R.id.right:
                fBase.cleanOriginalPoint();
                Pose2D right_pose2D = fBase.getOdometryPose(-1);
                fBase.setOriginalPoint(right_pose2D);
                fBase.addCheckPoint(0, 0, (float)(-BASE_YAW_ANGLE*Math.PI/180));
                // To move just the head
                //float right_value = fHead.getYawRespectBase().getAngle();
                //right_value -= BASE_YAW_ANGLE*Math.PI/180;
                //fHead.setWorldYaw(right_value);
                fHead.setHeadLightMode(7);
                break;
            case R.id.up:
                float up_value = fHead.getWorldPitch().getAngle();
                up_value += HEAD_PITCH_ANGLE*Math.PI/180;
                fHead.setWorldPitch(up_value);
                fHead.setHeadLightMode(8);
                break;
            case R.id.down:
                float down_value = fHead.getWorldPitch().getAngle();
                down_value -= HEAD_PITCH_ANGLE*Math.PI/180;
                fHead.setWorldPitch(down_value);
                fHead.setHeadLightMode(8);
                break;
            case R.id.ahead:
                fBase.cleanOriginalPoint();
                Pose2D ahead_pose2D = fBase.getOdometryPose(-1);
                fBase.setOriginalPoint(ahead_pose2D);
                fBase.addCheckPoint(STEP_SIZE, 0, 0);
                fHead.setHeadLightMode(5);
                break;
            case R.id.reset_all:
                Log.d(TAG, "RESET");
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
        mBaseYawValue = view.findViewById(R.id.base_yaw);
        mWorldYawValue = view.findViewById(R.id.world_yaw);
        mAngularVelocity = view.findViewById(R.id.angular_velocity);
        mLinearVelocity = view.findViewById(R.id.linear_velocity);
    }
}
