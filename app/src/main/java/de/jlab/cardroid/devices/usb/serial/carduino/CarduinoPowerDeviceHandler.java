package de.jlab.cardroid.devices.usb.serial.carduino;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.errors.ErrorDataProvider;

public final class CarduinoPowerDeviceHandler extends CarduinoUsbDeviceHandler {

    public CarduinoPowerDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull DeviceService service) {
        super(device, defaultBaudrate, service);
    }

    @Override
    protected void onConnect(CarduinoSerialReader reader) {
        this.getDeviceService().getRuleHandler().triggerRule(4);
    }

    @Override
    protected void onConnectFailed() {

    }

    @Override
    protected void onDisconnectCarduino(CarduinoSerialReader reader) {
        this.getDeviceService().getRuleHandler().triggerRule(5);
    }

    @Override
    public void onHandshake(CarduinoSerialReader reader) {

    }

    @NonNull
    @Override
    public Class<?>[] getFeatures() {
        return new Class<?>[] { ErrorDataProvider.class };
    }

}
