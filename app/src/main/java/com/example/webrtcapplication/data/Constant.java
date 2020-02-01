/**
 * Constant.class
 * Author: Cui Donghang
 * Version: 1.0
 * Date: 2019.12.20
 */
package com.example.webrtcapplication.data;

public class Constant {
    // Signal server ip address
    private static final String HOST = "192.168.43.74";
    // Signal server port
    private static final String PORT = "8080";
    // Signal server url
    public static final String WS_SERVER = "ws://" + HOST + ":" + PORT + "/WebRTCServer/websocket";
    // Stun server url
    public static final String STUN_SERVER = "stun:stun.l.google.com:19302";

    public static final String CHANNEL = "channel";
    public static final String OPEN = "open";
    public static final String INIT = "init";
    public static final int VOLUME = 5;
    // Default video width
    public static int VIDEO_WIDTH = 480;
    // Default video height
    public static int VIDEO_HEIGHT = 640;
    public static final int VIDEO_FPS = 60;
    // Events which are used in Signal.class and signal server
    public enum Event {
        JOIN, OFFER, ANSWER, CANDIDATE, LEAVE
    }
}
