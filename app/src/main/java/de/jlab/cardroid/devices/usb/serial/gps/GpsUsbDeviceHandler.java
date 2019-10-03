package de.jlab.cardroid.devices.usb.serial.gps;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.gps.GpsPositionParser;
import de.jlab.cardroid.devices.serial.gps.GpsSerialReader;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceHandler;
import de.jlab.cardroid.gps.GpsDataProvider;

public final class GpsUsbDeviceHandler extends UsbSerialDeviceHandler<GpsSerialReader> {

    private GpsPositionParser positionParser = new GpsPositionParser();

    public GpsUsbDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull DeviceService service) {
        super(device, defaultBaudrate, service);
    }

    @Override
    protected GpsSerialReader onConnect() {
        GpsSerialReader reader = new GpsSerialReader();
        reader.addSerialPacketListener(this.positionParser);
        return reader;
    }

    @Override
    protected void onConnectFailed() {}

    @Override
    protected void onDisconnect(GpsSerialReader reader) {
        reader.removeSerialPacketListener(this.positionParser);
    }

    public void addPositionListener(GpsPositionParser.PositionListener listener) {
        this.positionParser.addPositionListener(listener);
    }

    public void removePositionListener(GpsPositionParser.PositionListener listener) {
        this.positionParser.removePositionListener(listener);
    }

    @NonNull
    @Override
    public Class<?>[] getFeatures() {
        return new Class<?>[] { GpsDataProvider.class };
    }

}
