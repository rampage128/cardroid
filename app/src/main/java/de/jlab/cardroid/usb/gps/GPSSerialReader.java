package de.jlab.cardroid.usb.gps;

import android.util.Log;

import java.util.ArrayList;

import de.jlab.cardroid.usb.SerialConnectionManager;

public class GPSSerialReader implements SerialConnectionManager.SerialConnectionListener {

    private GpsPosition position = new GpsPosition();
    private StringBuilder dataLine = new StringBuilder();
    private ArrayList<PositionListener> positionListeners = new ArrayList<>();

    @Override
    public void onReceiveData(byte[] data) {
        if (data.length == 0) {
            return;
        }

        for (int i = 0; i < data.length; i++) {
            byte dataByte = data[i];
            if (dataByte == 0x0D || dataByte == 0x0A) {
                continue;
            }
            else if (dataByte == 0x24) {
                String rawData = this.dataLine.toString();
                if (this.parse(rawData)) {
                    for (PositionListener listener : positionListeners) {
                        listener.onUpdate(this.position, rawData);
                    }
                }
                this.dataLine.setLength(0);
            }
            this.dataLine.append((char)dataByte);
        }
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

        parser.parseSentence(tokens, this.position);
        return true;
    }

    @Override
    public void onConnect() {
        Log.d("GPS", "connected");
    }

    @Override
    public void onDisconnect() {
        Log.d("GPS", "disconnected");
    }

    public void addPositionListener(PositionListener listener) {
        this.positionListeners.add(listener);
    }

    public void removePositionListener(PositionListener listener) {
        this.positionListeners.remove(listener);
    }

    public interface PositionListener {
        void onUpdate(GpsPosition position, String rawData);
    }
}
