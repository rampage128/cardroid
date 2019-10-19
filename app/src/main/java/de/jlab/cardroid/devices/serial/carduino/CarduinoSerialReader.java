package de.jlab.cardroid.devices.serial.carduino;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.serial.SerialReader;

public final class CarduinoSerialReader extends SerialReader<CarduinoSerialPacket> {

    @Override
    protected CarduinoSerialPacket[] createPackets(@NonNull byte[] data) {
        ArrayList<CarduinoSerialPacket> packetList = new ArrayList<>();

        boolean foundPacket = false;
        int packetStartIndex = 0;
        byte packetType = 0x00;
        byte packetId = 0x00;
        for (int i = 0; i < data.length; i++) {
            byte currentByte = data[i];

            if (!foundPacket) {
                if (currentByte == CarduinoSerialPacket.HEADER) {
                    foundPacket = true;
                    packetStartIndex = i;
                }
                continue;
            }

            if (i == packetStartIndex + 1) {
                packetType = currentByte;
                continue;
            }

            if (i == packetStartIndex + 2) {
                packetId = currentByte;
                continue;
            }

            if (i == packetStartIndex + 3) {
                int length = -1;
                if (currentByte != CarduinoSerialPacket.FOOTER) {
                    length = (int) currentByte & 0xFF;
                    // TODO if packet is not complete at the end of array, we might want to store the already received data and try to continue with the next batch of data...
                    if (i + length + 1 >= data.length || data[i + length + 1] != CarduinoSerialPacket.FOOTER) {
                        // got wrong packet length? Hard to tell why ... lets just skip this packet
                        Log.w(this.getClass().getSimpleName(), "Wrong length given for packet type \"" + String.format("%02X", packetType) + "\" with id \"" + String.format("%02X", packetId) + "\". Length given: " + length);
                        this.dumpData(Log.WARN, "Data", data);
                        foundPacket = false;
                        packetType = 0x00;
                        continue;
                    }
                }

                CarduinoSerialPacket packet = new CarduinoSerialPacket(Arrays.copyOfRange(data, i - 2, i + length + 1));
                packetList.add(packet);

                foundPacket = false;
                packetId = 0x00;
                packetType = 0x00;
                i += length + 1;
            }
        }

        return packetList.toArray(new CarduinoSerialPacket[0]);
    }

    private void dumpData(int priority, String caption, byte[] data) {
        StringBuilder builder = new StringBuilder(caption).append(" -> ");
        for (byte b : data) {
            builder.append(" ");
            builder.append(String.format("%02X", b));
        }
        Log.println(priority, this.getClass().getSimpleName(), builder.toString());
    }

}
