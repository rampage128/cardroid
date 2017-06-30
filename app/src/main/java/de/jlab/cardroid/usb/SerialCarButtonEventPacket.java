package de.jlab.cardroid.usb;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SerialCarButtonEventPacket extends SerialDataPacket {
    public SerialCarButtonEventPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
    }

    public SerialCarButtonEventPacket(byte id, byte[] payload) {
        super(id, payload);
    }
}
