package de.jlab.cardroid.cansniffer;

import de.jlab.cardroid.usb.carduino.SerialCanPacket;

public class SniffedCanPacket {
    private long timestamp;
    private SerialCanPacket packet;

    public SniffedCanPacket(SerialCanPacket packet) {
        this.timestamp = System.currentTimeMillis();
        this.packet = packet;
    }

    public boolean isExpired(long timeout) {
        return System.currentTimeMillis() - this.timestamp > timeout;
    }

    public long getCanId() {
        return this.packet.getCanId();
    }

    public String getDataHex() {
        return this.packet.getDataHex();
    }

    @Override
    public boolean equals(Object obj) {
        return  obj instanceof SniffedCanPacket &&
                this.packet.payloadEquals(((SniffedCanPacket)obj).packet);
    }
}
