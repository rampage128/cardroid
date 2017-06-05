package de.jlab.cardroid.usb;

public class CarSystemSerialPacket extends SerialDataPacket {
    public CarSystemSerialPacket(byte id, byte[] payload) {
        super(id, payload);
    }
}
