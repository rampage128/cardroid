package de.jlab.cardroid.devices.usb.serial.carduino;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.errors.ErrorDataProvider;

public final class CarduinoLegacyDeviceHandler extends CarduinoUsbDeviceHandler {

    private CarduinoPowerDeviceHandler powerDevice;
    private CarduinoCanDeviceHandler canDevice;

    public CarduinoLegacyDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull DeviceService service) {
        super(device, defaultBaudrate, service);
        this.powerDevice = new CarduinoPowerDeviceHandler(device, defaultBaudrate, service);
        this.canDevice = new CarduinoCanDeviceHandler(device, defaultBaudrate, service);
    }

    public CarduinoCanDeviceHandler getCanDevice() {
        return this.canDevice;
    }

    @Override
    protected void onConnect(CarduinoSerialReader reader) {
        this.powerDevice.onConnect();
        this.canDevice.onConnect();
    }

    @Override
    protected void onConnectFailed() {
        this.powerDevice.onConnectFailed();
        this.canDevice.onConnectFailed();
    }

    @Override
    protected void onDisconnectCarduino(CarduinoSerialReader reader) {
        this.powerDevice.onDisconnectCarduino(reader);
        this.canDevice.onDisconnectCarduino(reader);
    }

    @Override
    public void onHandshake(CarduinoSerialReader reader) {
        this.powerDevice.onHandshake(reader);
        this.canDevice.onHandshake(reader);
    }

    public Class<?>[] getFeatures() {
        return new Class[] { CanDataProvider.class, ErrorDataProvider.class };
    }

}
