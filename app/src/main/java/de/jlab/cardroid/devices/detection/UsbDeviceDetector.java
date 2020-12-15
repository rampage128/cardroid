package de.jlab.cardroid.devices.detection;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceConnectionRequest;
import de.jlab.cardroid.devices.DeviceService;

public abstract class UsbDeviceDetector {

    private DeviceSink sink;
    private DeviceDrain drain;

    public void setSink(@NonNull DeviceSink sink) {
        this.sink = sink;
    }

    public void setOnError(@NonNull DeviceDrain drain) {
        this.drain = drain;
    }

    public void identify(@NonNull UsbDevice device, @NonNull DeviceService service) {
        if (!this.startIdentification(device, service)) {
            this.detectionFailed(device);
        }
    }

    protected void deviceDetected(@NonNull DeviceConnectionRequest connectionRequest) {
        this.sink.deviceDetected(connectionRequest);
    }

    protected void detectionFailed(@NonNull UsbDevice device) {
        this.drain.deviceDetectionFailed(device);
    }

    protected abstract boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service);

    public interface DeviceSink {
        void deviceDetected(@NonNull DeviceConnectionRequest connectionRequest);
    }

    public interface DeviceDrain {
        void deviceDetectionFailed(@NonNull UsbDevice device);
    }

}
