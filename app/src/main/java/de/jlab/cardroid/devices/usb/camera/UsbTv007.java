package de.jlab.cardroid.devices.usb.camera;

import android.app.Application;
import android.graphics.Camera;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.arksine.libusbtv.DeviceParams;
import com.arksine.libusbtv.IUsbTvDriver;
import com.arksine.libusbtv.UsbTv;
import com.arksine.libusbtv.UsbTvFrame;

import de.jlab.cardroid.camera.CameraDataProvider;
import de.jlab.cardroid.devices.camera.UsbTv007FrameReceiver;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.usb.UsbDeviceHandler;

// TODO: abstract this to a common Camera to provide support for other hw (e.g., built-in)
public class UsbTv007 extends UsbDeviceHandler {

    private CameraCallbacks callbacks;
    private UsbTv007FrameReceiver frameReceiver;
    private IUsbTvDriver videoDriver;
    private Handler uiHandler;

    public UsbTv007(@NonNull UsbDevice device, @NonNull Application app) {
        super(device, app);
        this.uiHandler = new Handler();
        this.callbacks = new CameraCallbacks();
        this.frameReceiver = new UsbTv007FrameReceiver();
    }

    @Override
    public void open() {
        AsyncTask.execute( () -> {
            this.setDeviceUid(DeviceUid.fromUsbDevice(this.getUsbDevice()));
            this.uiHandler.post(() -> {
                // TODO: read this from Shared Preferences
                DeviceParams params = new DeviceParams.Builder()
                        .setUsbDevice(this.getUsbDevice())
                        .setDriverCallbacks(callbacks)
                        .setInput(UsbTv.InputSelection.COMPOSITE)
                        .setScanType(UsbTv.ScanType.PROGRESSIVE)
                        .setTvNorm(UsbTv.TvNorm.NTSC)
                        .build();
                this.notifyStateChanged(State.OPEN);
                UsbTv.open(this.getApplication().getApplicationContext(), params);
            });

        });
    }

    @Override
    public void close() {
        videoDriver.stopStreaming();
        videoDriver.close();
        // FIXME: We should be able to start/stop the streaming independently of the device state
        notifyStateChanged(State.INVALID);
        // FIXME: library should implement explicit close, but this is one the case.
    }

    private class CameraCallbacks implements UsbTv.DriverCallbacks {
        @Override
        public void onOpen(IUsbTvDriver driver, boolean status) {
            UsbTv007.this.notifyStateChanged(status? State.READY: State.INVALID);
            if (status) {
                UsbTv007.this.videoDriver = driver;
                setDeviceUid(DeviceUid.fromUsbDevice(UsbTv007.this.getUsbDevice()));
                addObservable(frameReceiver);
                notifyFeatureDetected(CameraDataProvider.class);
                driver.setOnFrameReceivedListener(frameReceiver);
                driver.startStreaming();
            }
        }

        @Override
        public void onClose() {
            UsbTv007.this.notifyStateChanged(State.INVALID);
        }

        @Override
        public void onError() {
            UsbTv007.this.notifyStateChanged(State.INVALID);
        }
    }
}
