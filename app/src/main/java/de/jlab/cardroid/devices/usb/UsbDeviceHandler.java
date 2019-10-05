package de.jlab.cardroid.devices.usb;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;

public abstract class UsbDeviceHandler implements DeviceHandler {

    private UsbDevice device;

    public UsbDeviceHandler(@NonNull UsbDevice device, @NonNull DeviceService service) {
        this.device = device;
    }

    @Override
    public int getDeviceId() {
        return this.device.getDeviceId();
    }

    protected UsbDevice getUsbDevice() {
        return this.device;
    }

}
