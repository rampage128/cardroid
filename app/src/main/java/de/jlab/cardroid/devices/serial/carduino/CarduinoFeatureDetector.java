package de.jlab.cardroid.devices.serial.carduino;

import android.util.Log;

import java.util.ArrayList;

import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDevice;

public final class CarduinoFeatureDetector extends CarduinoPacketParser {

    private ArrayList<Byte> knownPacketTypes = new ArrayList<>();
    private CarduinoUsbDevice device;

    public CarduinoFeatureDetector(CarduinoUsbDevice device) {
        this.device = device;
    }

    @Override
    protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
        return !this.knownPacketTypes.contains(packet.getPacketType());
    }

    @Override
    protected void handlePacket(CarduinoSerialPacket packet) {
        byte rawPacketType = packet.getPacketType();
        this.knownPacketTypes.add(rawPacketType);
        CarduinoPacketType type = CarduinoPacketType.getFromPacket(packet);
        if (type != null) {
            this.device.addDynamicFeature(type);
        } else {
            Log.e(this.getClass().getSimpleName(), "Device registered unknown feature: " + new String(new byte[] { rawPacketType }));
        }
    }
}
