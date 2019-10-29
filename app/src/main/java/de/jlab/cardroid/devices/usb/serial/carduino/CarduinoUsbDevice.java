package de.jlab.cardroid.devices.usb.serial.carduino;

import android.app.Application;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.serial.carduino.CarduinoFeatureDetector;
import de.jlab.cardroid.devices.serial.carduino.CarduinoMetaParser;
import de.jlab.cardroid.devices.serial.carduino.CarduinoPacketParser;
import de.jlab.cardroid.devices.serial.carduino.CarduinoPacketType;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialPacket;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.devices.serial.carduino.CarduinoUidGenerator;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDevice;

public final class CarduinoUsbDevice extends UsbSerialDevice<CarduinoSerialReader> {

    private boolean isReady = false;
    private ArrayList<CarduinoPacketParser> packetParsers = new ArrayList<>();
    private ArrayList<CarduinoSerialPacket> pendingPackets = new ArrayList<>();
    private Application app;

    private byte[] carduinoId = null;

    public CarduinoUsbDevice(@NonNull UsbDevice device, int defaultBaudrate, @NonNull Application app) {
        super(device, defaultBaudrate, app);

        this.app = app;
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
            Log.e(this.getClass().getSimpleName(), "Error serializing packet " + String.format("%02x", packet.getPacketId()));
        }
    }

    public void addDynamicFeature(@NonNull CarduinoPacketType type) {
        Feature[] features = type.getFeatures();
        if (features != null) {
            for (Feature feature : features) {
                this.addFeature(feature);
                if (feature instanceof CarduinoPacketParser) {
                    getReader().addSerialPacketListener((CarduinoPacketParser)feature);
                }
            }
        }
    }

    @Override
    protected CarduinoSerialReader onOpenSuccess() {
        CarduinoSerialReader reader = new CarduinoSerialReader();
        reader.addSerialPacketListener(new CarduinoFeatureDetector(this));
        reader.addSerialPacketListener(new CarduinoMetaParser(this, this.app));
        return reader;
    }

    @Override
    protected void onOpenFailed() {}

    @Override
    protected void onClose(CarduinoSerialReader reader) {
        for (int i = 0; i < this.packetParsers.size(); i++) {
            reader.removeSerialPacketListener(this.packetParsers.remove(i));
        }
    }

    public void onCarduinoIdReceived(@NonNull byte[] carduinoId) {
        if (!Arrays.equals(this.carduinoId, carduinoId)) {
            this.carduinoId = carduinoId;
            this.setDeviceUid(CarduinoUidGenerator.getUid(carduinoId));
        }
    }

    public void onHandshake() {
        this.setState(State.READY);
        this.isReady = true;
        while (!this.pendingPackets.isEmpty()) {
            this.sendImmediately(this.pendingPackets.remove(0));
        }
    }

}
