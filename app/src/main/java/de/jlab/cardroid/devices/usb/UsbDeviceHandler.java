package de.jlab.cardroid.devices.usb;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;

public abstract class UsbDeviceHandler implements DeviceHandler {

    private UsbDevice device;
    private DeviceService service;

    public UsbDeviceHandler(@NonNull UsbDevice device, @NonNull DeviceService service) {
        this.device = device;
        this.service = service;
    }

    @Override
    public int getDeviceId() {
        return this.device.getDeviceId();
    }

    protected UsbDevice getUsbDevice() {
        return this.device;
    }

    protected DeviceService getDeviceService() {
        return this.service;
    }

}
