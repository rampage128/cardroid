package de.jlab.cardroid.camera;

import androidx.annotation.NonNull;

import com.arksine.libusbtv.UsbTvFrame;

import de.jlab.cardroid.devices.DeviceDataObservable;

public interface CameraObservable extends DeviceDataObservable {
    void addFrameListener(@NonNull FrameListener listener);
    void removeFrameListener(@NonNull FrameListener listener);

    interface FrameListener {
        // FIXME: we should provide generic interface for cameras here.
        public void processFrame(@NonNull UsbTvFrame frame);
    }
}
