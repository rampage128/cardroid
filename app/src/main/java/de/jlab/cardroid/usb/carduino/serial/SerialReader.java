package de.jlab.cardroid.usb.carduino.serial;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import de.jlab.cardroid.usb.SerialConnection;
import de.jlab.cardroid.usb.UsageStatistics;

public class SerialReader implements SerialConnection.SerialConnectionListener {
    private static final String LOG_TAG = "SerialReader";

    private ArrayList<SerialPacketListener> packetListeners = new ArrayList<>();
    private UsageStatistics packetUsage = new UsageStatistics(1000, 60);

    @Override
    public void onReceiveData(byte[] data) {
        ArrayList<SerialPacket> packetList = new ArrayList<>();

        boolean foundPacket = false;
        int packetStartIndex = 0;
        byte packetType = 0x00;
        byte packetId = 0x00;
        for (int i = 0; i < data.length; i++) {
            byte currentByte = data[i];

            if (!foundPacket) {
                if (currentByte == SerialPacketStructure.HEADER) {
                    foundPacket = true;
                    packetStartIndex = i;
                }
                continue;
            }

            if (foundPacket && i == packetStartIndex + 1) {
                packetType = currentByte;
                continue;
            }

            if (foundPacket && i == packetStartIndex + 2) {
                packetId = currentByte;
                continue;
            }

            if (foundPacket && i == packetStartIndex + 3) {
                int length = -1;
                if (currentByte != SerialPacketStructure.FOOTER) {
                    length = (int) currentByte & 0xFF;
                    if (i + length + 1 >= data.length || data[i + length + 1] != SerialPacketStructure.FOOTER) {
                        // got wrong packet length? Hard to tell why ... lets just skip this packet
                        Log.d(LOG_TAG, "Wrong length given for packet type \"" + byteToHexString(packetType) + "\" with id \"" + byteToHexString(packetId) + "\". Length given: " + length);
                        foundPacket = false;
                        packetType = 0x00;
                        continue;
                    }
                }

                // Count every packet, even unsuccessfull ones
                packetUsage.count();
                ByteArrayInputStream packetStream = new ByteArrayInputStream(Arrays.copyOfRange(data, i - 2, i + length + 1));
                try {
                    SerialPacket packet = SerialPacketFactory.getPacketFromData(packetStream);
                    packetList.add(packet);
                } catch (DeserializationException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                } catch (UnknownPacketTypeException e) {
                    Log.d(LOG_TAG, e.getMessage());
                }

                foundPacket = false;
                packetId = 0x00;
                packetType = 0x00;
                i += length + 1;
            }
        }

        if (!packetList.isEmpty()) {
            for(int i = 0; i < packetListeners.size(); i++){
                packetListeners.get(i).onReceivePackets(packetList);
            }
        }
    }

    private final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private String byteToHexString(byte data) {
        char[] hexChars = new char[2];
        int v = data & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
        return new String(hexChars);
    }

    @Override
    public void onConnect() {
        // Intentionally left blank
    }

    @Override
    public void onDisconnect() {
        // Intentionally left blank
    }

    public void addListener(SerialPacketListener packetListener) {
        this.packetListeners.add(packetListener);
    }

    public void removeListener(SerialPacketListener packetListener) {
        this.packetListeners.remove(packetListener);
    }

    public interface SerialPacketListener {
        void onReceivePackets(ArrayList<SerialPacket> packets);
    }

    public void addPacketStatisticListener(UsageStatistics.UsageStatisticsListener listener) {
        this.packetUsage.addListener(listener);
    }

    public void removePacketStatisticListener(UsageStatistics.UsageStatisticsListener listener) {
        this.packetUsage.removeListener(listener);
    }
}
