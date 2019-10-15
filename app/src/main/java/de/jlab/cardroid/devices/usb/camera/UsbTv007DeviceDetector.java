package de.jlab.cardroid.devices.usb.camera;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;

import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.usb.UsbDeviceDetector;

public class UsbTv007DeviceDetector extends UsbDeviceDetector {

    @Override
    protected boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service) {
        return false;
    }
}
