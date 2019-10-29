package de.jlab.cardroid.devices.usb;

import android.app.Application;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;

public abstract class UsbDevice extends Device {

    private android.hardware.usb.UsbDevice device;

    public UsbDevice(@NonNull android.hardware.usb.UsbDevice device, @NonNull Application app) {
        super(app);
        this.device = device;
        this.setConnectionId(DeviceConnectionId.fromUsbDevice(device));
    }

}
