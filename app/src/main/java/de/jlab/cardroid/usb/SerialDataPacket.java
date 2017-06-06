package de.jlab.cardroid.usb;

public abstract class SerialDataPacket extends SerialPacket {
    private byte[] payload;

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
}
