package de.jlab.cardroid.devices.usb.serial.gps;

import android.app.Application;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceDetector;

public final class GpsSerialMatcher implements UsbSerialDeviceDetector.SerialMatcher {

    private static final String PATTERN = ".*\\$GP...,.*";

    private String received = "";

    @Nullable
    @Override
    public Device detect(@NonNull byte[] data, @NonNull UsbDevice device, int baudRate, @NonNull Application app) {
        this.received += new String(data);
        if (this.received.matches(PATTERN)) {
            return new GpsUsbDevice(device, baudRate, app);
        }
        return null;
    }

    @Override
    public void clear() {
        this.received = "";
    }
}
