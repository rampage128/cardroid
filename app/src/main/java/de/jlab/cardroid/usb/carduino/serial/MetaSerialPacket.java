package de.jlab.cardroid.usb.carduino.serial;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MetaSerialPacket extends SerialDataPacket {
    public MetaSerialPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
    }

    public MetaSerialPacket(byte id, byte[] payload) {
        super(id, payload);
    }
}
