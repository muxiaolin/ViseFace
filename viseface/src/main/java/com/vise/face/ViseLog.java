package com.vise.face;

import android.util.Log;

/**
 * Author: PL
 * Date: 2022/4/25
 * Desc:
 */
public class ViseLog {

    public static boolean Debug = true;
    private static final String TAG = "vise";

    public static void i(String msg) {
        if (!Debug) {
            return;
        }
        Log.i(TAG, msg);
    }

    public static void d(String msg) {
        if (!Debug) {
            return;
        }
        Log.d(TAG, msg);
    }

    public static void w(String msg) {
        if (!Debug) {
            return;
        }
        Log.w(TAG, msg);
    }


    public static void e(String msg) {
        if (!Debug) {
            return;
        }
        Log.e(TAG, msg);
    }

}
