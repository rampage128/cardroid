package de.jlab.cardroid.usb.carduino;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SerialPacket {
    protected byte id;

    public SerialPacket(ByteArrayInputStream stream) throws IOException {
        this.id = (byte)stream.read();
    }

    public SerialPacket(byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }

    public void serialize(ByteArrayOutputStream stream) throws IOException {
        stream.write(this.id);
    }
}
