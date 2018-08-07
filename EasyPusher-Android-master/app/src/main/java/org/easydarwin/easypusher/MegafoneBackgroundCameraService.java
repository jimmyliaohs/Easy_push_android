package org.easydarwin.easypusher;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import org.easydarwin.push.EasyPusher;
import org.easydarwin.push.InitCallback;
import org.easydarwin.push.MediaStream;

import java.io.File;

public class MegafoneBackgroundCameraService extends Service implements TextureView.SurfaceTextureListener {
    private static final int NOTIFICATION_ID = 3001;
    private int width = 640, height = 480;//默认分辨率
    private static final String RECORD_DIR = "/megafone";

    private final TimeTickReceiver timeTickReceiver = new TimeTickReceiver();

    private TextureView textureView;
    private WindowManager windowManager;

    private MediaStream mMediaStream;
    private File record_dir;

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Background Video Recorder")
                .setContentText("")
//                .setSmallIcon(R.drawable.button_selector)
                .build();
        startForeground(NOTIFICATION_ID, notification);

        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(100, 100, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT);


                layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
                windowManager.addView(textureView, layoutParams);
            }
        } else {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(100, 100, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            windowManager.addView(textureView, layoutParams);
        }


        if (textureView.isAvailable()) {
            goonWithAvailableTexture(textureView.getSurfaceTexture());
        }

        IntentFilter timefilter = new IntentFilter();
        timefilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(timeTickReceiver, timefilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

      /*  boolean isStreaming = mMediaStream != null && mMediaStream.isStreaming();
        if (isStreaming) {
            mMediaStream.stopStream();
            mMediaStream = null;
        }

        if (textureView != null) {
            if (textureView.getParent() != null) {
                windowManager.removeView(textureView);
            }
        }

        unregisterReceiver(timeTickReceiver);
        stopForeground(true);
        stopSelf();*/
    }


    private void goonWithAvailableTexture(SurfaceTexture surface) {
        if (record_dir == null) {
            record_dir = new File(Environment.getExternalStorageDirectory() + RECORD_DIR);
        }
        if (!record_dir.exists()) {
            record_dir.mkdir();
        }
        if (mMediaStream != null) {
            mMediaStream.stopPreview();
            mMediaStream.setSurfaceTexture(surface);
            mMediaStream.startPreview();
            if (mMediaStream.isStreaming()) {
                String ip = EasyApplication.getEasyApplication().getIp();
                String port = EasyApplication.getEasyApplication().getPort();
                String id = EasyApplication.getEasyApplication().getId();
                String url = String.format("rtsp://%s:%s/%s.sdp", ip, port, id);
//                txtStreamAddress.setText(url);
            }
        } else {
            mMediaStream = new MediaStream(getApplicationContext(), surface, PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(EasyApplication.KEY_ENABLE_VIDEO, true));
            mMediaStream.setRecordPath(record_dir.getPath());
            startCamera();
        }
    }


    private void startCamera() {
        mMediaStream.updateResolution(width, height);
        mMediaStream.setDgree(getDgree());
        mMediaStream.createCamera();
        mMediaStream.startPreview();
        if (mMediaStream.isStreaming()) {
                String ip = EasyApplication.getEasyApplication().getIp();
                String port = EasyApplication.getEasyApplication().getPort();
                String id = EasyApplication.getEasyApplication().getId();
//                txtStreamAddress.setText(String.format("rtsp://%s:%s/%s.sdp", ip, port, id));
        }
    }

    private int getDgree() {
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }
        return degrees;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        goonWithAvailableTexture(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    private void record() {
        if (mMediaStream != null) {
            if (mMediaStream.isRecording()) {
                mMediaStream.stopRecord();
            } else {
                mMediaStream.startRecord();
            }
        }
    }

    public void pushStream() {
        if (!mMediaStream.isStreaming()) {
            String url = null;
            String ip = EasyApplication.getEasyApplication().getIp();
            String port = EasyApplication.getEasyApplication().getPort();
            String id = "123456789";/*EasyApplication.getEasyApplication().getId();*/
            mMediaStream.startStream(ip, port, id, new InitCallback() {
                @Override
                public void onCallback(int code) {
                    switch (code) {
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_INVALID_KEY:
//                            sendMessage("无效Key");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_SUCCESS:
//                            sendMessage("激活成功");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_CONNECTING:
//                            sendMessage("连接中");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_CONNECTED:
//                            sendMessage("连接成功");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_CONNECT_FAILED:
//                            sendMessage("连接失败");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_CONNECT_ABORT:
//                            sendMessage("连接异常中断");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_PUSHING:
//                            sendMessage("推流中");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_DISCONNECTED:
//                            sendMessage("断开连接");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_PLATFORM_ERR:
//                            sendMessage("平台不匹配");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_COMPANY_ID_LEN_ERR:
//                            sendMessage("断授权使用商不匹配");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_PROCESS_NAME_LEN_ERR:
//                            sendMessage("进程名称长度不匹配");
                            break;
                    }
                }
            });
//            url = String.format("rtsp://%s:%s/%s.sdp", ip, port, id);
//            btnSwitch.setText("停止");
//            txtStreamAddress.setText(url);
        } else {
            mMediaStream.stopStream();
//            btnSwitch.setText("开始");
//            sendMessage("断开连接");
        }
    }

    private long push_begin = 0;
    private long push_last = 0;

    private boolean isFirstPush = true;

    class TimeTickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            push_begin = System.currentTimeMillis();

            if (isFirstPush) {
                push_last = push_begin;
                isFirstPush = false;
            }

            long duration = push_begin - push_last;
            if (duration > 60 * 60 * 1000 || duration == 0) {
                pushStream();
                push_last = push_begin;
            }

        }
    }

}
