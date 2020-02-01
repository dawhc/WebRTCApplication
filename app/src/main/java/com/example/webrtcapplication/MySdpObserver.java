/**
 * MySdpObserver.class
 * Author: Cui Donghang
 * Version: 1.0
 * Date: 2019.12.20
 */
package com.example.webrtcapplication;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class MySdpObserver implements SdpObserver {

    private final String TAG = "MySdpObserver";

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d(TAG, "onCreateSuccess ==  ");
    }

    @Override
    public void onSetSuccess() {
        Log.d(TAG, "onSetSuccess ==  ");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.e(TAG, "onCreateFailure ==  " + s);
    }

    @Override
    public void onSetFailure(String s) {
        Log.e(TAG, "onSetFailure ==  " + s);
    }
}
