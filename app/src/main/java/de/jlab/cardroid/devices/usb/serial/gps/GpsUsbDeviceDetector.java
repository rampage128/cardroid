package de.jlab.cardroid.devices.usb.serial.gps;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.gps.GpsPositionParser;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceDetector;

public final class GpsUsbDeviceDetector extends UsbSerialDeviceDetector {

    private GpsUsbDeviceHandler gps;
    private GpsPositionParser.PositionListener positionListener = (position, packet) -> {
        this.gps.disconnectDevice();
        GpsUsbDeviceDetector.this.deviceDetected(this.gps);
    };

    @Override
    public boolean startSerialIdentification(@NonNull UsbDevice device, @NonNull DeviceService service) {
        this.gps.addPositionListener(this.positionListener);
        this.gps = new GpsUsbDeviceHandler(device, 4800, service);
        return this.gps.connectDevice();
    }

    @Override
    protected long getTimeout() {
        return 500;
    }
}
