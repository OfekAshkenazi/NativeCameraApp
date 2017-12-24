package com.example.ofek.nativecameraapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import Helpers.PermissionHelper;
import Runnables.ImageSaver;

public class MainActivity extends AppCompatActivity implements ImageSaver.OnImageSaved {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    public static final String FILE_PATH_TAG = "path";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    private static final String TAG = "Camera Debug :";
    private static final int STATE_PREVIEW = 8;
    private static final int STATE_WAIT_LOCK = 79;
    FloatingActionButton takePicBtn;
    TextureView cameraSurfaceTV;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private Size previewSize;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CaptureRequest captureRequest;
    private CameraCaptureSession captureSession;
    private HandlerThread bt;
    private Handler btHandler;
    private CaptureRequest.Builder captureRequestBuilder;
    static File imageFile;
    ImageReader imageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionHelper.getCameraPermission(this);
        PermissionHelper.getExternalStoragePermission(this);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED&&ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)
            setViews();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==PermissionHelper.REQUEST_BOTH&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&grantResults[1]==PackageManager.PERMISSION_GRANTED){
            setViews();
        }
    }

    private void setBackgroundThread() {
        bt=new HandlerThread("Camera Background Thread");
        bt.start();
        btHandler=new Handler(bt.getLooper());
    }
    private void closeBackgroundThread(){
        bt.quitSafely();
        try {
            bt.join();
            bt=null;
            btHandler=null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraSurfaceTV==null){
            return;
        }
        setBackgroundThread();
        if (cameraSurfaceTV.isAvailable()) {
            setupCamera(previewSize.getWidth(),previewSize.getHeight());
            openCamera();
        } else {
            cameraSurfaceTV.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        closeBackgroundThread();
        super.onPause();
    }

    private void closeCamera() {
        if (captureSession!=null) {
            captureSession.close();
            captureSession=null;
        }
        if (cameraDevice!=null){
            cameraDevice.close();
            cameraDevice=null;
        }
        if (imageReader!=null){
            imageReader.close();
            imageReader=null;
        }
    }

    private void setViews() {
        cameraSurfaceTV = findViewById(R.id.cameraSurface);
        takePicBtn = findViewById(R.id.takePicBtn);
        takePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto(view);

            }
        });
        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                setupCamera(width, height);
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };
        cameraSurfaceTV.setSurfaceTextureListener(surfaceTextureListener);
    }

    private void takePhoto(View view) {
        imageFile=createImageFile();
        lockFocus();
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String fileName="";
        String fileType ="";
        String dir="";
        fileName="IMG_"+timeStamp;
        fileType=".jpeg";
        dir= Environment.DIRECTORY_PICTURES;
        File storageDir = Environment.getExternalStoragePublicDirectory(dir);
        return new File(storageDir,fileName+fileType);
    }

    private void lockFocus() {
        state=STATE_WAIT_LOCK;
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            captureSession.capture(captureRequestBuilder.build(),captureCallbackListener,btHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void unlockFocus() {
        state=STATE_PREVIEW;
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            captureSession.capture(captureRequestBuilder.build(),captureCallbackListener,btHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraManager.openCamera(cameraId, cameraCallback, btHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager= (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId:cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics= cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap configurationMap=cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size largestImageSize=Collections.max(Arrays.asList(configurationMap.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return Long.signum(lhs.getWidth()*lhs.getHeight()-rhs.getWidth()*rhs.getHeight());
                    }
                });
                imageReader=ImageReader.newInstance(largestImageSize.getWidth(),largestImageSize.getHeight(),ImageFormat.JPEG,1);
                imageReader.setOnImageAvailableListener(onImageAvailableListener,btHandler);
                Size[] mapSizes=configurationMap.getOutputSizes(SurfaceTexture.class);
                previewSize= getPreferredPreviewSize(mapSizes,width,height);
                this.cameraId=cameraId;
            }
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height) {
        List<Size> collectorSize=new ArrayList<>();
        for (Size option:mapSizes){
            if (width>height){
                if (option.getWidth()>width&&option.getHeight()>height){
                    collectorSize.add(option);
                }
            }
            else {
                if (option.getWidth()>height&&option.getHeight()>width){
                    collectorSize.add(option);
                }
            }
        }
        if (!collectorSize.isEmpty()){
            Collections.min(collectorSize, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getHeight()*lhs.getHeight()-rhs.getWidth()*rhs.getHeight());
                }
            });
        }
        return mapSizes[0];
    }

    CameraDevice.StateCallback cameraCallback=new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            cameraDevice=null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture= cameraSurfaceTV.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(),previewSize.getHeight());
            Surface previewSurfaceView=new Surface(surfaceTexture);
            captureRequestBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurfaceView);
            cameraDevice.createCaptureSession(Arrays.asList(previewSurfaceView,imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice!=null){
                        try {
                            captureRequest =captureRequestBuilder.build();
                            captureSession=cameraCaptureSession;
                            captureSession.setRepeatingRequest(captureRequest,captureCallbackListener,btHandler);
                        }catch (CameraAccessException e){
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.v(TAG,"create camera session failed");
                }
            },btHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }
    private final ImageReader.OnImageAvailableListener onImageAvailableListener=new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            btHandler.post(new ImageSaver(imageReader.acquireNextImage(),MainActivity.this,imageFile));
        }
    };
    private int state;
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result){
            switch (state){
                case STATE_PREVIEW:{
                    // do nothing
                    break;
                }
                case STATE_WAIT_LOCK:{
                    int afState=result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState==CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED){
                        Log.e(TAG,"Focus locked successfully");
                        captureImage();
                    }
                }
            }
        }
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);

        }
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            process(result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };
    private void captureImage(){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MediaActionSound sound = new MediaActionSound();
                    sound.play(MediaActionSound.SHUTTER_CLICK);
                }
            });
            CaptureRequest.Builder requestBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            requestBuilder.addTarget(imageReader.getSurface());
            int orientation=getWindowManager().getDefaultDisplay().getRotation();
            requestBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(orientation));
            CameraCaptureSession.CaptureCallback captureCallback=new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.e(TAG,"Image captured!");
                    unlockFocus();
                }
            };
            captureSession.capture(requestBuilder.build(),captureCallback,null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onImageSaved() {
        Intent intent=new Intent(MainActivity.this,ShowPicture.class);
        intent.putExtra(FILE_PATH_TAG,imageFile.getAbsolutePath());
        startActivity(intent);
    }


}
