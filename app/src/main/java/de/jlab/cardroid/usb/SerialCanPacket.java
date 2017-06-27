package de.jlab.cardroid.usb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

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
        byte[] data = this.getPayload();
        return Arrays.copyOfRange(data, 4, data.length);
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
