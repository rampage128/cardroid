package de.jlab.cardroid.devices.usb.serial.carduino;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.car.CanPacketDescriptor;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.can.CanDeviceHandler;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.errors.ErrorDataProvider;

public final class CarduinoLegacyDeviceHandler extends CarduinoUsbDeviceHandler implements CanDeviceHandler {

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
        this.powerDevice.onConnect(reader);
        this.canDevice.onConnect(reader);
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

    @Override
    public void addCanListener(CanPacketListener listener) {
        this.canDevice.addCanListener(listener);
    }

    @Override
    public void removeCanListener(CanPacketListener listener) {
        this.canDevice.removeCanListener(listener);
    }

    @Override
    public void registerCanId(CanPacketDescriptor descriptor) {
        this.canDevice.registerCanId(descriptor);
    }

    @Override
    public void unregisterCanId(CanPacketDescriptor descriptor) {
        this.canDevice.unregisterCanId(descriptor);
    }

    @Override
    public void startSniffer() {
        this.canDevice.startSniffer();
    }

    @Override
    public void stopSniffer() {
        this.canDevice.stopSniffer();
    }

    public Class<?>[] getFeatures() {
        return new Class[] { CanDataProvider.class, ErrorDataProvider.class };
    }

}
