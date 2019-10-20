package de.jlab.cardroid.devices.usb.camera;

import android.app.Application;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;

import com.arksine.libusbtv.UsbTv;

import java.util.ArrayList;

import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.usb.UsbDeviceDetector;

public class UsbTv007DeviceDetector extends UsbDeviceDetector {

    @Override
    protected boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service) {
        ArrayList<UsbDevice> devList = UsbTv.enumerateUsbtvDevices(service.getBaseContext());
        boolean detected = devList.contains(device);
        this.deviceDetected(new UsbTv007(device, service.getApplication()));
        return detected;
    }
}
