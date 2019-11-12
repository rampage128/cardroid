package de.jlab.cardroid.devices.detection;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceConnectionRequest;
import de.jlab.cardroid.devices.DeviceService;

public abstract class UsbDeviceDetector {

    private DeviceSink sink;
    private Runnable onError;

    public void setSink(@NonNull DeviceSink sink) {
        this.sink = sink;
    }

    public void setOnError(@NonNull Runnable onError) {
        this.onError = onError;
    }

    public void identify(@NonNull UsbDevice device, @NonNull DeviceService service) {
        if (!this.startIdentification(device, service)) {
            this.detectionFailed();
        }
    }

    protected void deviceDetected(@NonNull DeviceConnectionRequest connectionRequest) {
        this.sink.deviceDetected(connectionRequest);
    }

    protected void detectionFailed() {
        this.onError.run();
    }

    protected abstract boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service);

    public interface DeviceSink {
        void deviceDetected(@NonNull DeviceConnectionRequest connectionRequest);
    }

}
