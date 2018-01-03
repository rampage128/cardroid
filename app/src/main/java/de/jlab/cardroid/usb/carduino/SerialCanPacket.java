package de.jlab.cardroid.usb.carduino;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SerialCanPacket extends SerialDataPacket {
    public SerialCanPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
    }

    public SerialCanPacket(byte id, byte[] payload) {
        super(id, payload);
    }

    public long getCanId() {
        return this.readDWord(0);
    }

    public byte[] getDataRaw() {
        this.payload.position(4);
        byte[] data = new byte[this.payload.remaining()];
        this.payload.get(data);
        return data;
    }

    private final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public String getDataHex() {
        StringBuilder hexBuilder = new StringBuilder();

        byte[] bytes = this.getDataRaw();
        for (byte raw : bytes) {
            int value = raw & 0xFF;
            hexBuilder.append(" ")
                      .append(hexArray[value >>> 4])
                      .append(hexArray[value & 0x0F]);
        }
        return hexBuilder.toString().trim();
    }
}
