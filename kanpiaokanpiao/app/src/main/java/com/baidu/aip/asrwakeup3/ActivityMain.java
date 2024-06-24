package com.baidu.aip.asrwakeup3;



/**
 * 这个Activity用于展示和调用AndroidManifest.xml里定义的其它Activity。本身没有业务逻辑。
 * 使用前请阅读 README_IMPORTANT
 * <p>
 * ActivityOnlineRecog : 在线识别，用于展示在线情况下的识别参数和效果。
 * ActivityOffline：离线命令词识别。用于展示离线情况下，离线语法的参数和效果。并可以测试在线的去情况下，离线语法的参数并不启用。
 * ActivityNlu： 语义理解。 在识别出文字的情况下，百度语音在线服务端和本地SDK可以对这些识别出的文字做语义解析（即分词）。
 * 其中语音服务端做的语义解析必须在线。即语义解析的中文为在线识别成功后的结果。
 * 本地SDK做的语义解析，会覆盖服务端做的语义解析结果。语义解析的中文可以是在线识别成功后的结果，也可以是离线语法的识别结果
 * ActivityAllRecog ： 全部识别功能，涵盖前面三个acitivty的所有功能。
 * ActivityWakeUp： 唤醒词功能。 唤醒词指的是SDK收到某个关键词的声音后回调用户的代码，与Android 系统的锁屏唤醒无关。
 * <p>
 * AcitivityMiniRecog： 内含有调用SDK识别功能的最小代码。用于debug和反馈SDK问题。
 */
//import com.baidu.aip.asrwakeup3.uiasr.activity.ActivityListMain;
//
//public class ActivityMain extends ActivityListMain {
//
//}



import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.net.DhcpInfo;
import android.os.Build;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.net.wifi.WifiManager;

import com.baidu.aip.asrwakeup3.core.util.AuthUtil;
import com.baidu.speech.asr.SpeechConstant;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

public class ActivityMain extends AppCompatActivity {

    FishWakeup  aa;
    private FishCameraService cameraService;
    private boolean isBound = false;
    SurfaceTexture previewsurface = null;

//    private ScaleGestureDetector scaleGestureDetector;
//    private GestureDetector gestureDetector;

    private ScaleGestureDetector scaleGestureDetector;
    private float zoomLevel = 1.0f;

    private String SERVER_IP;


    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FishCameraService.LocalBinder binder = (FishCameraService.LocalBinder) service;
            cameraService = binder.getService();
            isBound = true;
            if (previewsurface != null) {
                cameraService.setSurfaceTexture(previewsurface);  //rpc call 啊， 这是最终的跨GUIleader, 和 subSerice 之间的调用。
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private static String intToIp(int ipInt) {
        return (ipInt & 0xff) + "." +
                ((ipInt >> 8) & 0xff) + "." +
                ((ipInt >> 16) & 0xff) + "." +
                (ipInt >> 24 & 0xff);
    }

    public void udpSend(String message) {

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 12347);
            socket.send(sendPacket);
            // Since System.out.println is not recommended for Android, use Log.d for debugging
            Log.d("UDP", "Message sent: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) { //我是client， 我是看漂的， 我必须打开wifi，连接的wifi是热点， 是另外一个抖音直播的手机且打开热点的
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            if (dhcpInfo != null) {
//                localip=  intToIp(dhcpInfo.ipAddress);
                SERVER_IP = intToIp(dhcpInfo.serverAddress);

            }
        }



        // Create a ConstraintLayout as the root layout
        ConstraintLayout rootLayout = new ConstraintLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Create the TextureView
        TextureView textureView1 = new TextureView(this);
        textureView1.setId(View.generateViewId());
        ConstraintLayout.LayoutParams textureViewParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT, 0);
        textureViewParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        textureViewParams.bottomToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        textureViewParams.matchConstraintPercentHeight = 0.6f; // 60% of the parent's height
        textureView1.setLayoutParams(textureViewParams);
        rootLayout.addView(textureView1);

        // Create the LinearLayout for the bottom part
        LinearLayout bottomLayout = new LinearLayout(this);
        bottomLayout.setId(View.generateViewId());
        bottomLayout.setOrientation(LinearLayout.VERTICAL);
        ConstraintLayout.LayoutParams bottomLayoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT, 0);
        bottomLayoutParams.topToBottom = textureView1.getId();
        bottomLayoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        bottomLayoutParams.matchConstraintPercentHeight = 0.4f; // 40% of the parent's height
        bottomLayout.setLayoutParams(bottomLayoutParams);
        rootLayout.addView(bottomLayout);


        Button personView = new Button(this);
        personView.setText("遛鱼");
        personView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        bottomLayout.addView(personView);

        Button kanpiao = new Button(this);
        kanpiao.setText("看漂");
        kanpiao.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        bottomLayout.addView(kanpiao);


        // Add a Button to the bottom layout
        Button button1 = new Button(this);
        button1.setText("Button 1");
        button1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        bottomLayout.addView(button1);

        // Add an EditText to the bottom layout
        final EditText editText = new EditText(this);
        editText.setHint("Enter text here");
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        bottomLayout.addView(editText);

        // Set the root layout as the content view
        setContentView(rootLayout);


        // Set the button click listener

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = editText.getText().toString();
                // Network operations should not be done on the UI thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        udpSend(message);
                    }
                }).start();
            }
        });


        personView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Network operations should not be done on the UI thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        udpSend("fishcatching");
                    }
                }).start();
            }
        });


        kanpiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Network operations should not be done on the UI thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        udpSend("kanpiao");
                    }
                }).start();
            }
        });








        //权限申请需要在GUI leader app 里完成
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET}, 198);

        }

        //GUI leader 启动背后的服务进程， 这个服务进程一直运行，不管GUI进程的生命周期的（pause，active）
        //但是如果GUIleader (this MainActivity） 退出里， 那么这个服务进程也会退出的。

        //启动后面的服务，放到后面了。
//        Intent serviceIntent = new Intent(this, CameraService.class);
//        startForegroundService(serviceIntent); //api26 以上

//
//        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
//        gestureDetector = new GestureDetector(this, new GestureListener());


        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                zoomLevel *= detector.getScaleFactor();
                if (zoomLevel < 1.0f) {
                    zoomLevel = 1.0f;
                }
                if (isBound) {
                    cameraService.setZoom(zoomLevel);
                }
                return true;
            }
        });

        textureView1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                return true;
            }
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // 权限尚未被授予，现在请求它们
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1235);
        }



         textureView1.setSurfaceTextureListener(surfaceTextureListener);


        aa = new FishWakeup(this, this);
        Map<String, Object> params = AuthUtil.getParam();
        params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");
        aa.start(params); //一次成功唤醒一次，唤醒引擎就退出了。





    } //end of 首页初始化


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            previewsurface = surface;
//            startCameraService(surface);



            // 绑定服务并传递 SurfaceTexture
            Intent serviceIntent = new Intent(ActivityMain.this, FishCameraService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE); //guileader 和 subSerivde 之间是否连接成功，在connection相关的 callback 里, 我们在callback 里去业务。

//
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };





    //单手，点击，连续点击2下
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
//            lastTouchX = (int) event.getRawX();
//            lastTouchY = (int) event.getRawY();
            return true;
        }

        @Override //移动
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
//            int currentTouchX = (int) event2.getRawX();
//            int currentTouchY = (int) event2.getRawY();
//
//            int offsetX = currentTouchX - lastTouchX;
//            int offsetY = currentTouchY - lastTouchY;
//
//            zoominPiaoViewParams.leftMargin += offsetX;
//            zoominPiaoViewParams.topMargin += offsetY;
//            zoominPIaoView.setLayoutParams(zoominPiaoViewParams);
//
//            lastTouchX = currentTouchX;
//            lastTouchY = currentTouchY;

            return true;
        }


    }


//
//    private void updateZoom(float zoomRatio) {
//        if (cameraCaptureSession != null && cameraDevice != null) {
//            try {
//                CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//                captureBuilder.addTarget(new Surface(textureView1.getSurfaceTexture()));
//                captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio);
//                cameraCaptureSession.setRepeatingRequest(captureBuilder.build(), null, mBackgroundHandler);
//            } catch (CameraAccessException e) {
//                e.printStackTrace();
//            }
//        }
//    }

}


