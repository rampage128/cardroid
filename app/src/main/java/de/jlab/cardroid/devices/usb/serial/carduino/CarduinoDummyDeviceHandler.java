package de.jlab.cardroid.devices.usb.serial.carduino;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceHandler;

public final class CarduinoDummyDeviceHandler extends UsbSerialDeviceHandler<CarduinoSerialReader> {

    private CarduinoSerialReader reader;

    public CarduinoDummyDeviceHandler(@NonNull CarduinoSerialReader reader, @NonNull UsbDevice device, int defaultBaudrate, @NonNull DeviceService service) {
        super(device, defaultBaudrate, service);
        this.reader = reader;
    }

    @Override
    protected CarduinoSerialReader onConnect() {
        return this.reader;
    }

    @Override
    protected void onConnectFailed() {
        this.reader = null;
    }

    @Override
    protected void onDisconnect(CarduinoSerialReader reader) {
        this.reader = null;
    }

    @NonNull
    @Override
    public Class<?>[] getFeatures() {
        return new Class[0];
    }

}
