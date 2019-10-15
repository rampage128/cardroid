package de.jlab.cardroid.devices.usb.serial.carduino;

import android.app.Application;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.serial.carduino.CarduinoFeatureDetector;
import de.jlab.cardroid.devices.serial.carduino.CarduinoMetaParser;
import de.jlab.cardroid.devices.serial.carduino.CarduinoMetaType;
import de.jlab.cardroid.devices.serial.carduino.CarduinoPacketParser;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialPacket;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.devices.serial.carduino.CarduinoUidGenerator;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceHandler;

public final class CarduinoUsbDeviceHandler extends UsbSerialDeviceHandler<CarduinoSerialReader> {

    private boolean isReady = false;
    private ArrayList<CarduinoPacketParser> packetParsers = new ArrayList<>();
    private ArrayList<CarduinoSerialPacket> pendingPackets = new ArrayList<>();
    private CarduinoMetaParser metaParser = null;

    private byte[] carduinoId = null;

    public CarduinoUsbDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull Application app) {
        super(device, defaultBaudrate, app);
    }

    public void send(CarduinoSerialPacket packet) {
        if (!this.isReady) {
            this.pendingPackets.add(packet);
        } else {
            this.sendImmediately(packet);
        }
    }

    public void sendImmediately(CarduinoSerialPacket packet) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            packet.serialize(bos);
            this.send(bos.toByteArray());
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error serializing packet " + String.format("%02x", packet.getPacketId()) + " for device " + this.getDeviceId());
        }
    }

    @Override
    protected final CarduinoSerialReader onConnect() {
        CarduinoSerialReader reader = new CarduinoSerialReader();

        this.metaParser = new CarduinoMetaParser(this, reader);

        this.addPacketParser(new CarduinoFeatureDetector(this, reader), reader);
        this.addPacketParser(this.metaParser, reader);

        return reader;
    }

    public void addPacketParser(CarduinoPacketParser parser, CarduinoSerialReader reader) {
        reader.addSerialPacketListener(parser);
    }

    @Override
    protected void onConnectFailed() {
        // Nothing to do here
    }

    @Override
    protected final void onDisconnect(CarduinoSerialReader reader) {
        for (int i = 0; i < this.packetParsers.size(); i++) {
            reader.removeSerialPacketListener(this.packetParsers.remove(i));
        }
    }

    public void onCarduinoIdReceived(@NonNull byte[] carduinoId) {
        if (!Arrays.equals(this.carduinoId, carduinoId)) {
            this.carduinoId = carduinoId;
            this.notifyUidReceived(CarduinoUidGenerator.getUid(carduinoId));
        }
    }

    public void onHandshake(CarduinoSerialReader reader) {
        this.isReady = true;
        while (!this.pendingPackets.isEmpty()) {
            this.sendImmediately(this.pendingPackets.remove(0));
        }
    }

    @Override
    public DeviceUid requestNewUid(@NonNull Application app) {
        byte[] carduinoId = CarduinoUidGenerator.generateId(app).getBytes();
        this.sendImmediately(CarduinoMetaType.createPacket(CarduinoMetaType.SET_UID, carduinoId));
        return CarduinoUidGenerator.getUid(carduinoId);
    }

    @Override
    public void allowCommunication() {
        this.metaParser.allowHandshake();
    }

}
