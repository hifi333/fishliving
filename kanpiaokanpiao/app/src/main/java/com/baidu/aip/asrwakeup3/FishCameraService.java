package com.baidu.aip.asrwakeup3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FishCameraService extends Service {

    private static final String CHANNEL_ID = "CameraServiceChannel";
    private   String SERVER_IP =  "";  // ""192.168.141.139";  //小米的手机热点缺省ip, 这些缺省的ip 不准确， 修改为系统获取
    // private static final String SERVER_IP = "192.168.43.1";  //安卓的手机热点缺省ip
//    private static final String SERVER_IP = "192.168.0.111";  //独立wifi 分配的临时ip
    private static final int SERVER_PORT = 12345;

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private MediaCodec mediaCodec;
    private Surface encoderSurface;
    private Handler backgroundHandler;
    private Socket socket;
    private Surface previewSurface;

    private float currentZoomLevel = 1.0f;
    private float maxZoomLevel;
    private CaptureRequest.Builder captureBuilder;
    String cameraId ="";
    CameraManager manager = null;
    CameraCharacteristics cameraCharacteristics = null;


    private static String intToIp(int ipInt) {
        return (ipInt & 0xff) + "." +
                ((ipInt >> 8) & 0xff) + "." +
                ((ipInt >> 16) & 0xff) + "." +
                (ipInt >> 24 & 0xff);
    }

    @Override
    public void onCreate() {
        super.onCreate();


        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) { //我是client， 我是看漂的， 我必须打开wifi，连接的wifi是热点， 是另外一个抖音直播的手机且打开热点的
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            if (dhcpInfo != null) {
//                localip=  intToIp(dhcpInfo.ipAddress);
                SERVER_IP=  intToIp(dhcpInfo.serverAddress);

            }
        }


        createNotificationChannel();
        startForeground(1, getNotification());

        startBackgroundThread();
//        openCamera();   //缺省这里启动业务，但外部启动这个进程的时候，带过来有些核心参数， 我们在接收这个参数的地方启动这个业务。
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { //但外部启动这个进程的时候，带过来有些核心参数
//        SurfaceTexture surfaceTexture = intent.getParcelableExtra("surfaceTexture");
//        if (surfaceTexture != null) {
//            surfaceTexture.setDefaultBufferSize(1920, 1080);
//            previewSurface = new Surface(surfaceTexture);
//            openCamera();
//        }
        return START_STICKY;
    }


    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Camera Service")
                .setContentText("Running camera service in the background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Camera Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void startBackgroundThread() {
        HandlerThread thread = new HandlerThread("CameraBackground");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());
    }



    private void openCamera() {
        manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
            try {
                cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                maxZoomLevel = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            }catch (Exception ee) {ee.printStackTrace();}
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
            stopSelf();
        }
    };

    private void startPreview() {
        try {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size videoSize = new Size(1920, 1080);

            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", videoSize.getWidth(), videoSize.getHeight());
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 5000000);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoderSurface = mediaCodec.createInputSurface();
            mediaCodec.start();



            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureBuilder.addTarget(encoderSurface);

            captureBuilder.addTarget(previewSurface);


            cameraDevice.createCaptureSession(Arrays.asList( previewSurface,encoderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    try {
                        session.setRepeatingRequest(captureBuilder.build(), null, backgroundHandler);
                        startSocketThread();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(FishCameraService.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Socket zoompiaosocket = null;

    private class Server_connection_Runnable extends Thread {

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(2000);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }

                if (zoompiaosocket == null) {
                    try { //发起新连接
                        zoompiaosocket = new Socket(SERVER_IP, SERVER_PORT); //TCP 发送的， 每次发送的数据不丢，且多次发送的都是管道一样，保持顺序的
                        System.out.println("fish重新连接服务器成功！");

                    } catch (Exception e) {
                      //  e.printStackTrace();
                        try {
                            if (zoompiaosocket != null) {
                                zoompiaosocket.close();
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        //连接服务器失败： 为了简单请先启动开热点按个手机上的app2， 再来启动这个app1
                        System.out.println("fish连接失败里， 休息2秒，再次重新连接把");

                    }

                }
            }
        }
    }


    private class ClientHandler extends Thread {

        boolean fuck= true;

        public ClientHandler() {
        }

        public void run() {
            while (true) {
                if(zoompiaosocket == null) {
                    try {
                        // 让当前线程暂停20秒
                        Thread.sleep(1000 * 2);
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                        // 线程被中断时的处理代码
                    }
                }

                //连接成功了，开始工作。
                while (zoompiaosocket !=null) { //持续从本地摄像头（编码器里）读取数据，zoompiaosocket 发送出去
                    try {
                        // 使用 bufferInfo.size 来确定缓冲区中有效数据的大小
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo(); //一帧的缓冲区数据buffer，关键帧全量数据，其他长尾帧是变化的部分数据
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000); // 从解码器里获取一个帧，找到最新的一个帧的硬件缓存区的编号

                        if (outputBufferIndex >= 0) { //拿到有效的一帧数据
                            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex); //取出这个帧的缓冲区
                            byte[] buffer = new byte[bufferInfo.size]; //准备数据块，来存储帧数据
                            // System.out.println("数据帧大小:" + bufferInfo.size);
                            outputBuffer.get(buffer); //把当前摄像头里对应的编码器里一帧的数据，copy 出来到buffer里。
                            //数据帧大小:158732
                            //数据帧大小:421421 421kb,不到1M
                            outputBuffer.clear();

                            zoompiaosocket.getOutputStream().write(new byte[]{0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04}); // 这是我自己定的，几个数字。添加NALU起始码，有利于从流中分析出实际后面的帧数据块
                            zoompiaosocket.getOutputStream().write(buffer); //TCP 数据流发送出去，特定是顺序，不丢。。 但每次服务器读取的位置不确定的，类似一个绳子每次剪刀剪的地方不确定

//                            System.out.println("fish发送数据:" + "8");
//                            System.out.println("fish发送数据:" + bufferInfo.size);
                            //释放buffer，

                            //开始模拟不发送出去，本地的另外一个解码器消费，且在下面的展示出来。
                            //buffer 里到底放里多个字节呢？ 答案：bufferInfo.size
//                                    byte[] copy = new byte[buffer.length];
//                                    System.arraycopy(buffer, 0, copy, 0, buffer.length);
//                                    decode2localpreview(copy.length,copy); //这个是多此一举， 估计是浪费计算力，后面再优化把。
//                                    decode2localpreview(bufferInfo.size,buffer); //这个是多此一举， 估计是浪费计算力，后面再优化把。


                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        }

                    } catch (Exception e) {
                        // zoompiaosocket 发送过程异常
                        System.out.println("fish发送异常，重新连接");

                        zoompiaosocket  = null; //跳出while，进入顶层while 等待新的连接。
                        fuck = false;
                    }
                } //loop for this good connection

            }//用这个新的socket tcp 连接工作，期间异常后， 会在这个loop 里重新建立新的连接。        }
        }
    }



    private void startSocketThread() {

        new Server_connection_Runnable().start();
        new ClientHandler().start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeCamera();
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
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        FishCameraService getService() {
            return FishCameraService.this;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        if (surfaceTexture != null) {
            surfaceTexture.setDefaultBufferSize(1920, 1080);
            previewSurface = new Surface(surfaceTexture);
            openCamera();
        }
    }


    public void setZoom(float zoomLevel) {
        if (cameraDevice != null && captureBuilder != null && cameraCaptureSession != null) {
            try {
                float newZoomLevel = Math.max(1.0f, Math.min(zoomLevel, maxZoomLevel));
                Rect m = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                int cropW = (int) (m.width() / newZoomLevel);
                int cropH = (int) (m.height() / newZoomLevel);
                int cropX = (m.width() - cropW) / 2;
                int cropY = (m.height() - cropH) / 2;

                Rect zoomRect = new Rect(cropX, cropY, cropX + cropW, cropY + cropH);
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
                cameraCaptureSession.setRepeatingRequest(captureBuilder.build(), null, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

}
