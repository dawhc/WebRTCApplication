/**
 * DateChannelObserver.java
 * Author: Cui Donghang
 * Version: 1.0
 * Date: 2019.12.20
 */

package com.example.webrtcapplication;

import android.util.Log;

import org.webrtc.DataChannel;


public class DateChannelObserver implements DataChannel.Observer {

    private String TAG = "DateChannelObserver";

    @Override
    public void onBufferedAmountChange(long l) {
        Log.d(TAG, "onBufferedAmountChange : " + l);
    }

    @Override
    public void onStateChange() {

    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        Log.d(TAG, "onMessage DataChannel : " + buffer.toString());
    }
}
