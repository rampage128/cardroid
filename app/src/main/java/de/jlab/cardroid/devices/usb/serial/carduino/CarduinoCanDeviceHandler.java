package de.jlab.cardroid.devices.usb.serial.carduino;

import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.util.LongSparseArray;

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
    private LongSparseArray<Byte> canIdRequests = new LongSparseArray<>();
    private boolean isHandshakeComplete = false;

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
        if (this.isHandshakeComplete) {
            this.sendCanIdRequest(descriptor.getCanId(), descriptor.getByteMask());
        } else {
            this.canIdRequests.put(descriptor.getCanId(), descriptor.getByteMask());
        }
    }

    @Override
    public void unregisterCanId(CanPacketDescriptor descriptor) {
        if (this.isHandshakeComplete) {
            this.sendCanIdRequest(descriptor.getCanId(), (byte)0x00);
        } else {
            this.canIdRequests.put(descriptor.getCanId(), (byte)0x00);
        }
    }

    private void sendCanIdRequest(long canId, byte mask) {
        byte[] payload = ByteBuffer.allocate(5).putInt((int)canId).put(mask).array();
        try {
            Log.e(this.getClass().getSimpleName(), "Send request " + String.format("%02x", canId) + " to device " + this.getDeviceId() + ".");
            this.send(CarduinoMetaType.createPacket(CarduinoMetaType.CAR_DATA_DEFINITION, payload));
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error sending can id request " + String.format("%02x", canId) + " to device " + this.getDeviceId() + ".");
        }
    }

    private void sendPendingCanIdRequests() {
        for (int i = 0; i < this.canIdRequests.size(); i++) {
            this.sendCanIdRequest(this.canIdRequests.keyAt(i), this.canIdRequests.valueAt(i));
        }
        this.canIdRequests.clear();
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
        this.isHandshakeComplete = false;
        // TODO TRIGGER EVENT FOR DISCONNECT OF CAN DEVICE
    }

    @Override
    public void onHandshake(CarduinoSerialReader reader) {
        reader.addSerialPacketListener(this.canParser);
        this.isHandshakeComplete = true;
        this.sendPendingCanIdRequests();
    }

    @Override
    protected void onConnectFailed() {

    }

    @Override
    public Class<?>[] getFeatures() {
        return new Class[] { CanDataProvider.class, ErrorDataProvider.class };
    }

}
