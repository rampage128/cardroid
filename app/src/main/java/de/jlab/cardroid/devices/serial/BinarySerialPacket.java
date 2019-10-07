package de.jlab.cardroid.devices.serial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import androidx.annotation.NonNull;

public abstract class BinarySerialPacket implements SerialPacket {

    protected byte[] data;

    public BinarySerialPacket(@NonNull byte[] rawData) {
        this.data = rawData;
    }

    @Override
    public void serialize(@NonNull ByteArrayOutputStream stream) throws IOException {
        stream.write(this.data);
    }

    protected int readByte(int index) {
        return this.data[index] & 0xFF;
    }

    protected long readDWord(int index) {
        long value = 0;
            for (int i = index; i < index + 4; i++) {
            value = (value << 8) + (this.data[i] & 0xff);
        }
        return value;
    }

    protected byte[] readBytes(int startIndex, int length) {
        return Arrays.copyOfRange(this.data, startIndex, startIndex + length);
    }

    protected byte[] readBytes(int startIndex) {
        return Arrays.copyOfRange(this.data, startIndex, this.data.length);
    }

}
