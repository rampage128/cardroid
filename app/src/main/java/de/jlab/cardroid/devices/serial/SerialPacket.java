package de.jlab.cardroid.devices.serial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;

public interface SerialPacket {

    void serialize(@NonNull ByteArrayOutputStream stream) throws IOException;

}
