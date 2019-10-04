package de.jlab.cardroid.devices.usb.serial.carduino;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.car.CanPacketDescriptor;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.can.CanDeviceHandler;
import de.jlab.cardroid.devices.serial.carduino.CarduinoCanParser;
import de.jlab.cardroid.devices.serial.carduino.CarduinoMetaType;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.errors.ErrorDataProvider;

public final class CarduinoCanDeviceHandler extends CarduinoUsbDeviceHandler implements CanDeviceHandler {

    private CarduinoCanParser canParser;

    public CarduinoCanDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull DeviceService service) {
        super(device, defaultBaudrate, service);
    }

    @Override
    public void addCanListener(CanPacketListener listener) {
        this.canParser.addCanListener(listener);
    }

    @Override
    public void removeCanListener(CanPacketListener listener) {
        this.canParser.removeCanListener(listener);
    }

    @Override
    public void registerCanId(CanPacketDescriptor descriptor) {
        byte[] payload = ByteBuffer.allocate(5).putInt((int)descriptor.getCanId()).put(descriptor.getByteMask()).array();
        try {
            this.send(CarduinoMetaType.createPacket(CarduinoMetaType.CAR_DATA_DEFINITION, payload));
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error registering can id " + String.format("%02x", descriptor.getCanId()) + " with device " + this.getDeviceId() + ".");
        }
    }

    @Override
    public void unregisterCanId(CanPacketDescriptor descriptor) {
        byte[] payload = ByteBuffer.allocate(5).putInt((int)descriptor.getCanId()).put((byte)0x00).array();
        try {
            this.send(CarduinoMetaType.createPacket(CarduinoMetaType.CAR_DATA_DEFINITION, payload));
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error unregistering can id " + String.format("%02x", descriptor.getCanId()) + " from device " + this.getDeviceId() + ".");
        }
    }

    @Override
    public void startSniffer() {
        try {
            this.send(CarduinoMetaType.createPacket(CarduinoMetaType.START_SNIFFING, null));
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error starting can sniffer for device " + this.getDeviceId() + ".");
        }
    }

    @Override
    public void stopSniffer() {
        try {
            this.send(CarduinoMetaType.createPacket(CarduinoMetaType.STOP_SNIFFING, null));
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error stopping can sniffer for device " + this.getDeviceId() + ".");
        }
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
        // FIXME: requested can packets should probably be sent to the device here
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
