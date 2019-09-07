package de.jlab.cardroid.usb.carduino.serial;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import de.jlab.cardroid.usb.carduino.serial.SerialDataPacket;

public class CarSystemSerialPacket extends SerialDataPacket {
    public CarSystemSerialPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
    }

    public CarSystemSerialPacket(byte id, byte[] payload) {
        super(id, payload);
    }
}
