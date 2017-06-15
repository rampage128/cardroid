package de.jlab.cardroid.usb;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SerialCommandPacket extends SerialDataPacket {
    public SerialCommandPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
    }

    public SerialCommandPacket(byte id, byte[] payload) {
        super(id, payload);
    }
}
