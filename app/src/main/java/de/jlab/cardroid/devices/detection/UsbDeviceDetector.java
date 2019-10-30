package de.jlab.cardroid.devices.detection;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.identification.DeviceUid;

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

    protected void deviceDetected(@NonNull Device handler, @Nullable DeviceUid predictedDeviceUid) {
        this.sink.deviceDetected(handler, predictedDeviceUid);
    }

    protected void detectionFailed() {
        this.onError.run();
    }

    protected abstract boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service);

    public interface DeviceSink {
        void deviceDetected(@NonNull Device device, @Nullable DeviceUid predictedDeviceUid);
    }

}
