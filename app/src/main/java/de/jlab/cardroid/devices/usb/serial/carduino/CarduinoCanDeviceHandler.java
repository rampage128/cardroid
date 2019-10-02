package de.jlab.cardroid.devices.usb.serial.carduino;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.can.CanDeviceHandler;
import de.jlab.cardroid.devices.serial.carduino.CarduinoCanParser;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.errors.ErrorDataProvider;

public final class CarduinoCanDeviceHandler extends CarduinoUsbDeviceHandler implements CanDeviceHandler {

    private CarduinoCanParser canParser;

    public CarduinoCanDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull DeviceService service) {
        super(device, defaultBaudrate, service);
    }

    public void addCanListener(CarduinoCanParser.CanPacketListener listener) {
        this.canParser.addCanListener(listener);
    }

    public void removeCanListener(CarduinoCanParser.CanPacketListener listener) {
        this.canParser.removeCanListener(listener);
    }

    @Override
    protected void onConnect(CarduinoSerialReader reader) {
        // TODO TRIGGER EVENT FOR CONNECT OF CAN DEVICE
        this.canParser = new CarduinoCanParser();
    }

    @Override
    protected void onDisconnectCarduino(CarduinoSerialReader reader) {
        reader.removeSerialPacketListener(this.canParser);
        this.canParser = null;
        // TODO TRIGGER EVENT FOR DISCONNECT OF CAN DEVICE
    }

    @Override
    public void onHandshake(CarduinoSerialReader reader) {
        reader.addSerialPacketListener(this.canParser);
    }

    @Override
    protected void onConnectFailed() {

    }

    @Override
    public Class<?>[] getFeatures() {
        return new Class[] { CanDataProvider.class, ErrorDataProvider.class };
    }

}
