package com.example.backgroundsendpiaoview;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.IBinder;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.backgroundsendpiaoview.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private CameraService cameraService;
    private boolean isBound = false;
    SurfaceTexture previewsurface = null;

//    private ScaleGestureDetector scaleGestureDetector;
//    private GestureDetector gestureDetector;

    private ScaleGestureDetector scaleGestureDetector;
    private float zoomLevel = 1.0f;


    private String localip = "";
    private String apserverip = "";
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CameraService.LocalBinder binder = (CameraService.LocalBinder) service;
            cameraService = binder.getService();
            isBound = true;
            if(previewsurface !=null) {
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        /*
        try {
            // Check if the Wi-Fi hotspot is enabled
            Method isWifiApEnabledMethod = wifiManager.getClass().getMethod("isWifiApEnabled");
            boolean isWifiApEnabled = (boolean) isWifiApEnabledMethod.invoke(wifiManager);

            if (isWifiApEnabled) {
                // Default IP address for Android hotspot
                String defaultIpAddress = "192.168.43.1";

                // Retrieve DHCP information
                Method getDhcpInfoMethod = wifiManager.getClass().getMethod("getDhcpInfo");
                Object dhcpInfo = getDhcpInfoMethod.invoke(wifiManager);
                int ipAddress = (int) dhcpInfo.getClass().getField("serverAddress").get(dhcpInfo);
                localip=  intToIp(ipAddress);
                //Ap model， 可以进到这里， 但返回的ip是0

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
*/



        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        setContentView(layout);

        // 创建第一个TextureView用于摄像头预览
        TextureView textureView1 = new TextureView(this);
        textureView1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        layout.addView(textureView1);
        textureView1.setSurfaceTextureListener(surfaceTextureListener);


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


    }


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            previewsurface = surface;
//            startCameraService(surface);

            // 绑定服务并传递 SurfaceTexture
            Intent serviceIntent = new Intent(MainActivity.this, CameraService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE); //guileader 和 subSerivde 之间是否连接成功，在connection相关的 callback 里, 我们在callback 里去业务。

//
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    /*
    private void startCameraService(SurfaceTexture surfaceTexture) {
        Intent serviceIntent = new Intent(this, CameraService.class);
        serviceIntent.putExtra("surfaceTexture", surfaceTexture);  //这个方式不可以，putExtra 只能传递基本的数据类型，不是序列化传递对象啊
        //要在guileader 和 service 直接传递对象（类型rpc call）， 需要用另外一个技术： ServiceConnection

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

     */



//
//
//    @Override
//    public boolean onTouchEvent(android.view.MotionEvent event) {
//        scaleGestureDetector.onTouchEvent(event);
//        gestureDetector.onTouchEvent(event);
//
//        return true;
//    }
//
//    //双手操作，size 放大，或者zoom in/zoom out
//    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
//        @Override
//        public boolean onScale(ScaleGestureDetector detector) {
//            float thisscale =  detector.getScaleFactor();
//
//            if (thisscale >=1) { //放大，直到最大
//                currentZoomRatio = currentZoomRatio * thisscale *1.05f;
//                currentZoomRatio = Math.max(minZoomRatio, Math.min(currentZoomRatio, maxZoomRatio));
//                updateZoom(currentZoomRatio);
//            }else{
//                currentZoomRatio = currentZoomRatio * thisscale * thisscale; //相乘，为了效果明显
//                currentZoomRatio = Math.max(minZoomRatio, Math.min(currentZoomRatio, maxZoomRatio));
//                updateZoom(currentZoomRatio);
//
//            }
//            return true;
//        }
//    }

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

        /*
        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (tozoominflag && currentZoomRatio < maxZoomRatio) {
                if(currentZoomRatio +1 < maxZoomRatio) {
                    currentZoomRatio = currentZoomRatio + 1f;
                }else {
                    currentZoomRatio = maxZoomRatio;
                    tozoominflag = false;
                }
            } else {
                if(currentZoomRatio -1 >= minZoomRatio) {
                    currentZoomRatio = currentZoomRatio - 1f;
                }else {
                    currentZoomRatio = minZoomRatio;
                    tozoominflag = true;
                }

            }
            updateZoom(currentZoomRatio);
            return true;
        }

*/
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