package de.jlab.cardroid.devices.usb;

import android.app.Application;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;

public abstract class UsbDeviceHandler extends DeviceHandler {

    private UsbDevice device;

    protected UsbDevice getUsbDevice() { return this.device; }

    public UsbDeviceHandler(@NonNull UsbDevice device, @NonNull Application app) {
        super(app);
        this.device = device;
        this.setConnectionId(DeviceConnectionId.fromUsbDevice(device));
    }

}
