# WebRTCApplication
A simple WebRTC application for android
---
This is a simple WebRTC video chat application.
* Management framework:
> Gradle
* Dependencies:
> androidx.appcompat:appcompat:1.1.0
> androidx.constraintlayout:constraintlayout:1.1.3
> com.google.android.material:material:1.0.0
> org.webrtc:google-webrtc:1.0.28513
> org.java-websocket:Java-WebSocket:1.4.0
> com.google.code.gson:gson:2.8.5
> org.jetbrains:annotations:15.0
* Android SDK version:
> Android 8.0(API Level 26) or newer
* Instruction:
The application should run with a signal server used to transpond signal. Mine is here:
https://github.com/gale-force-eight/WebRTCServer
1. Open the directory with Android Studio(Recommended), and then the Gradle will download dependencies automatically.
2. Modify the configuration which is in **./WebRTCApplication/app/src/main/com/example/webrtcapplication/data/Constant.java**. 
> * HOST: the ip address of your signal server
> * PORT: the port of your signal server
> * WS_SERVER: the whole url of your signal server
> * STUN_SERVER: the url of stun server
> * Event: the signal used to transpond between signal server and clients.
3. Start your signal server before the application runs on your phone.
4. Build bundle or apk and install it on two android phones(Android 8.0 or newer recommended). Open and login with two different username, and then you can enjoy it!
---
P.s. It is just a simple semi-finished product and is used for study.
