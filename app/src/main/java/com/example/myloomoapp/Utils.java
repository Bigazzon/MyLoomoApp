package com.example.myloomoapp;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by rbigazzi on 2019/11/8.
 */

public class Utils {
    private static final String TAG = "Utils";

    static int HEAD_PITCH_ANGLE = 15; //degrees
    static int BASE_YAW_ANGLE = 10;   //degrees
    static float STEP_SIZE = 0.3f;    //meters

    public static boolean isEditTextEmpty(EditText editText) {
        if (editText == null) {
            return false;
        }
        String text = editText.getText().toString().trim();
        return TextUtils.isEmpty(text);
    }

    static String floatToString(float f) {
        return String.valueOf(f);
    }

    public static float getEditTextFloatValue(EditText editText) {
        String text = editText.getText().toString().trim();
        return Float.parseFloat(text);
    }
}