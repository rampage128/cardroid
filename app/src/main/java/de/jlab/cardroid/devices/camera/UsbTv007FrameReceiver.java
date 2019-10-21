package de.jlab.cardroid.devices.camera;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arksine.libusbtv.UsbTv;
import com.arksine.libusbtv.UsbTvFrame;

import java.util.ArrayList;

import de.jlab.cardroid.camera.CameraObservable;
import de.jlab.cardroid.car.CanObservable;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.usb.camera.UsbTv007;

public class UsbTv007FrameReceiver implements CameraObservable, UsbTv.onFrameReceivedListener {

    // TODO: This class should get a handle of the UsbTv007Driver and lazily start streaming (when listeners subscribe to this class)
    private UsbTv007 device;
    private ArrayList<FrameListener> listeners = new ArrayList<>();

    @Override
    public void setDevice(@NonNull DeviceHandler device) {
        this.device = (UsbTv007)device;
    }


    @Nullable
    @Override
    public DeviceHandler getDevice() {
        return this.device;
    }

    @Override
    public void addFrameListener(@NonNull FrameListener listener) {
        if (listeners.contains(listener)) {
            return;
        }
        listeners.add(listener);
    }

    @Override
    public void removeFrameListener(@NonNull FrameListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    @Override
    public void onFrameReceived(UsbTvFrame frame) {
        for (FrameListener listener: listeners) {
            listener.processFrame(frame);
        }
        frame.returnFrame();
    }
}
