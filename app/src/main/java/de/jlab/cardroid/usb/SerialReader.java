package de.jlab.cardroid.usb;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class SerialReader implements SerialConnectionManager.CarduinoListener {
    private static final String LOG_TAG = "SerialReader";

    private ArrayList<SerialPacketListener> packetListeners = new ArrayList<>();


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
                if (currentByte == 0x3E) {
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
                int length = (int) currentByte & 0xFF;

                if (data[i + length + 1] != 0x7C) {
                    // got wrong packet length? Hard to tell why ... lets just skip this packet
                    Log.d(LOG_TAG, "Wrong length given for packet type \"" + byteToHexString(packetType) + "\" with id \"" + byteToHexString(packetId) + "\". Length given: " + length);
                    foundPacket = false;
                    packetType = 0x00;
                    continue;
                }

                // TODO SCAN AHEAD TO CHECK IF DATA IS LONG ENOUGH ... IF NOT, STORE INCOMPLETE PACKAGE OR DROP IT
                byte[] payload = Arrays.copyOfRange(data, i + 1, i + length + 1);

                SerialPacket packet = null;
                switch (packetType) {
                    case 0x73:
                        packet = new CarSystemSerialPacket(packetId, payload);
                        break;
                }

                if (packet != null) {
                    packetList.add(packet);
                } else {
                    Log.d(LOG_TAG, "Unknown Id for packet type \"" + byteToHexString(packetType) + "\" with id \"" + byteToHexString(packetId) + "\".");
                }

                foundPacket = false;
                packetId = 0x00;
                packetType = 0x00;
                i += length + 2;
            }
        }

        if (!packetList.isEmpty()) {
            for (SerialPacketListener listener : packetListeners) {
                listener.onReceivePackets(packetList);
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

    public interface SerialPacketListener {
        void onReceivePackets(ArrayList<SerialPacket> packets);
    }
}
