package de.jlab.cardroid.usb.carduino.serial;

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

    public String readString(int index, int length) {
        return new String(readByteArray(index, length));
    }

    public byte[] readByteArray(int index, int length) {
        byte[] bytes = new byte[length];
        this.payload.get(bytes, index, length);
        return bytes;
    }

    public long readNumber(int index, int length) {
        long value = 0;
        for (int i = index; i < index + length; i++) {
            value = (value << 8) + (this.payload.get(i) & 0xff);
        }
        return value;
    }

    public long readNumberLittleEndian(int index, int length) {
        long value = 0;
        int lastIndex = index + length - 1;
        for (int i = index; i < index + length; i++) {
            value = (value << 8) + (this.payload.get(lastIndex - i) & 0xff);
        }
        return value;
    }

    public long readDWord(int index) {
        return this.payload.getInt(index);
    }

    public int readWord(int index) {
        return this.payload.getShort(index);
    }

    public int readUnsignedByte(int index) {
        return this.payload.get(index) & 0xFF;
    }

    public byte readByte(int index) {
        return this.payload.get(index);
    }

    public int payloadSize() {
        return this.payload.capacity();
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

    public String payloadAsHexString() {
        byte[] payload = new byte[this.payload.capacity()];
        this.payload.rewind();
        this.payload.get(payload);

        StringBuilder sb = new StringBuilder();
        for (byte b : payload) {
            sb.append("0x");
            sb.append(String.format("%02x", b));
            sb.append(" ");
        }
        return sb.toString();
    }

    public boolean payloadEquals(SerialDataPacket otherPacket) {
        return this.payload.rewind().equals(otherPacket.payload.rewind());
    }
}
