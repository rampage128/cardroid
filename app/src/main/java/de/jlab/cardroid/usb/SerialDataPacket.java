package de.jlab.cardroid.usb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SerialDataPacket extends SerialPacket {
    private byte[] payload;

    public SerialDataPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
        int payloadLength = stream.read();
        this.payload = new byte[payloadLength];
        stream.read(this.payload);
    }

    public SerialDataPacket(byte id, byte[] payload) {
        super(id);
        this.payload = payload;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public boolean readFlag(int index, int bitNum) {
        return (this.payload[index] & (1<<bitNum)) != 0;
    }

    public byte readByte(int index) {
        return this.payload[index];
    }

    public void serialize(ByteArrayOutputStream stream) throws IOException {
        super.serialize(stream);

        if (this.payload != null && this.payload.length > 0) {
            stream.write(this.payload.length);
            stream.write(this.payload);
        }
    }
}
