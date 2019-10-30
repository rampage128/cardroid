package de.jlab.cardroid.devices.detection;

import android.app.Application;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.DeviceConnectionRequest;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDevice;

public final class GpsSerialMatcher implements UsbSerialDeviceDetector.SerialMatcher {

    private static final String PATTERN = ".*\\$GP...,.*";

    private String received = "";

    @Nullable
    @Override
    public DeviceConnectionRequest detect(@NonNull byte[] data, @NonNull UsbDevice device, int baudRate, @NonNull Application app) {
        this.received += new String(data);
        if (this.received.matches(PATTERN)) {
            return new DeviceConnectionRequest(
                    new GpsUsbDevice(device, baudRate, app),
                    DeviceUid.fromUsbDevice(device)
            );
        }
        return null;
    }

    /*
        @Nullable
        @Override
        public Device detect(@NonNull byte[] data, @NonNull UsbDevice device, int baudRate, @NonNull Application app) {

        }

        @NonNull
        @Override
        public DeviceConnectionRequest predictDeviceUid(@NonNull UsbDevice device) {
            return DeviceUid.fromUsbDevice(device);
        }
    */
    @Override
    public void clear() {
        this.received = "";
    }
}
