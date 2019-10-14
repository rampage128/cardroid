package de.jlab.cardroid.car;

import java.nio.ByteOrder;
import java.util.Arrays;

public final class CanPacket {

    private long canId = 0;
    private byte[] data;

    public CanPacket(long canId, byte[] data) {
        this.canId = canId;
        this.data = data;
    }

    public void updateData(byte[] data) {
        this.data = data;
    }

    public int getDataLength() {
        return this.data.length;
    }

    public long getCanId() {
        return this.canId;
    }

    public long readBigEndian(int startBit, int bitLength) {
        return this.read(startBit, bitLength, ByteOrder.BIG_ENDIAN);
    }

    public long readLittleEndian(int startBit, int bitLength) {
        return this.read(startBit, bitLength, ByteOrder.LITTLE_ENDIAN);
    }

    private long read(int startBit, int bitLength, ByteOrder order) {
        long result = 0;
        int currentByte = (int)Math.floor((startBit + 1) / 8f);
        int currentBit;

        for (int i = startBit; i < startBit + bitLength; i++) {
            // get the offset bit index in current data byte
            int offsetBit = i % 8;
            // Retrieve byte for bit-index from data array
            if (offsetBit == 0) {
                currentByte = (int)Math.floor((i + 1) / 8f);
            }

            result <<= 1;

            // transfer current bit if 1
            currentBit = (this.data[currentByte] >> (7 - offsetBit)) & 0x01;
            if (currentBit != 0) {
                result |= 0x01;
            }
        }

        if (order == ByteOrder.LITTLE_ENDIAN) {
            //swap bytes in the result
            if (bitLength <= 8) {
                return (byte)result;
            } else if (bitLength <= 16) {
                return Short.reverseBytes((short)result);
            } else if (bitLength <= 32) {
                return Integer.reverseBytes((int)result);
            } else {
                return Long.reverseBytes(result);
            }
        }

        return result;
    }

    // TODO: Might be nice to be able to read complex flags using bitIndex, bitLength and a mask to compare?
    public long readFlag(int bitIndex) {
        return this.readBigEndian(bitIndex, 1);
    }

    // TODO: Maybe we should zero the bits that are outside of startBit and and startBit + bitLength?
    public byte[] readBytes(int startBit, int bitLength) {
        int startByte = startBit / 8;
        int endByte = startBit + bitLength / 8;

        return Arrays.copyOfRange(this.data, startByte, endByte);
    }

    public byte[] getData() {
        return this.data;
    }

}
