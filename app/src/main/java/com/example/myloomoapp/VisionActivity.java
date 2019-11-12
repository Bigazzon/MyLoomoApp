package com.example.myloomoapp;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.vision.Vision;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;

public class VisionActivity extends AppCompatActivity {

    private static final String TAG = "VisionSampleActivity";
    private static final int TIME_PERIOD = 5 * 1000;

    private Vision mVision;

    ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind() called");
            mPreviewSwitch.setEnabled(true);
            mTransferSwitch.setEnabled(true);
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
        mVision.bindService(this, mBindStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVision.unbindService();
        finish();
    }




}
