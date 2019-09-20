package de.jlab.cardroid.usb.gps.serial;

import android.util.Log;

import java.util.ArrayList;

import de.jlab.cardroid.usb.SerialConnection;
import de.jlab.cardroid.usb.UsageStatistics;
import de.jlab.cardroid.usb.gps.GpsPosition;

public class GPSSerialReader implements SerialConnection.SerialConnectionListener {

    private long lastPositionTime = 0;
    private GpsPosition position = new GpsPosition();
    private StringBuilder dataLine = new StringBuilder();
    private ArrayList<PositionListener> positionListeners = new ArrayList<>();
    private UsageStatistics sentenceStatistics = new UsageStatistics(1000, 60);
    private UsageStatistics updateStatistics = new UsageStatistics(1000, 60);

    @Override
    public void onReceiveData(byte[] data) {
        if (data.length == 0) {
            return;
        }

        for (byte dataByte : data) {
            if (dataByte == 0x0D || dataByte == 0x0A) {
                continue;
            } else if (dataByte == 0x24) {
                String rawData = this.dataLine.toString();
                if (this.parse(rawData)) {
                    for (PositionListener listener : positionListeners) {
                        listener.onUpdate(this.position, rawData);
                    }
                    updateStatistics.count();
                }
                sentenceStatistics.count();
                this.dataLine.setLength(0);
            }
            this.dataLine.append((char) dataByte);
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

        long newPositionTime = this.position.getLocation().getTime();
        if (newPositionTime > this.lastPositionTime) {
            this.lastPositionTime = newPositionTime;
            return true;
        }

        return false;
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

    public void addSentenceStatisticListener(UsageStatistics.UsageStatisticsListener listener) {
        this.sentenceStatistics.addListener(listener);
    }

    public void removeSentenceStatisticListener(UsageStatistics.UsageStatisticsListener listener) {
        this.sentenceStatistics.removeListener(listener);
    }

    public void addUpdateStatisticListener(UsageStatistics.UsageStatisticsListener listener) {
        this.updateStatistics.addListener(listener);
    }

    public void removeUpdateStatisticListener(UsageStatistics.UsageStatisticsListener listener) {
        this.updateStatistics.removeListener(listener);
    }

    public interface PositionListener {
        void onUpdate(GpsPosition position, String rawData);
    }
}
