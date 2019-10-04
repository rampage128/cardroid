package de.jlab.cardroid.devices.usb.serial;

import android.hardware.usb.UsbDevice;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.usb.UsbDeviceDetector;

public abstract class UsbSerialDeviceDetector extends UsbDeviceDetector {

    private Timer timer;

    @Override
    protected boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service) {
        boolean connectionSuccess = this.startSerialIdentification(device, service);
        if (connectionSuccess) {
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    UsbSerialDeviceDetector.this.detectionFailed();
                }
            }, this.getTimeout());
        }
        return connectionSuccess;
    }

    protected void deviceDetected(@NonNull DeviceHandler handler) {
        this.timer.cancel();
        super.deviceDetected(handler);
    }

    protected abstract boolean startSerialIdentification(@NonNull UsbDevice device, @NonNull DeviceService service);
    protected abstract long getTimeout();

}
