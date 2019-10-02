package de.jlab.cardroid.devices.usb.serial.carduino;

import android.hardware.usb.UsbDevice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.carduino.CarduinoErrorParser;
import de.jlab.cardroid.devices.serial.carduino.CarduinoMetaParser;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialPacket;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceHandler;

public abstract class CarduinoUsbDeviceHandler extends UsbSerialDeviceHandler<CarduinoSerialReader> {

    private CarduinoMetaParser metaParser;
    private CarduinoErrorParser errorParser;

    public CarduinoUsbDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull DeviceService service) {
        super(device, defaultBaudrate, service);
    }

    public void send(CarduinoSerialPacket packet) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        packet.serialize(bos);
        this.send(bos.toByteArray());
    }

    @Override
    protected final CarduinoSerialReader onConnect() {
        CarduinoSerialReader reader = new CarduinoSerialReader();

        this.errorParser = new CarduinoErrorParser();
        this.metaParser = new CarduinoMetaParser(this, reader);
        reader.addSerialPacketListener(this.metaParser);
        reader.addSerialPacketListener(this.errorParser);

        return reader;
    }

    public void addErrorListener(CarduinoErrorParser.ErrorListener listener) {
        this.errorParser.addListener(listener);
    }

    public void removeErrorListener(CarduinoErrorParser.ErrorListener listener) {
        this.errorParser.removeListener(listener);
    }

    @Override
    protected final void onDisconnect(CarduinoSerialReader reader) {
        reader.removeSerialPacketListener(this.metaParser);
        reader.removeSerialPacketListener(this.errorParser);
        this.metaParser = null;
        this.errorParser = null;
        this.onDisconnectCarduino(reader);
    }

    protected abstract void onConnect(CarduinoSerialReader reader);
    protected abstract void onDisconnectCarduino(CarduinoSerialReader reader);

    public abstract void onHandshake(CarduinoSerialReader reader);

}
