package org.easydarwin.easypusher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.easydarwin.push.EasyPusher;
import org.easydarwin.push.InitCallback;
import org.easydarwin.push.MediaStream;

import java.io.File;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.easydarwin.easypusher.EasyApplication.BUS;

public class MegafoneActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    public static final int REQUEST_PERMISSION = 2001;
    private static final String RECORD_DIR = "/megafone";

    private boolean mNeedGrantedPermission;

    private TextureView textureView;
    private MediaStream mMediaStream;
    private File record_dir;
    //默认分辨率
    private int width = 640, height = 480;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_megafone);
        BUS.register(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REQUEST_PUSH_RECORD);
        registerReceiver(broadcast, filter);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED
                ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            mNeedGrantedPermission = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("lhs", "onResume()");
        if (!mNeedGrantedPermission) {
            goonWithPermissionGranted();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("lhs","onDestroy()");
        BUS.unregister(this);
        unregisterReceiver(broadcast);
        boolean isStreaming = mMediaStream != null && mMediaStream.isStreaming();
        if (isStreaming) {
            mMediaStream.stopStream();
            mMediaStream = null;
        }
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
            String id = "123456";/*EasyApplication.getEasyApplication().getId();*/
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


    private void goonWithPermissionGranted() {
        textureView = (TextureView) findViewById(R.id.texture_view_megafone);
        textureView.setSurfaceTextureListener(this);

        if(mMediaStream != null && mMediaStream.isStreaming()){
            return;
        }else{
            if(textureView.isAvailable()){
                goonWithAvailableTexture(textureView.getSurfaceTexture());
            }
        }
    }


    private void goonWithAvailableTexture(SurfaceTexture surface) {
        if (record_dir == null) {
            record_dir = new File(Environment.getExternalStorageDirectory() + RECORD_DIR);
        }
        if (!record_dir.exists()) {
            record_dir.mkdir();
        }
//        MediaStream ms = mService.getMediaStream();
        if (mMediaStream != null) {    // switch from background to front
           //lhs modify begin
            mMediaStream.stopPreview();
            //lhs modify end
//            mService.inActivePreview();
            mMediaStream.setSurfaceTexture(surface);
            //lhs modify begin
            mMediaStream.startPreview();
            //lhs modify end
            if (mMediaStream.isStreaming()) {
                String ip = EasyApplication.getEasyApplication().getIp();
                String port = EasyApplication.getEasyApplication().getPort();
                String id = EasyApplication.getEasyApplication().getId();
                String url = String.format("rtsp://%s:%s/%s.sdp", ip, port, id);
                if (EasyApplication.isRTMP()) {
                    url = EasyApplication.getEasyApplication().getUrl();
                }
//                btnSwitch.setText("停止");
//                txtStreamAddress.setText(url);
//                sendMessage("推流中");
            }
        } else {
            mMediaStream = new MediaStream(getApplicationContext(), surface, PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(EasyApplication.KEY_ENABLE_VIDEO, true));
            mMediaStream.setRecordPath(record_dir.getPath());
//            mMediaStream = ms;
            startCamera();
//            mService.setMediaStream(ms);
        }
    }

    private void startCamera() {
        mMediaStream.updateResolution(width, height);
        mMediaStream.setDgree(getDgree());
        mMediaStream.createCamera();
        //lhs modify begin
        mMediaStream.startPreview();
        //lhs modify end
        if (mMediaStream.isStreaming()) {
//            sendMessage("推流中");
//            btnSwitch.setText("停止");
            if (EasyApplication.isRTMP()) {
//                txtStreamAddress.setText(EasyApplication.getEasyApplication().getUrl());
            } else {
                String ip = EasyApplication.getEasyApplication().getIp();
                String port = EasyApplication.getEasyApplication().getPort();
                String id = EasyApplication.getEasyApplication().getId();
//                txtStreamAddress.setText(String.format("rtsp://%s:%s/%s.sdp", ip, port, id));
            }
        }
    }

    private int getDgree() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
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
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                if (grantResults.length > 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("lhs", "onRequestPermissionsResult()");
                    mNeedGrantedPermission = false;
                    goonWithPermissionGranted();
                } else {
                    finish();
                }
                break;
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Log.d("lhs", "onSurfaceTextureAvailable");
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

    private static final String ACTION_REQUEST_PUSH_RECORD = "action_request_push_record";
    private final PushRecordBroadcast broadcast = new PushRecordBroadcast();

    class PushRecordBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REQUEST_PUSH_RECORD.equals(intent.getAction())) {
                pushStream();
                record();
            }
        }
    }


}
