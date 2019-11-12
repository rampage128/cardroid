package de.jlab.cardroid.devices.serial.gps;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.serial.SerialReader;

public final class GpsSerialReader extends SerialReader<GpsSerialPacket> {

    private StringBuilder dataLine = new StringBuilder();

    @Override
    protected GpsSerialPacket[] createPackets(@NonNull byte[] data) {
        ArrayList<GpsSerialPacket> packets = new ArrayList<>();
        for (byte dataByte : data) {
            if (dataByte == 0x0D || dataByte == 0x0A) {
                continue;
            } else if (dataByte == 0x24) {
                String rawData = this.dataLine.toString();
                if (rawData.startsWith("$")) {
                    packets.add(new GpsSerialPacket(this.dataLine.toString()));
                }
                this.dataLine.setLength(0);
            }
            this.dataLine.append((char) dataByte);
        }
        return packets.toArray(new GpsSerialPacket[0]);
    }
}
