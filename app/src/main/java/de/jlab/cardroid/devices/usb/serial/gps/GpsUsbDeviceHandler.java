package de.jlab.cardroid.devices.usb.serial.gps;

import android.app.Application;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.serial.gps.GpsPositionParser;
import de.jlab.cardroid.devices.serial.gps.GpsSerialReader;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceHandler;
import de.jlab.cardroid.gps.GpsDataProvider;

public final class GpsUsbDeviceHandler extends UsbSerialDeviceHandler<GpsSerialReader> {

    private GpsPositionParser positionParser = new GpsPositionParser();

    public GpsUsbDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull Application app) {
        super(device, defaultBaudrate, app);
    }

    @Override
    protected GpsSerialReader onConnect() {
        GpsSerialReader reader = new GpsSerialReader();
        reader.addSerialPacketListener(this.positionParser);
        this.notifyFeatureDetected(GpsDataProvider.class, this.positionParser, null);
        return reader;
    }

    @Override
    protected void onConnectFailed() {}

    @Override
    protected void onDisconnect(GpsSerialReader reader) {
        reader.removeSerialPacketListener(this.positionParser);
    }

}
