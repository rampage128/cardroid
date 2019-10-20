package de.jlab.cardroid.camera;

import android.bluetooth.BluetoothClass;

import androidx.annotation.NonNull;

import com.arksine.libusbtv.UsbTvFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.camera.UsbTv007FrameReceiver;

public class CameraDataProvider extends DeviceDataProvider implements CameraObservable.FrameListener {

    private DeviceHandler device;
    private CameraProviderListener listener;
    private BlockingQueue<UsbTvFrame> queuedFrames;
    private UsbTv007GLProcessor drawingEngine;


    // The maximum amount of frames which we hold unprocessed
    // More implies more latency in the video, a less jittery experience
    // TODO: play with this value to reduce it as max as possible
    // TODO: maybe even measure jitter and adapt buffer size dynamically
    private final Integer framesBufferSize = 4;


    public CameraDataProvider(@NonNull DeviceService service) {
        super(service);
        queuedFrames = new ArrayBlockingQueue<>(framesBufferSize, true);
        drawingEngine = new UsbTv007GLProcessor();
    }

    public void attachToGLContext(int texName) {
        drawingEngine.attach(texName);
    }

    public void detachFromGLContext() {
        drawingEngine.detach();
    }

    public void updateImage() {
        UsbTvFrame frame;
        try {
            // Poll for 100 ms
           frame = queuedFrames.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return;
        }
        drawingEngine.drawFrame(frame);
    }



    public void setFrameListener(@NonNull CameraProviderListener listener) {
        this.listener = listener;
    }

    protected void deviceDisconnected(@NonNull DeviceHandler device) {
        if (this.device.equals(device)) {
            UsbTv007FrameReceiver receiver = (UsbTv007FrameReceiver) device.getObservable(CameraObservable.class);
            receiver.removeFrameListener(this);
            this.device = null;
        }
    }

    protected void deviceConnected(@NonNull DeviceHandler device) {
        UsbTv007FrameReceiver receiver = (UsbTv007FrameReceiver) device.getObservable(CameraObservable.class);
        receiver.removeFrameListener(this);
        this.device = device;
    }

    @Override
    protected void onUpdate(@NonNull DeviceHandler previousDevice, @NonNull DeviceHandler newDevice, @NonNull DeviceService service) {
        this.deviceDisconnected(previousDevice);
        this.deviceConnected(newDevice);
    }

    @Override
    protected void onStart(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        this.deviceConnected(device);
    }

    @Override
    protected void onStop(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        this.deviceDisconnected(device);
    }

    // FrameListener
    @Override
    public void processFrame(@NonNull UsbTvFrame frame) {
        if (!queuedFrames.offer(frame)) {
            try {
                queuedFrames.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                frame.returnFrame();
                return;
            }
            queuedFrames.offer(frame);
        }
        frame.returnFrame();
        this.listener.onFrameUpdate();
    }

    public interface CameraProviderListener {
        void onFrameUpdate();
    }
}
