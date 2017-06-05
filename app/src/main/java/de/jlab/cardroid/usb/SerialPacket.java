package de.jlab.cardroid.usb;

public abstract class SerialPacket {
    private byte id;


    public SerialPacket(byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }
}
