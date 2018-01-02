package de.jlab.cardroid.usb.gps;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import de.jlab.cardroid.usb.UsbService;
import de.jlab.cardroid.usb.UsbWatchDog;

public class GpsWatchDog extends UsbWatchDog {
    public GpsWatchDog(Context context) {
        super(context);
    }

    @Override
    protected boolean shouldWatchDevice(UsbDevice device) {
        return device.getVendorId() == 0x067B && device.getProductId() == 0x2303;
    }

    @Override
    protected Class<? extends UsbService> getServiceClass() {
        return GpsService.class;
    }
}
