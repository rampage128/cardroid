package de.jlab.cardroid.devices.serial.gps;

import java.util.ArrayList;

import de.jlab.cardroid.devices.serial.SerialReader;

public final class GpsPositionParser implements SerialReader.SerialPacketListener<GpsSerialPacket> {

    private long lastPositionTime = 0;
    private GpsPosition position = new GpsPosition();
    private ArrayList<PositionListener> positionListeners = new ArrayList<>();


    public void addPositionListener(PositionListener listener) {
        this.positionListeners.add(listener);
    }

    public void removePositionListener(PositionListener listener) {
        this.positionListeners.remove(listener);
    }

    @Override
    public void onReceivePackets(GpsSerialPacket[] packets) {
        for (GpsSerialPacket packet : packets) {
            if (this.parse(packet)) {
                for (PositionListener listener : positionListeners) {
                    listener.onUpdate(this.position, packet);
                }
            }
        }
    }

    private boolean parse(GpsSerialPacket packet) {
        NmeaParser parser = NmeaParser.createFrom(packet.readSentenceType());
        if (parser == null) {
            return false;
        }

        parser.parseSentence(packet, this.position);

        long newPositionTime = this.position.getLocation().getTime();
        if (newPositionTime > this.lastPositionTime) {
            this.lastPositionTime = newPositionTime;
            return true;
        }

        return false;
    }

    public interface PositionListener {
        void onUpdate(GpsPosition position, GpsSerialPacket packet);
    }

}
