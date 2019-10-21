package de.jlab.cardroid.camera.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.camera2.CameraAccessException;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.IBinder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.jlab.cardroid.R;
import de.jlab.cardroid.camera.CameraDataProvider;
import de.jlab.cardroid.devices.DeviceService;

public class CameraViewActivity extends AppCompatActivity implements CameraDataProvider.CameraProviderListener, GLSurfaceView.Renderer {

    private GLSurfaceView cameraView;

    private DeviceService.DeviceServiceBinder serviceBinder;
    boolean surfaceReady = false;
    boolean attached = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            CameraViewActivity.this.serviceBinder = (DeviceService.DeviceServiceBinder) service;
            CameraViewActivity.this.bindDataProvider();
        }

        public void onServiceDisconnected(ComponentName className) {
            CameraViewActivity.this.unbindDataProvider();
            CameraViewActivity.this.serviceBinder = null;
        }
    };

    private void bindDataProvider() {
        CameraDataProvider cameraProvider = this.serviceBinder.getDeviceProvider(CameraDataProvider.class);
        if (cameraProvider != null) {
            cameraProvider.setFrameListener(this);

        }
    }

    private void unbindDataProvider() {
        CameraDataProvider cameraProvider = this.serviceBinder.getDeviceProvider(CameraDataProvider.class);
        if (cameraProvider != null) {
            cameraProvider.setFrameListener(null);
        }    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_view);
        cameraView = findViewById(R.id.camera_view);
        // TODO: get these from the provider
        cameraView.getHolder().setFixedSize(720, 240);
        cameraView.setEGLContextClientVersion(2);
        cameraView.setRenderer(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.getApplicationContext().unbindService(this.serviceConnection);
        surfaceReady = false;
        attached = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.getApplicationContext().bindService(new Intent(this.getApplicationContext(), DeviceService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onFrameUpdate() {}

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        this.surfaceReady = true;
        if (this.serviceBinder == null) {
            return;
        }
        CameraDataProvider cameraProvider = this.serviceBinder.getDeviceProvider(CameraDataProvider.class);
        if (cameraProvider != null) {
            cameraProvider.attachToGLContext(GLES20.GL_TEXTURE1);
            attached = true;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //Center the screen in a 4/3 ratio
        // TODO: we should get access to the current video stream frame ratio
        int maxWidth = Math.min(height * 4 / 3, width);
        int maxHeight = maxWidth * 3 / 4;
        GLES20.glViewport((width - maxWidth) / 2, (height - maxHeight) / 2, maxWidth, maxHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (surfaceReady && !attached && this.serviceBinder != null) {
            CameraDataProvider cameraProvider = this.serviceBinder.getDeviceProvider(CameraDataProvider.class);
            if (cameraProvider != null) {
                cameraProvider.attachToGLContext(GLES20.GL_TEXTURE1);
                attached = true;
            }
        }
        // Clear the color buffer
        // TODO: Is this the cause of the initial swirl?
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        // TODO: maybe we should get the actual rendered frame (if there was any)
        if (this.serviceBinder == null) {
            return;
        }
        CameraDataProvider cameraProvider = this.serviceBinder.getDeviceProvider(CameraDataProvider.class);
        if (cameraProvider != null) {
            cameraProvider.updateImage();
        }
    }
}
