/**
* MainActivity.class
* Author: Cui Donghang
* Version: 1.0
* Date: 2019.12.20
 */

package com.example.webrtcapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.example.webrtcapplication.model.User;
import com.example.webrtcapplication.model.Signal;
import com.example.webrtcapplication.data.Constant;

/**
 * WebRTC Client Main Activity
 */
public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private ProgressBar loadingProgressBar;
    private EditText remoteUserEditText;
    private LinearLayout remoteLayout;
    private TextView remoteOfferTextView;
    private Button acceptButton;
    private Button refuseButton;
    private Button connectButton;
    private Button disconnectButton;

    private User currentRemoteUser;

    private SurfaceViewRenderer localSurfaceView;
    private SurfaceViewRenderer remoteSurfaceView;
    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection.RTCConfiguration webRTCConfiguration;
    private PeerConnectionObserver connectionObserver;

    private MediaStream localMediaStream;
    private VideoTrack localVideoTrack, remoteVideoTrack;
    private AudioTrack audioTrack;
    private PeerConnection peerConnection;
    private List<String> streamList;
    private WebSocketClient webSocketClient;
    private DataChannel channel;
    private MySdpObserver observer;

    private boolean isDisconnected = true;
    private User user;
    
    private Message toastMsg;
    private Message createPeerConnectionMsg;
    private Message viewVisibilityMsg;
    private Message btnEnabledMsg;

    /************ Handlers to refresh ui ************/
    /** Set a toast */
    @SuppressLint("HandlerLeak")
    private Handler toastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_SHORT).show();
        }
    };
    /** Initialize or start peer connection */
    @SuppressLint("HandlerLeak")
    private Handler createPeerConnectionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null) {
                String obj = (String) msg.obj;
                if (!TextUtils.isEmpty(obj)) {
                    switch (obj) {
                        case "init":
                            initPeerConnection();
                            break;
                        case "open":
                            createPeerConnection();
                            break;
                    }
                }
            }
        }
    };

    /** Set visibility of views */
    @SuppressLint("HandlerLeak")
    private Handler viewVisibilityHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null) {
                findViewById(msg.arg1).setVisibility(msg.arg2 == 0 ? View.GONE : View.VISIBLE);
            }
        }
    };

    /** Set availability of buttons */
    @SuppressLint("HandlerLeak")
    private Handler btnEnabledHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null) {
                findViewById(msg.arg1).setEnabled(msg.arg2 != 0);
            }
        }
    };

    /** MainActivity onCreate() */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        user = new User(getIntent().getStringExtra("username"));
        loadingProgressBar = findViewById(R.id.main_loading);
        remoteUserEditText = findViewById(R.id.remote_username);
        remoteLayout = findViewById(R.id.remoteLayout);
        remoteOfferTextView = findViewById(R.id.remote_offer);
        acceptButton = findViewById(R.id.accept);
        refuseButton = findViewById(R.id.refuse);
        acceptButton.setEnabled(true);
        refuseButton.setEnabled(true);
        localSurfaceView = findViewById(R.id.local_video);
        remoteSurfaceView = findViewById(R.id.remote_video);
        connectButton = findViewById(R.id.connect);
        disconnectButton = findViewById(R.id.disconnect);

        // Initialize PeerConnection
        createPeerConnectionMsg = Message.obtain(createPeerConnectionHandler);
        createPeerConnectionMsg.obj = Constant.INIT;
        createPeerConnectionHandler.sendMessage(createPeerConnectionMsg);

        // Initialize local surface view
        setToast("Welcome to WebRTC Application!\nYour username: " + user.getUsername());
        loadingProgressBar.setVisibility(View.VISIBLE);
        setToast("Connecting to signal server");


        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton shareButton = findViewById(R.id.share);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v, "Share WebRTC Client to your friends!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Initialize connectButton
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Connect to remote RTCPeer
                currentRemoteUser = new User(remoteUserEditText.getText().toString());
                startConn();
                viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
                viewVisibilityMsg.arg1 = R.id.main_loading;
                viewVisibilityMsg.arg2 = 1;
                viewVisibilityHandler.sendMessage(viewVisibilityMsg);
                //loadingProgressBar.setVisibility(View.VISIBLE);
                btnEnabledMsg = Message.obtain(btnEnabledHandler);
                btnEnabledMsg.arg1 = R.id.connect;
                btnEnabledMsg.arg2 = 0;
                btnEnabledHandler.sendMessage(btnEnabledMsg);
                // connectButton.setEnabled(false);
            }
        });

        // Initialize disconnectButton
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disconnect from remote RTCPeer
                setToast("Disconnecting");
                closeConn();
                isDisconnected = true;
                btnEnabledMsg = Message.obtain(btnEnabledHandler);
                btnEnabledMsg.arg1 = R.id.disconnect;
                btnEnabledMsg.arg2 = 0;
                btnEnabledHandler.sendMessage(btnEnabledMsg);
                viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
                viewVisibilityMsg.arg1 = R.id.main_loading;
                viewVisibilityMsg.arg2 = 0;
                viewVisibilityHandler.sendMessage(viewVisibilityMsg);
                if (!remoteUserEditText.getText().toString().equals("")) {
                    btnEnabledMsg = Message.obtain(btnEnabledHandler);
                    btnEnabledMsg.arg1 = R.id.connect;
                    btnEnabledMsg.arg2 = 1;
                    btnEnabledHandler.sendMessage(btnEnabledMsg);
                }
            }
        });

        remoteUserEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Check whether the remote user is currently online or not
                if (isDisconnected) {
                    if (!s.toString().equals("")) {
                        btnEnabledMsg = Message.obtain(btnEnabledHandler);
                        btnEnabledMsg.arg1 = R.id.connect;
                        btnEnabledMsg.arg2 = 1;
                        btnEnabledHandler.sendMessage(btnEnabledMsg);
                    }
                    else {
                        btnEnabledMsg = Message.obtain(btnEnabledHandler);
                        btnEnabledMsg.arg1 = R.id.connect;
                        btnEnabledMsg.arg2 = 0;
                        btnEnabledHandler.sendMessage(btnEnabledMsg);
                    }
                }
            }
        });

        setupConnection();
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
        if (id == R.id.about_me) {
            return true;
        }
        if (id == R.id.app_version) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupConnection();
    }

    /** MainActivity onDestroy() */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocketClient.close();
    }

    /** MainActivity onPause() */
    @Override
    protected void onPause() {
        super.onPause();
        webSocketClient.close();
    }

    void setToast(String msg) {
        toastMsg = Message.obtain(toastHandler);
        toastMsg.obj = msg;
        toastHandler.sendMessage(toastMsg);
    }

    /** Initialize websocket client and connect to signal server */
    void setupConnection() {
        if (webSocketClient == null || webSocketClient.isClosed()) {
            webSocketClient = new WebSocketClient(URI.create(Constant.WS_SERVER)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Message viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
                    viewVisibilityMsg.arg1 = R.id.main_loading;
                    viewVisibilityMsg.arg2 = 1;
                    viewVisibilityHandler.sendMessage(viewVisibilityMsg);
                    webSocketClient.send(new Gson().toJson(new Signal(Constant.Event.JOIN, user.getUsername(), "", null, null)));
                }

                @Override
                public void onMessage(String message) {
                    if (!TextUtils.isEmpty(message)) {
                        Log.d("onMessage: ", message);
                        Signal s = new Gson().fromJson(message, Signal.class);
                        if (s != null) {
                            Constant.Event e = s.getSignal();
                            if (e != null) {
                                switch(e) {
                                    case JOIN:
                                        viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
                                        viewVisibilityMsg.arg1 = R.id.main_loading;
                                        viewVisibilityMsg.arg2 = 0;
                                        viewVisibilityHandler.sendMessage(viewVisibilityMsg);
                                        // Toast.makeText(MainActivity.this, s.getMsg(), Toast.LENGTH_LONG).show();
                                        setToast(s.getMsg());
                                        if (s.getCode() == 0) {
                                            createPeerConnectionMsg = Message.obtain(createPeerConnectionHandler);
                                            createPeerConnectionMsg.obj = Constant.OPEN;
                                            createPeerConnectionHandler.sendMessage(createPeerConnectionMsg);
                                        }
                                        break;

                                    case OFFER: {
                                        acceptButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                // Accept the offer from someone
                                                isDisconnected = false;
                                                btnEnabledMsg = Message.obtain(btnEnabledHandler);
                                                btnEnabledMsg.arg1 = R.id.disconnect;
                                                btnEnabledMsg.arg2 = 1;
                                                btnEnabledHandler.sendMessage(btnEnabledMsg);
                                                currentRemoteUser = new User(s.getRemoteUser());

                                                viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
                                                viewVisibilityMsg.arg1 = R.id.main_loading;
                                                viewVisibilityMsg.arg2 = 1;
                                                viewVisibilityHandler.sendMessage(viewVisibilityMsg);
                                                viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
                                                viewVisibilityMsg.arg1 = R.id.remoteLayout;
                                                viewVisibilityMsg.arg2 = 0;
                                                viewVisibilityHandler.sendMessage(viewVisibilityMsg);

                                                peerConnection.setRemoteDescription(observer, s.getSdp());
                                                MediaConstraints mediaConstraints = new MediaConstraints();
                                                mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
                                                peerConnection.createAnswer(observer, mediaConstraints);
                                            }
                                        });
                                        refuseButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                // Refuse the offer from someone
                                                isDisconnected = true;
                                                viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
                                                viewVisibilityMsg.arg1 = R.id.remoteLayout;
                                                viewVisibilityMsg.arg2 = 0;
                                                viewVisibilityHandler.sendMessage(viewVisibilityMsg);
                                                webSocketClient.send(new Gson().toJson(new Signal(Constant.Event.ANSWER, user.getUsername(), s.getRemoteUser(), null, null)));
                                            }
                                        });

                                        remoteOfferTextView.setText(s.getRemoteUser() + " want to connect with you");
                                        viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
                                        viewVisibilityMsg.arg1 = R.id.remoteLayout;
                                        viewVisibilityMsg.arg2 = 1;
                                        viewVisibilityHandler.sendMessage(viewVisibilityMsg);
                                        break;
                                    }

                                    case ANSWER:
                                        if (s.getCode() == 1) {
                                            currentRemoteUser = null;
                                            setToast(s.getMsg());
                                        } else {
                                            setToast("Received answer");
                                            isDisconnected = false;
                                            btnEnabledMsg = Message.obtain(btnEnabledHandler);
                                            btnEnabledMsg.arg1 = R.id.disconnect;
                                            btnEnabledMsg.arg2 = 1;
                                            btnEnabledHandler.sendMessage(btnEnabledMsg);
                                            peerConnection.setRemoteDescription(observer, s.getSdp());
                                        }
                                        break;

                                    case CANDIDATE:
                                        IceCandidate iceCandidate = s.getCandidate();
                                        if (iceCandidate != null) {
                                            peerConnection.addIceCandidate(iceCandidate);
                                        }
                                        break;

                                    case LEAVE:
                                        closeConn();
                                        btnEnabledMsg = Message.obtain(btnEnabledHandler);
                                        btnEnabledMsg.arg1 = R.id.disconnect;
                                        btnEnabledMsg.arg2 = 0;
                                        btnEnabledHandler.sendMessage(btnEnabledMsg);
                                        break;
                                }
                            }
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    closeConn();
                }

                @Override
                public void onError(Exception ex) {
                    Log.d(TAG, ex.getMessage());
                }
            };
            try {
                webSocketClient.connectBlocking();
            }
            catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }

    /** Initialize PeerConnectionFactory and SurfaceRenderer views */
    private void initPeerConnection() {
        // Initialising PeerConnectionFactory
        List<PeerConnection.IceServer> iceServers;

        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        // Create EglBase
        eglBase = EglBase.create();
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableEncryption = true;
        options.disableNetworkMonitor = true;
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setOptions(options)
                .createPeerConnectionFactory();


        // Initialize IceServer
        iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(Constant.STUN_SERVER).createIceServer();
        iceServers.add(iceServer);

        streamList = new ArrayList<>();

        webRTCConfiguration = new PeerConnection.RTCConfiguration(iceServers);

        connectionObserver = new PeerConnectionObserver() {
            /** Trigger when the local peerConnection received its ice candidate */
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Log.d(TAG, "onIceCandidate : " + iceCandidate.sdp);
                Log.d(TAG, "onIceCandidate : sdpMid = " + iceCandidate.sdpMid + " sdpMLineIndex = " + iceCandidate.sdpMLineIndex);
                if (currentRemoteUser != null) {
                    Signal s = new Signal(Constant.Event.CANDIDATE, user.getUsername(), currentRemoteUser.getUsername(), null, iceCandidate);
                    String text = new Gson().toJson(s);
                    Log.d(TAG, "setIceCandidate : " + text);
                    webSocketClient.send(new Gson().toJson(s));
                }

            }

            /** Trigger when the local peerConnection received media stream from another peer */
            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
                viewVisibilityMsg.arg1 = R.id.main_loading;
                viewVisibilityMsg.arg2 = 0;
                viewVisibilityHandler.sendMessage(viewVisibilityMsg);
                Log.d(TAG, "onAddStream : " + mediaStream.toString());
                List<VideoTrack> videoTracks = mediaStream.videoTracks;
                if (videoTracks != null && videoTracks.size() > 0) {
                    remoteVideoTrack = videoTracks.get(0);
                    if (remoteVideoTrack != null) {
                        remoteVideoTrack.addSink(remoteSurfaceView);
                    }
                }
                List<AudioTrack> audioTracks = mediaStream.audioTracks;
                if (audioTracks != null && audioTracks.size() > 0) {
                    AudioTrack audioTrack = audioTracks.get(0);
                    if (audioTrack != null) {
                        audioTrack.setVolume(Constant.VOLUME);
                    }
                }
            }
        };
        initSurfaceView(remoteSurfaceView);
        initSurfaceView(localSurfaceView);
        localSurfaceView.post(new Runnable() {
            @Override
            public void run() {
                Constant.VIDEO_HEIGHT = localSurfaceView.getHeight();
                Constant.VIDEO_WIDTH = localSurfaceView.getWidth();
            }
        });
        setupLocalStream(localSurfaceView);
    }

    private void createPeerConnection() {
        peerConnection = peerConnectionFactory.createPeerConnection(webRTCConfiguration, connectionObserver);

        DataChannel.Init init = new DataChannel.Init();
        if (peerConnection != null) {
            channel = peerConnection.createDataChannel(Constant.CHANNEL, init);
        }
        DateChannelObserver channelObserver = new DateChannelObserver();
        connectionObserver.setObserver(channelObserver);

        /**
         * Initialize sdp observer(listener)
         * Trigger when local peerConnection create a SessionDescription(Offer or Answer)
         */
        observer = new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                //将会话描述设置在本地
                peerConnection.setLocalDescription(this, sessionDescription);
                SessionDescription localDescription = peerConnection.getLocalDescription();
                SessionDescription.Type type = localDescription.type;
                Log.d(TAG, "onCreateSuccess == " + " type == " + type);
                //接下来使用之前的WebSocket实例将offer发送给服务器
                if (type == SessionDescription.Type.OFFER) {
                    //呼叫
                    webSocketClient.send(new Gson().toJson(new Signal(Constant.Event.OFFER, user.getUsername(), remoteUserEditText.getText().toString(), sessionDescription, null)));
                } else if (type == SessionDescription.Type.ANSWER) {
                    //应答
                    webSocketClient.send(new Gson().toJson(new Signal(Constant.Event.ANSWER, user.getUsername(), currentRemoteUser.getUsername(), sessionDescription, null)));

                } else if (type == SessionDescription.Type.PRANSWER) {
                    //再次应答

                }
            }
        };

        peerConnection.addTrack(localVideoTrack, streamList);
        peerConnection.addTrack(audioTrack, streamList);
        peerConnection.addStream(localMediaStream);

    }

    /** Start connecting to another client */
    private void startConn() {
        setToast("Connecting to " + remoteUserEditText.getText().toString());
        // Toast.makeText(getApplicationContext(), "Connecting to " + remoteUserEditText.getText().toString(), Toast.LENGTH_LONG).show();
        viewVisibilityMsg = Message.obtain(viewVisibilityHandler);
        viewVisibilityMsg.arg1 = R.id.main_loading;
        viewVisibilityMsg.arg2 = 1;
        viewVisibilityHandler.sendMessage(viewVisibilityMsg);
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(observer, mediaConstraints);
    }

    private void initSurfaceView(SurfaceViewRenderer localSurfaceView) {
        localSurfaceView.init(eglBase.getEglBaseContext(), null);
        localSurfaceView.setMirror(true);
        localSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        localSurfaceView.setKeepScreenOn(true);
        localSurfaceView.setZOrderMediaOverlay(true);
        localSurfaceView.setEnableHardwareScaler(false);
    }

    /** Setup local media stream */
    private void setupLocalStream(SurfaceViewRenderer localSurfaceView) {
        localMediaStream = peerConnectionFactory.createLocalMediaStream("LocalStream1");

        // Initialize video
        VideoSource videoSource = peerConnectionFactory.createVideoSource(true);
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), eglBase.getEglBaseContext());
        VideoCapturer videoCapturer = createVideoCapturer();
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(Constant.VIDEO_WIDTH, Constant.VIDEO_HEIGHT, Constant.VIDEO_FPS); // width, height, frame per second
        localVideoTrack = peerConnectionFactory.createVideoTrack("VideoTrack1", videoSource);
        localVideoTrack.addSink(localSurfaceView);
        localMediaStream.addTrack(localVideoTrack);

        // Initialize audio
        MediaConstraints audioConstraints = new MediaConstraints();
        //回声消除
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        //自动增益
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        //高音过滤
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        //噪音处理
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        AudioSource audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        audioTrack = peerConnectionFactory.createAudioTrack("AudioTrack1", audioSource);
        audioTrack.setVolume(Constant.VOLUME);
        localMediaStream.addTrack(audioTrack);

    }


    /** Initialize local camera capture */
    private VideoCapturer createVideoCapturer() {
        if (Camera2Enumerator.isSupported(this)) {
            return createCameraCapturer(new Camera2Enumerator(this));
        } else {
            return createCameraCapturer(new Camera1Enumerator(true));
        }
    }


    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // Try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    private void sendMessage(String message) {
        byte[] msg = message.getBytes();
        DataChannel.Buffer buffer = new DataChannel.Buffer(ByteBuffer.wrap(msg), false);
        channel.send(buffer);
    }

    /** close the connection between two peer connection */
    private void closeConn() {
        if (currentRemoteUser != null) {
            String to = currentRemoteUser.getUsername();
            currentRemoteUser = null;
            webSocketClient.send(new Gson().toJson(new Signal(Constant.Event.LEAVE, user.getUsername(), to, null, null)));
        }

        isDisconnected = true;
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
 /*     if (webSocketClient != null) webSocketClient.close();
        if (localSurfaceView != null) {
            remoteSurfaceView.release();
            initSurfaceView(remoteSurfaceView);
        }
*/
        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeSink(remoteSurfaceView);
            remoteVideoTrack = null;
        }
        createPeerConnectionMsg = Message.obtain(createPeerConnectionHandler);
        createPeerConnectionMsg.obj = Constant.OPEN;
        createPeerConnectionHandler.sendMessage(createPeerConnectionMsg);
    }

}
