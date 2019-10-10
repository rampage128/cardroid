package de.jlab.cardroid.devices.serial.gps;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.serial.SerialReader;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDeviceHandler;
import de.jlab.cardroid.gps.GpsObservable;

public final class GpsPositionParser implements SerialReader.SerialPacketListener<GpsSerialPacket>, GpsObservable {

    private GpsUsbDeviceHandler device;
    private long lastPositionTime = 0;
    private GpsPosition position = new GpsPosition();
    private ArrayList<PositionListener> positionListeners = new ArrayList<>();

    @Override
    public void setDevice(@NonNull DeviceHandler device) {
        this.device = (GpsUsbDeviceHandler)device;
    }

    @Override
    public long getDeviceId() {
        return this.device.getDeviceId();
    }

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
                    listener.onUpdate(this.position, packet.readSentence());
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

}
