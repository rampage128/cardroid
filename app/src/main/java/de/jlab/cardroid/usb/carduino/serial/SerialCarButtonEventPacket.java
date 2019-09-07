package de.jlab.cardroid.usb.carduino.serial;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import de.jlab.cardroid.usb.carduino.serial.SerialDataPacket;

public class SerialCarButtonEventPacket extends SerialDataPacket {
    public SerialCarButtonEventPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
    }

    public SerialCarButtonEventPacket(byte id, byte[] payload) {
        super(id, payload);
    }
}
