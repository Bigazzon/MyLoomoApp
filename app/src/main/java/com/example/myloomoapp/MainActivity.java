package com.example.myloomoapp;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.AngularVelocity;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.locomotion.sbv.LinearVelocity;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Head mHead;
    Base mBase;
    boolean isBind = false;

    static final float RADIUS = (float) 0.23;

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

    View mEditTextFocus;
    Timer mTimer = new Timer();
    TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // get robot head pitch value, the value is angle between head and base int the pitch direction.
                    mBasePitchValue.setText(Util.floatToString(mHead.getPitchRespectBase().getAngle()));
                    // get robot head yaw value, the value is angle between head and base int the yaw direction.
                    mBaseYawValue.setText(Util.floatToString(mHead.getYawRespectBase().getAngle()));
                    // get robot head yaw value, the value is angle between head and world int the yaw direction.
                    mWorldYawValue.setText(Util.floatToString(mHead.getWorldYaw().getAngle()));
                    // get robot head pitch value, the value is angle between head and world int the pitch direction.
                    mWorldPitchValue.setText(Util.floatToString(mHead.getWorldPitch().getAngle()));
                    final AngularVelocity av = mBase.getAngularVelocity();
                    final LinearVelocity lv = mBase.getLinearVelocity();
                    mAngularVelocity.setText("AngularVelocity:" + av.getSpeed());
                    mLinearVelocity.setText("LinearVelocity:" + lv.getSpeed());
                }
            });
        }
    };

    private ServiceBinder.BindStateListener mServiceBindListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            isBind = true;
            // get robot head current movement pattern.
            /*switch (mHead.getMode()) {
                case Head.MODE_ORIENTATION_LOCK:
                    ((RadioButton) findViewById(R.id.lock)).setChecked(true);
                    break;
                case Head.MODE_SMOOTH_TACKING:
                    ((RadioButton) findViewById(R.id.smooth_track)).setChecked(true);
                    break;
                default:
                    break;
            }*/
            mTimer.schedule(mTimerTask, 50, 50);
        }

        @Override
        public void onUnbind(String reason) {
            isBind = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Closing Application", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                finish();
                System.exit(0);
            }
        });

        init();

        // get Head instance.
        mHead = Head.getInstance();
        // bindService, if not, all Head api will not work.
        mHead.bindService(getApplicationContext(), mServiceBindListener);
        /*
        mMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.smooth_track:
                        // set robot head in MODE_SMOOTH_TACKING.
                        if (isBind) {
                            mHead.setMode(Head.MODE_SMOOTH_TACKING);
                        }
                        break;
                    case R.id.lock:
                        // set robot head in MODE_ORIENTATION_LOCK.
                        if (isBind) {
                            mHead.setMode(Head.MODE_ORIENTATION_LOCK);
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

        mBasePitchValue = findViewById(R.id.base_pitch);
        mWorldPitchValue = findViewById(R.id.world_pitch);
        mBaseYawValue = findViewById(R.id.base_yaw);
        mWorldYawValue = findViewById(R.id.world_yaw);
        mAngularVelocity = findViewById(R.id.angular_velocity);
        mLinearVelocity = findViewById(R.id.linear_velocity);
    }

    public void onClick(View view) {
        if (!isBind) {
            return;
        }
        switch (view.getId()) {
            case R.id.reset_all:
                mHead.resetOrientation();
                mBase.setAngularVelocity(0);
                mBase.setLinearVelocity(0);
                break;
            case R.id.left:
                mHead.setWorldPitch(Util.getEditTextFloatValue(mWorldPitch));
                break;
            case R.id.right:
                mHead.setWorldYaw(Util.getEditTextFloatValue(mWorldYaw));
                break;
            case R.id.up:
                mHead.setYawRespectBase(Util.getEditTextFloatValue(mBaseYaw));
                break;
            case R.id.down:
                mHead.setYawRespectBase(Util.getEditTextFloatValue(mBaseYaw));
                break;
            case R.id.ahead:
                mHead.setPitchAngularVelocity(Util.getEditTextFloatValue(mSpeedPitch));
                break;
                /*
            case R.id.yaw_speed:
                if (!Util.isEditTextEmpty(mSpeedYaw)
                        && mHead.getMode() == Head.MODE_ORIENTATION_LOCK) {
                    // set robot head yaw angularVelocity.
                    mHead.setYawAngularVelocity(Util.getEditTextFloatValue(mSpeedYaw));
                }
                break;
            case R.id.yaw_incremental:
                if (!Util.isEditTextEmpty(mYawIncremental)
                        && mHead.getMode() == Head.MODE_SMOOTH_TACKING) {
                    // Set the angular increment in the yaw direction, the value is angle between head and base int the yaw direction
                    mHead.setIncrementalYaw(Util.getEditTextFloatValue(mYawIncremental));
                }
                break;
            case R.id.pitch_incremental:
                if (!Util.isEditTextEmpty(mPitchIncremental)
                        && mHead.getMode() == Head.MODE_SMOOTH_TACKING) {
                    // Set the angular increment in the pitch direction, the value is angle between head and base int the pitch direction
                    mHead.setIncrementalPitch(Util.getEditTextFloatValue(mPitchIncremental));
                }
                break;
            case R.id.head_mode:
                if (!Util.isEditTextEmpty(mHeadMode)) {
                    mHead.setHeadLightMode(Integer.parseInt(mHeadMode.getText().toString().trim()));
                }
                break;
                 */
            default:
                break;
        }
    }
}
