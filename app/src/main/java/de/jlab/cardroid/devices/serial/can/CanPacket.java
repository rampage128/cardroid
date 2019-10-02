package de.jlab.cardroid.devices.serial.can;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

public class CanPacket {

    private long canId = 0;
    private BitSet bits;
    private byte[] data;

    public CanPacket(long canId, byte[] data) {
        this.canId = canId;
        this.data = data;

        // BitSet wants to be initialized with an array in Little Endian.
        // reverse the array
        byte[] reversed = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            reversed[i] = data[data.length - 1 - i];
        }
        this.bits = BitSet.valueOf(reversed);
    }

    public void updateData(byte[] data) {
        this.bits = BitSet.valueOf(data);
    }

    public int getDataLength() {
        return this.bits.length()/8;
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


    private long read(int start, int length, ByteOrder order) {
        BitSet readValue = this.bits.get(start, start + length);
        ByteBuffer bytes = ByteBuffer.wrap(readValue.toByteArray());
        // bytes are in Little Endian. But because we've swapped endianness in the intialized
        // we need to invert endianness here.
        if (order == ByteOrder.LITTLE_ENDIAN) {
            bytes.order(ByteOrder.BIG_ENDIAN);
        } else {
            bytes.order(ByteOrder.LITTLE_ENDIAN);
        }

        if (length <= 8) {
            return bytes.get();
        } else if (length <= 16) {
            return bytes.getShort();
        } else if (length <= 32) {
            return bytes.getInt();
        } else {
            return bytes.getLong();
        }
    }

    public long readOriginal(int startBit, int bitLength, ByteOrder order) {
        long result = 0;
        int currentByte = (int)Math.floor((startBit + 1) / 8f);
        int currentBit;

        for (int i = startBit; i < startBit + bitLength; i++) {
            // get the offset bit index in current data byte
            int offsetBit = i % 8;
            // Retrieve byte for bit-index from data array
            if (offsetBit == 0) {
                currentByte = (int)Math.floor((i + 1) / 8f);
                // TODO make readBigEndian shift whole bytes to make a little endian version
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

    public byte[] getData() {
        return this.data;
    }

}
