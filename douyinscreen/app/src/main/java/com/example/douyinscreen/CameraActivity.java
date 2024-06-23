package com.example.douyinscreen;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
//    private static final String SERVER_IP = "192.168.0.109"; // 服务器IP地址
    private static final int SERVER_PORT_piao = 12345; // 服务器端口
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;



//    private TextureView textureView;
    private Handler mBackgroundHandler;
    private Socket socket;
    private TextureView bigbackgroundView;
    private TextureView zoominPIaoView;
    private TextureView personView;
    private CustomTextDrawView textDrawView;
    private ServerSocket serverSocket;
    private boolean ZoominPiao_isRunning = true;

    private StreamConfigurationMap myCameraConfigmap;
    private float minZoomRatio =1.0f;
    private float maxZoomRatio =0;
    private float currentZoomRatio = 1.0f;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private int lastTouchX, lastTouchY;
    private FrameLayout.LayoutParams zoominPiaoViewParams;
    private boolean tozoominflag = true;
    private boolean currentis_fishcatchingModel = false;

    private Paint paint;
    private String text = "Hello, World!";


    private Socket zoominPiaoClientSocket = null;
    private Socket personviewClientSocket = null;
    private Socket DrawTextClientSocket = null;

    Surface surfaceVideo_person =null; //这个很重要，是从对应View的渲染后的callback（会多次发生） 函数中获取更新的， 这个要关联到解码器上的
    Surface surfaceVideo_zoominpiao =null;

//    private Timer timer;
//    private TimerTask timerTask;
//
//    private void startTimer() {
//        timer = new Timer();
//        timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                Log.d(TAG, "Hello, this is printed every 10 seconds!");
//                setGUIfishCatching(isfishcatching);
//                isfishcatching = !isfishcatching;
//            }
//        };
//        timer.schedule(timerTask, 0, 30000);
//    }
//    private void stopTimer() {
//        if (timer != null) {
//            timer.cancel();
//            timer = null;
//        }
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//
        /*
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        setContentView(layout);

        // 创建第一个TextureView用于摄像头预览
        textureView1 = new TextureView(this);
        textureView1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        layout.addView(textureView1);

        // 创建第二个TextureView用于接收视频流
        textureView2 = new TextureView(this);
        textureView2.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        layout.addView(textureView2);

         */

        // 创建根布局 FrameLayout
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        // 创建用于全屏显示的 bigbackgroundView
        bigbackgroundView = new TextureView(this);
        FrameLayout.LayoutParams paramsTextureView2 = new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        rootLayout.addView(bigbackgroundView, paramsTextureView2);

        // 创建用于右上角显示的小 zoominPIaoView
        zoominPIaoView = new TextureView(this);
        zoominPiaoViewParams = new FrameLayout.LayoutParams(
                400, // 设置 textureView2 的宽度，可以根据需要调整
                600  // 设置 textureView2 的高度，可以根据需要调整
        );
        // 将 zoominPIaoView 定位到右上角
//        zoominPiaoViewParams.gravity = Gravity.TOP | Gravity.RIGHT;
        zoominPiaoViewParams.leftMargin = 100;
        zoominPiaoViewParams.topMargin = 100;
        rootLayout.addView(zoominPIaoView, zoominPiaoViewParams);

        // 创建CustomView并添加到FrameLayout
         textDrawView = new CustomTextDrawView(this);
        textDrawView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));
        textDrawView.bringToFront(); // 将CustomView置于TextureView之上
        rootLayout.addView(textDrawView);


        //创建一个临时浮动的80% 全屏且在最上面的一个View，展示抓鱼时刻的第一视觉的摄像头（挂在脖子下面那个）
        personView = new TextureView(this);

        personView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));

        rootLayout.addView(personView);

//
//        FrameLayout.LayoutParams paramsTextureView3 = new FrameLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
//
//        rootLayout.addView(personView, paramsTextureView3);

//        personView.bringToFront();
//        personView.setVisibility(View.GONE);

        // 将 rootLayout 设置为当前 Activity 的内容视图
        setContentView(rootLayout);


        // 初始化Paint对象
        paint = new Paint();
        paint.setColor(Color.WHITE); // 设置文字颜色
        paint.setTextSize(40); // 设置文字大小
        paint.setAlpha(128); // 设置文字透明度为50%
        paint.setAntiAlias(true); // 设置抗锯齿


        zoominPIaoView.setSurfaceTextureListener(surfaceTextureListener_zoominpiao);
        personView.setSurfaceTextureListener(surfaceTextureListener_personview);

//        personView.setSurfaceTextureListener(surfaceTextureListener_zoominpiao);
//        zoominPIaoView.setSurfaceTextureListener(surfaceTextureListener_personview);


        bigbackgroundView.setSurfaceTextureListener(surfaceTextureListener_bigbackground);



        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        gestureDetector = new GestureDetector(this, new GestureListener());

//        setGUIfishCatching(true);

        startBackgroundThread();
        start_DrawText_ServerSocket();

//
//        bigbackgroundView.setVisibility(View.INVISIBLE);
//        zoominPIaoView.setVisibility(View.INVISIBLE);
//
//        personView.setLayoutParams(new FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                Gravity.CENTER));
//        personView.bringToFront();

    }

    private void setGUIfishCatching(boolean isCatching){

        currentis_fishcatchingModel = isCatching;

        if(isCatching && personviewClientSocket!=null) {
            bigbackgroundView.setVisibility(View.INVISIBLE);
            zoominPIaoView.setVisibility(View.INVISIBLE);
            textDrawView.setVisibility(View.INVISIBLE);


            personView.setVisibility(View.VISIBLE); //可见
            personView.bringToFront(); //前面
        }else {
            personView.setVisibility(View.INVISIBLE);

            bigbackgroundView.setVisibility(View.VISIBLE); //可见
            zoominPIaoView.setVisibility(View.VISIBLE); //可见
            textDrawView.setVisibility(View.VISIBLE); //可见

            bigbackgroundView.bringToFront(); //前面
            zoominPIaoView.bringToFront(); //前面
            textDrawView.bringToFront();


        }

    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            lastTouchX = (int) event.getRawX();
            lastTouchY = (int) event.getRawY();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
            int currentTouchX = (int) event2.getRawX();
            int currentTouchY = (int) event2.getRawY();

            int offsetX = currentTouchX - lastTouchX;
            int offsetY = currentTouchY - lastTouchY;

            zoominPiaoViewParams.leftMargin += offsetX;
            zoominPiaoViewParams.topMargin += offsetY;
            zoominPIaoView.setLayoutParams(zoominPiaoViewParams);

            lastTouchX = currentTouchX;
            lastTouchY = currentTouchY;

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

    private TextureView.SurfaceTextureListener surfaceTextureListener_bigbackground = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            checkPermissions();
            // 当SurfaceTexture内容更新时调用
          //  Canvas canvas = bigbackgroundView.lockCanvas();
            //if (canvas != null) {
               // canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR); // 清除画布
               // canvas.drawText(text, 0, 0, paint); // 绘制文字
                //bigbackgroundView.unlockCanvasAndPost(canvas); // 解锁Canvas并提交绘制
           // }
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


    private TextureView.SurfaceTextureListener surfaceTextureListener_personview = new TextureView.SurfaceTextureListener() {

        //1When a TextureView is added to the view hierarchy and becomes visible
        //2device screen rotation
        //3When an activity is resumed, and the TextureView was previously paused or stopped, the SurfaceTexture becomes available again
        //Setting a TextureView to View.INVISIBLE and then back to View.VISIBLE does not trigger the onSurfaceTextureAvailable callback.
        @Override //这个函数的特效，如果当前View，被隐藏了，就不会触发这个函数，
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                ////远程来的数据，解码器后，绑定到这个view
                System.out.println("++++++++personView 又展示出来了，这个函数的特效，如果当前View，被隐藏了，就不会触发这个函数");
                //这个触发函数可能被多次触发， so不能在这里初始化服务器， 只能这里更新最新的Surface 给解码器。
                //只能初始化一次啊：

                Surface aa  = new Surface(surface);

                if(surfaceVideo_person == null) { //确保只初始化1次。
                    surfaceVideo_person = aa;
                    start_personview_ServerSocket(surfaceVideo_person);
                }else{
                    surfaceVideo_person = aa; //及时保持这个更新，方便后面的解码器也跟新这个关联
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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



    private TextureView.SurfaceTextureListener surfaceTextureListener_zoominpiao = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                ////远程来的数据，解码器后，绑定到这个view
                System.out.println("++++++++zoominPiaoView 又展示出来了，这个函数的特效，如果当前View，被隐藏了，就不会触发这个函数");

                Surface aa  = new Surface(surface);

                if(surfaceVideo_zoominpiao == null) { //确保只初始化1次。
                    surfaceVideo_zoominpiao = aa;
                    start_ZoominPiao_ServerSocket(surfaceVideo_zoominpiao);
                }else{
                    surfaceVideo_zoominpiao = aa; //及时保持这个更新，方便后面的解码器也跟新这个关联
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
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


    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera_bigbackgroundSurface();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera_bigbackgroundSurface();
            } else {
                Toast.makeText(this, "Permissions denied.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void openCamera_bigbackgroundSurface() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, mBackgroundHandler);

            /*
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            myCameraConfigmap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

//            Float maxZoomRatio = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE).getUpper();
//            boolean isZoomSupported = (maxZoomRatio > 1.0f);

            // 获取缩放比例范围,下面的代码要安卓SDKapi30版本后才支持，但我的华为p10的版本是29，不能用啊
            Range<Float> zoomRatioRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
            if (zoomRatioRange != null ) {
                minZoomRatio = zoomRatioRange.getLower(); // 最小缩放比例
                maxZoomRatio = zoomRatioRange.getUpper(); // 最大缩放比例

                // 检查是否支持变焦
                boolean isZoomSupported = (minZoomRatio < maxZoomRatio);
                if (isZoomSupported) {
                    // 可以进行变焦操作
                }
            }
*/


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview_bigbackgroundSurface();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            finish();
        }
    };

    private void startPreview_bigbackgroundSurface() {
        try {
            SurfaceTexture texture1 = bigbackgroundView.getSurfaceTexture();
            assert texture1 != null;
            texture1.setDefaultBufferSize(400, 600); // 设置预览大小
            Surface bigbackgroundSurface = new Surface(texture1);

            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureBuilder.addTarget(bigbackgroundSurface);

//
//            //准备放大，看漂！
//            if (maxZoomRatio >0) {
//                captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, maxZoomRatio);
//            }


            cameraDevice.createCaptureSession(Arrays.asList(bigbackgroundSurface /*, encoderSurface*/), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    try {
                        session.setRepeatingRequest(captureBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        HandlerThread thread = new HandlerThread("Camera Background");
        thread.start();
        mBackgroundHandler = new Handler(thread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandler.getLooper().quitSafely();
        try {
            mBackgroundHandler.getLooper().getThread().join();
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        isRunning = false;
//        if (serverSocket != null) {
//            try {
//                serverSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        if (zoominPiao_Decocode_mediaCodec2 != null) {
//            try {
////                zoominPiao_Decocode_mediaCodec2.stop();
////            zoominPiao_Decocode_mediaCodec2.release();
////            zoominPiao_Decocode_mediaCodec2 = null;
//            }catch (Exception e) {e.printStackTrace();}


//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (zoominPiao_Decocode_mediaCodec2 != null) {
//            try {
////                zoominPiao_Decocode_mediaCodec2.start();
//            }catch (Exception e) {e.printStackTrace();}
//        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  //不要关闭屏幕，保持一直摄像
    }
    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
//        if (zoominPiao_Decocode_mediaCodec2 != null) {
//            zoominPiao_Decocode_mediaCodec2.stop();
//            zoominPiao_Decocode_mediaCodec2.release();
//            zoominPiao_Decocode_mediaCodec2 = null;
//        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (zoominPiao_Decocode_mediaCodec2 != null) {
//            try {
//                zoominPiao_Decocode_mediaCodec2.stop();
//                zoominPiao_Decocode_mediaCodec2.release();
//            } catch (IllegalStateException e) {
//                e.printStackTrace();
//            }
//        }
    }



private void start_ZoominPiao_ServerSocket(Surface surface) {

    new Decode_thread_ClientHandler(surface,true).start(); //整个app，固定一个线程来处理接收数据，和解码数据到View
    //整个线程里初始化私有的解码器，避免解码器遇到多线程问题。 另外从解码器里申请一个InputbufferIndex，
    // dequeueInputBuffer（放收到的视频数据进去），此时需要设定timeout时间，可以避免线程block住导致画面block住
    //整个线程应该长期运行在：  at java.net.SocketInputStream.read

    try {
        Thread.sleep(2000);
    }catch (Exception ee) {ee.printStackTrace();}

    //启动这个线程， 持续的监听这个端口，等待一个特定的业务client，就是放大漂的视频数据，
    //用线程来监听，而不是一次性的， 是为了放大漂的client 会启动/关闭，重复连接。
    new Thread(new ZoominPiao_ServerRunnable()).start(); //一个接收TCP端口为12345对应zoominpiao的client 的连接线程。收到连接就交给别人处理，


}


    private void start_personview_ServerSocket(Surface surface) {

        new Decode_thread_ClientHandler(surface,false).start(); //整个app，固定一个线程来处理接收数据，和解码数据到View
        //整个线程里初始化私有的解码器，避免解码器遇到多线程问题。 另外从解码器里申请一个InputbufferIndex，
        // dequeueInputBuffer（放收到的视频数据进去），此时需要设定timeout时间，可以避免线程block住导致画面block住
        //整个线程应该长期运行在：  at java.net.SocketInputStream.read


        //启动这个线程， 持续的监听这个端口，等待一个特定的业务client，就是放大漂的视频数据，
        //用线程来监听，而不是一次性的， 是为了放大漂的client 会启动/关闭，重复连接。
        new Thread(new persionview_ServerRunnable()).start(); //一个接收TCP端口为12345对应zoominpiao的client 的连接线程。收到连接就交给别人处理，


    }

    private void start_DrawText_ServerSocket() {

        //启动这个线程， 持续的监听这个端口，等待一个特定的业务client，就是放大漂的视频数据，
        //用线程来监听，而不是一次性的， 是为了放大漂的client 会启动/关闭，重复连接。
        new Thread(new DrawText_ServerRunnable()).start(); //一个接收TCP端口为12345对应zoominpiao的client 的连接线程。收到连接就交给别人处理，


    }


    //UDP server
    private class DrawText_ServerRunnable implements Runnable {
        private int thisserverport =12347;

        @Override
        public void run() {
            try  {
                DatagramSocket  socket = new DatagramSocket(thisserverport);
                Log.d(TAG, "UDP Server started on port " + thisserverport);
                while (true) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());
                        Log.d(TAG, "Received: " + message);

                        //处理发来的指令，更新当前douyinScreen
                        if(message.equalsIgnoreCase("fishcatching")){

                            //临时切换以下屏幕
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setGUIfishCatching(true);
                                }
                            });
                            //end

                        }else  if(message.equalsIgnoreCase("kanpiao")){

                            //临时切换以下屏幕
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setGUIfishCatching(false);
                                }
                            });
                            //end
                        }else {

                            textDrawView.showtextlist[0] = message;
                            textDrawView.invalidate();

                        }


                    }catch (Exception eee) {eee.printStackTrace();}

                }
            } catch (IOException ex) {
                Log.e(TAG, "Server exception: " + ex.getMessage(), ex);
            }
        }
    }

    private class ZoominPiao_ServerRunnable implements Runnable {
        private int thisserverport =12345;

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(thisserverport)) {
                System.out.println("Server startup with port:" + thisserverport);
                while (true) {
                    Socket socket = serverSocket.accept();
                    zoominPiaoClientSocket = socket;
                    Log.d(TAG, "New client connected for : " + thisserverport);
                }
            } catch (IOException ex) {
                Log.e(TAG, "Server exception: " + ex.getMessage(), ex);
            }
        }
    }

    private class persionview_ServerRunnable implements Runnable {
        private int thisserverport =12346;

        @Override
        public void run() {
            try {
                Thread.sleep(2000);
            }catch (Exception ee) {ee.printStackTrace();}

            try (ServerSocket serverSocket = new ServerSocket(thisserverport)) {
                System.out.println("personserver startup with port:" + thisserverport);
                while (true) {
                    Socket socket = serverSocket.accept();
                    personviewClientSocket = socket;



                    Log.d(TAG, "personserver New client connected for : " + thisserverport);
                }
            } catch (IOException ex) {
                Log.e(TAG, "Server exception: " + ex.getMessage(), ex);
            }
        }
    }

//
//    private class DrawText_ClientHandler extends Thread {
//
//        public void run() {
//            byte[] tcpbuffer = new byte[65536];
//            while (true) {
//
//                if(DrawTextClientSocket == null){  //当前没有可用的连接， 就sleep吧
//                    try {
//                        Thread.sleep(1000);
////                        System.out.println("当前没有可用的连接， 就sleep吧");
//                        continue;
//                    }catch (Exception ee ){ee.printStackTrace();}
//                }
//
//                try {
//                    InputStream inputStream = DrawTextClientSocket.getInputStream();
//                    int bytesRead = inputStream.read(tcpbuffer, 0, tcpbuffer.length);
//                    String receivedString = new String(tcpbuffer, 0, bytesRead, StandardCharsets.UTF_8);
//                    System.out.println("++++++++++++++++" +receivedString);
//
//                }catch (Exception ee) {ee.printStackTrace();}
//
//
//
//            }
//        }
//    }

        private class Decode_thread_ClientHandler extends Thread {
        private MediaCodec xDecocode_mediaCodec2;
        private Surface thisSurfaceTexture;
        private boolean thisisPiaoNotPersonView = true;
        public void initStartEncoder(){

            try {
                xDecocode_mediaCodec2 = MediaCodec.createDecoderByType("video/avc");
                MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//            format.setInteger(MediaFormat.KEY_BIT_RATE, 5000000); // 5 Mbps
//            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // I帧间隔1秒

                format.setInteger(MediaFormat.KEY_ROTATION, 90); //图像右转90度

                if(thisSurfaceTexture==null) {
                    System.out.println("zoominPiao_surfaceTexture is null");
                }
                //thisSurfaceTexture 如果该app发生生命周期的暂停/继续，这个对象会改变的，需要及时更新/关联到这个解码器上
                xDecocode_mediaCodec2.configure(format, thisSurfaceTexture, null, 0);
                xDecocode_mediaCodec2.start(); //需要在同一个线程里，初始化，和后面的日常使用。

                // 设置 TextureView 的旋转
//            Matrix matrix = new Matrix();
//            matrix.postRotate(90);
//            zoominPIaoView.setTransform(matrix);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void releaseEncoder(){ //这个方法有问题，需要解决
            if (xDecocode_mediaCodec2 != null) {
                try {
                   // zoominPiao_Decocode_mediaCodec2.stop();
//                    zoominPiao_Decocode_mediaCodec2.release();
                    System.out.println("releaseEncoder released");
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

                System.out.println("in releaseEncoder");
            }
        }
        public Decode_thread_ClientHandler(Surface surfaceTexture,boolean isPiaoNotPersonView) {
            thisisPiaoNotPersonView = isPiaoNotPersonView;
            thisSurfaceTexture = surfaceTexture;
            this.setName("fish_ZoominPiao_ClientHandler");

            initStartEncoder();


        }

        public void run() {
            byte[] tcpbuffer = new byte[65536]; //65K空间,且这个不能很大，网卡接收不住，存储不下1帧的图像数据（400KB 到 5M）， 所以每次读取的数据只能是一帧的部分数据啊。 且里面最多有N帧数据， N-2个帧的完整数据，头尾都是帧的部分数据 也就是说里面包括n-2个帧的开头标记！
            byte[] tobeHandPartImageBuffer = new byte[6553600];//6.5M, 大于 一帧的数据上限要求. 存储在本地的一帧数据的部分，等凑满一帧后，才处理。 啥时候凑满呢，答： 遇到下一帧的开头标记
            int    tobeHandPartImageBuffer_len = 0; //为了从tcp的流中，切割到一个完整的帧，这个帧的长度。
            Socket thisSocket = null;

            int framestartflaglen= 8;
            while (true) {

                if(thisisPiaoNotPersonView)
                    thisSocket = zoominPiaoClientSocket;
                else
                    thisSocket = personviewClientSocket;

                if(currentis_fishcatchingModel && thisisPiaoNotPersonView) {
                    try {
                        Thread.sleep(2*1000);
                    }catch (Exception ee ){ee.printStackTrace();}

                    continue; //不处理啊， 因为piaoView 当前是隐藏的哦。 处理也是浪费cpu时间

                }else  if(!currentis_fishcatchingModel && !thisisPiaoNotPersonView) {
                    try {
                        Thread.sleep(2*1000);
                    }catch (Exception ee ){ee.printStackTrace();}

                    continue; //不处理啊，
                }



                if(thisSocket == null){  //当前没有可用的连接， 就sleep吧
                    try {
                        System.out.println("当前线程看漂：" + thisisPiaoNotPersonView + "  socket == null, sleep 等待连接");
                        Thread.sleep(2*1000);
                    }catch (Exception ee ){ee.printStackTrace();}

                    continue;

                }

                try {
                    InputStream inputStream = thisSocket.getInputStream();
                    int bytesRead = inputStream.read(tcpbuffer, 0, tcpbuffer.length);

                    //System.out.println("当前线程看漂：" + thisisPiaoNotPersonView + "  读取到数据：" + bytesRead);


                    int streamSearchIndex = 0;// 100000000, 当前这次tcp流的遍历index

                    while (streamSearchIndex < bytesRead) { ////从绳子上剪到一批数据啦
                        // 查找NALU起始码
                        int naluStartIndex = findNalUnitStartCode(tcpbuffer, streamSearchIndex, bytesRead);
                        if (naluStartIndex == 0) {
                            // 刚好本次流的开始就是一帧的开始， 那么也是上一个帧的结尾。 我们看看我们剪下来的上一帧缓存是否在，要是在，就处理上一个NALU单元, 上一个刚好是完整的一个帧啊
                            if (tobeHandPartImageBuffer_len > 0) {
                                try {
                                    decocode_processNalUnit(xDecocode_mediaCodec2, tobeHandPartImageBuffer, tobeHandPartImageBuffer_len);
                                    tobeHandPartImageBuffer_len = 0; //帧缓存reset
                                } catch (IllegalStateException e) {
                                    e.printStackTrace();
                                    // releaseEncoder();
                                    // initStartEncoder();
                                }

                            }
                            //这个标记后面的数据要继续分析啊，其中有可能还是包含framestart 标记啊
                            streamSearchIndex = naluStartIndex + framestartflaglen; //往后继续推进，跳过这个标记啊

                        }else if (naluStartIndex > 0) {
                            // 将起始码之前的数据复制到NALU缓冲区
                            int length = naluStartIndex - streamSearchIndex;
                            if (length > 0) {
                                System.arraycopy(tcpbuffer, streamSearchIndex, tobeHandPartImageBuffer, tobeHandPartImageBuffer_len, length);
                                tobeHandPartImageBuffer_len += length;
                                //这下又得到一个完整的帧数据啦，赶快处理
                                if (tobeHandPartImageBuffer_len > 0) {
                                    try{
                                     decocode_processNalUnit(xDecocode_mediaCodec2,tobeHandPartImageBuffer, tobeHandPartImageBuffer_len);
                                     tobeHandPartImageBuffer_len = 0;
                                    } catch (IllegalStateException e) {
                                        e.printStackTrace();
                                       // releaseEncoder();
                                        //initStartEncoder();
                                    }

                                }
                            }

                            // 这个标记后面的数据要继续分析啊，其中有可能还是包含framestart 标记啊
                            streamSearchIndex = naluStartIndex + framestartflaglen; //往后继续推进，跳过这个标记啊
                        } else {
                            // 本次剪刀剪出的内容里，没有找到一个完整的帧标记啊， 看来这个数据是上一个帧的部分body内容里，把这个数据复制到NALU缓冲区
                            int length = bytesRead - streamSearchIndex;
                            if (length > 0) {
                                System.arraycopy(tcpbuffer, streamSearchIndex, tobeHandPartImageBuffer, tobeHandPartImageBuffer_len, length);
                                tobeHandPartImageBuffer_len += length;
                            }
                            streamSearchIndex = bytesRead; //退出本次剪刀剪出的数据的处理
                        }
                    } //继续循环分析这次socket 读取数据数组。。


                }catch (IOException e) {
                    e.printStackTrace();  //如果直播过程中，网络问题，该client 连接问题出现，这里就异常，然后这个线程结束，
                    System.out.println("fish: 当前这个zoominpiaoclient 读取异常，让对应的处理线程退出");
//                    isrunning = false; //连接中断， 这个线程就退出运行把。
                    if(thisisPiaoNotPersonView)
                         zoominPiaoClientSocket = null;
                    else
                         personviewClientSocket = null;                }

            } //while(true)

//            System.out.println("fish: zoomin处理线程退出了,88");
//            try {
//                zoominPiao_Decocode_mediaCodec2.stop();
//                zoominPiao_Decocode_mediaCodec2.release();
//            }catch (Exception ee) {ee.printStackTrace();}

        }
    }




    private int findNalUnitStartCode(byte[] buffer, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex - 8; i++) {
            if (    buffer[i] == 0x00
                    && buffer[i + 1] == 0x00
                    && buffer[i + 2] == 0x00
                    && buffer[i + 3] == 0x00
                    && buffer[i + 4] == 0x01
                    && buffer[i + 5] == 0x02
                    && buffer[i + 6] == 0x03
                    && buffer[i + 7] == 0x04
            ) {
                return i;
            }
        }
        return -1;
    }

    private void decocode_processNalUnit(MediaCodec mediaCodec_, byte[] buffer, int bytesRead) { //解码器，解析每帧数据，自动播放到关联的View上


                int inputBufferIndex = mediaCodec_.dequeueInputBuffer(1000); //1000，表示timeout 时间，避免block// -1表示没有timeout时间，本线程同步等待这里，然后漂画面不动了
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = mediaCodec_.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(buffer, 0, bytesRead);
                    mediaCodec_.queueInputBuffer(inputBufferIndex, 0, bytesRead, System.nanoTime() / 1000, 0);
                }

                MediaCodec.BufferInfo bufferInfo2 = new MediaCodec.BufferInfo();
                int outputBufferIndex2 = mediaCodec_.dequeueOutputBuffer(bufferInfo2, 0);
                while (outputBufferIndex2 >= 0) {
                    mediaCodec_.releaseOutputBuffer(outputBufferIndex2, true);
                    outputBufferIndex2 = mediaCodec_.dequeueOutputBuffer(bufferInfo2, 0);
                }

    }


    private void closeServerSocket() {

    }



    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
//            currentZoomRatio *= detector.getScaleFactor();
//            currentZoomRatio = Math.max(minZoomRatio, Math.min(currentZoomRatio, maxZoomRatio));
//            updateZoom(currentZoomRatio);

            float scaleFactor = detector.getScaleFactor();

            // 调整zoomInView的大小
            zoominPiaoViewParams.width = (int) (zoominPIaoView.getWidth() * scaleFactor);
            zoominPiaoViewParams.height = (int) (zoominPIaoView.getHeight() * scaleFactor);

            // 限制缩放比例
            zoominPiaoViewParams.width = Math.max(200, Math.min(zoominPiaoViewParams.width, 2000));
            zoominPiaoViewParams.height = Math.max(300, Math.min(zoominPiaoViewParams.height, 2000));

            zoominPIaoView.setLayoutParams(zoominPiaoViewParams);
            return true;

        }
    }

    private void updateZoom(float zoomRatio) {
        if (cameraCaptureSession != null && cameraDevice != null) {
            try {
                CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                captureBuilder.addTarget(new Surface(zoominPIaoView.getSurfaceTexture()));
                captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio);
                cameraCaptureSession.setRepeatingRequest(captureBuilder.build(), null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }



}