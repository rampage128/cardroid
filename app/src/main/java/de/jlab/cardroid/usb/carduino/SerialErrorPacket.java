package de.jlab.cardroid.usb.carduino;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SerialErrorPacket extends SerialPacket {

    public SerialErrorPacket(ByteArrayInputStream stream) throws IOException {
        super(stream);
    }

}
