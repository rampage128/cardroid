package de.jlab.cardroid.devices.usb.serial;

import android.hardware.usb.UsbDevice;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.usb.UsbDeviceDetector;

public abstract class UsbSerialDeviceDetector extends UsbDeviceDetector {

    private Timer timer = new Timer();

    @Override
    protected boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service) {
        boolean connectionSuccess = this.startSerialIdentification(device, service);
        if (connectionSuccess) {
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    UsbSerialDeviceDetector.this.detectionFailed();
                }
            }, this.getTimeout());
        }
        return connectionSuccess;
    }

    protected abstract boolean startSerialIdentification(@NonNull UsbDevice device, @NonNull DeviceService service);
    protected abstract long getTimeout();

}
