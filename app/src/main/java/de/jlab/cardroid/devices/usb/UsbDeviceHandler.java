package de.jlab.cardroid.devices.usb;

import android.app.Application;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;

public abstract class UsbDeviceHandler extends DeviceHandler {

    private UsbDevice device;
    private DeviceConnectionId connectionId;

    public UsbDeviceHandler(@NonNull UsbDevice device) {
        this.device = device;
        this.connectionId = DeviceConnectionId.fromUsbDevice(device);
    }

    @Override
    @NonNull
    public DeviceConnectionId getConnectionId() {
        return this.connectionId;
    }

    @Override
    @NonNull
    public DeviceUid requestNewUid(@NonNull Application app) {
        return DeviceUid.fromUsbDevice(this.device);
    }

}
