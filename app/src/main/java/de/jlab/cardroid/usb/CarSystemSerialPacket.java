package de.jlab.cardroid.usb;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CarSystemSerialPacket extends SerialDataPacket {
    public CarSystemSerialPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
    }

    public CarSystemSerialPacket(byte id, byte[] payload) {
        super(id, payload);
    }
}
