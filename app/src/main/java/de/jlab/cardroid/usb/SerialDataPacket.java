package de.jlab.cardroid.usb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SerialDataPacket extends SerialPacket {
    protected ByteBuffer payload;

    public SerialDataPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
        int payloadLength = stream.read();
        if (payloadLength > -1) {
            byte[] payload = new byte[payloadLength];
            stream.read(payload);
            this.payload = ByteBuffer.wrap(payload);
        }
        else {
            this.payload = null;
        }
    }

    public SerialDataPacket(byte id, byte[] payload) {
        super(id);
        if (payload != null) {
            this.payload = ByteBuffer.wrap(payload);
        }
    }

    public boolean readFlag(int index, int bitNum) {
        return (this.payload.get(index) & (1<<bitNum)) != 0;
    }

    public long readDWord(int index) {
        return this.payload.getInt(index);
    }

    public int readWord(int index) {
        return this.payload.getShort(index);
    }

    public byte readByte(int index) {
        return this.payload.get(index);
    }

    public void serialize(ByteArrayOutputStream stream) throws IOException {
        super.serialize(stream);

        if (this.payload != null && this.payload.capacity() > 0) {
            stream.write(this.payload.capacity());
            byte[] payload = new byte[this.payload.capacity()];
            this.payload.rewind();
            this.payload.get(payload);
            stream.write(payload);
        }
    }

    public boolean payloadEquals(SerialDataPacket otherPacket) {
        return this.payload.rewind().equals(otherPacket.payload.rewind());
    }
}
