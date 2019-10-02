package de.jlab.cardroid.devices.usb;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;

public abstract class UsbDeviceDetector {

    private DetectionObserver observer;

    public void setObserver(DetectionObserver observer) {
        this.observer = observer;
    }

    public void identify(@NonNull UsbDevice device, @NonNull DeviceService service) {
        if (this.observer == null) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " can not identify a device without an observer! Did you call setObserver(DetectionObserver)?");
        }
        if (!this.startIdentification(device, service)) {
            this.detectionFailed();
        }
    }

    protected void deviceDetected(@NonNull DeviceHandler handler) {
        this.observer.deviceDetected(handler);
    }

    protected void detectionFailed() {
        this.observer.detectionFailed();
    }

    protected abstract boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service);

    public interface DetectionObserver {
        void deviceDetected(@NonNull DeviceHandler handler);
        void detectionFailed();
    }

}
