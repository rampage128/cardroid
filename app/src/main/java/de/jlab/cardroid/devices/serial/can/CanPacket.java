package de.jlab.cardroid.devices.serial.can;

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
        return this.read(startBit, bitLength);
    }

    public long readLittleEndian(int startBit, int bitLength) {
        return this.read(startBit, bitLength);
    }

    private long read(int startBit, int bitLength) {
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

        return result;
    }

    public byte[] getData() {
        return this.data;
    }

}
