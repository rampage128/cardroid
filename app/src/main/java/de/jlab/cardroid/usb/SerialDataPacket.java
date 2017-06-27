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

    public long readDWord(int index) {
        return ((this.payload[index+3] & 0xff) << 24) | ((this.payload[index+2] & 0xff) << 16) | ((this.payload[index+1] & 0xff) << 8) | (this.payload[index] & 0xff);
    }

    public int readWord(int index) {
        return ((this.payload[index+1] & 0xff) << 8) | (this.payload[index] & 0xff);
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
