package de.jlab.cardroid.devices.usb.serial.gps;

import android.app.Application;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.serial.gps.GpsPositionParser;
import de.jlab.cardroid.devices.serial.gps.GpsSerialReader;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDevice;

public final class GpsUsbDevice extends UsbSerialDevice<GpsSerialReader> {

    private GpsPositionParser positionParser = new GpsPositionParser();
    private DeviceUid uid;

    public GpsUsbDevice(@NonNull UsbDevice device, int defaultBaudrate, @NonNull Application app) {
        super(device, defaultBaudrate, app);

        this.uid = DeviceUid.fromUsbDevice(device);
    }

    @Override
    protected GpsSerialReader onOpenSuccess() {
        GpsSerialReader reader = new GpsSerialReader();
        reader.addSerialPacketListener(this.positionParser);
        this.setDeviceUid(this.uid);
        this.setState(State.READY);
        this.addFeature(this.positionParser);
        return reader;
    }

    @Override
    protected void onOpenFailed() {}

    @Override
    protected void onClose(GpsSerialReader reader) {
        reader.removeSerialPacketListener(this.positionParser);
    }

}
