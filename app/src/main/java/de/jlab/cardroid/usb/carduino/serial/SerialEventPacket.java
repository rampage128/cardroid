package de.jlab.cardroid.usb.carduino.serial;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SerialEventPacket extends SerialDataPacket {
    public SerialEventPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
    }

    public SerialEventPacket(byte id, byte[] payload) {
        super(id, payload);
    }
}
