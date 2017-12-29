package de.jlab.cardroid.usb.gps;

import android.util.Log;

import java.util.ArrayList;

import de.jlab.cardroid.usb.SerialConnectionManager;

public class GPSSerialReader implements SerialConnectionManager.SerialConnectionListener {

    private GpsPosition position = new GpsPosition();

    private StringBuilder dataLine = new StringBuilder();
    private boolean newLine = false;
    private boolean initialized = false;

    private ArrayList<PositionListener> positionListeners = new ArrayList<>();

    @Override
    public void onReceiveData(byte[] data) {
        if (data.length == 0) {
            return;
        }

        if (!initialized) {
            if (data[0] != 0x24) {
                Log.d("GPS", this.bytesToHex(data) + ": " + new String(data));
                for (PositionListener listener : positionListeners) {
                    listener.onError();
                }
            }
            else {
                this.initialized = true;
            }
        }

        for (int i = 0; i < data.length; i++) {
            byte dataByte = data[0];
            if (dataByte == 0x0D) {
                this.newLine = true;
            }
            else if (dataByte == 0x0A) {
                if (this.newLine) {
                    if (this.parse(this.dataLine.toString())) {
                        for (PositionListener listener : positionListeners) {
                            listener.onUpdate(this.position);
                        }
                        Log.d("GPS", this.position.toString());
                    }
                    this.dataLine.setLength(0);
                    this.newLine = false;
                }
            }
            else {
                this.dataLine.append((char)dataByte);
            }
        }
    }

    private final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

    private boolean parse(String line) {
        if (!line.startsWith("$")) {
            return false;
        }

        String[] tokens = line.split(",");
        NMEAParser parser = NMEAParser.createFrom(tokens[0].substring(1));
        if (parser == null) {
            return false;
        }

        parser.parseSentence(tokens);
        return this.position.update(parser);
    }

    @Override
    public void onConnect() {
        initialized = false;
        Log.d("GPS", "connected");
    }

    @Override
    public void onDisconnect() {
        initialized = false;
        Log.d("GPS", "disconnected");
    }

    public void addPositionListener(PositionListener listener) {
        this.positionListeners.add(listener);
    }

    public void removePositionListener(PositionListener listener) {
        this.positionListeners.remove(listener);
    }

    public interface PositionListener {
        void onUpdate(GpsPosition position);
        void onError();
    }
}
