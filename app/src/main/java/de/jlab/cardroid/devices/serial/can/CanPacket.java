package de.jlab.cardroid.devices.serial.can;

import java.nio.ByteOrder;

public class CanPacket {

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
        int currentByte = startBit / 8;
        int currentBit;

        for (int i = startBit; i < startBit + bitLength;) {
            // get the offset bit index in current data byte
            int offsetBit = i % 8;
            // Calculate how many bits can we read simultaneously
            byte bitSize = (byte) (Math.min(8, bitLength + startBit - i) - offsetBit);
            currentByte = i / 8;
            if (bitSize == 8) {
                // fast path
                result <<= 8;
                result  |= this.data[currentByte] & 0xFF;
            } else {
                result <<= bitSize;
                // offset the chunk we are interested in, then mask it
                result |= (byte)((this.data[currentByte] >> (8 - bitSize - offsetBit)) & (( 1 << bitSize ) - 1));
            }

            i += bitSize;
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

    public byte[] getData() {
        return this.data;
    }

}