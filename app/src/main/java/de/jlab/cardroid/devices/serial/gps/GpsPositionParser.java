package de.jlab.cardroid.devices.serial.gps;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.serial.SerialReader;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDevice;
import de.jlab.cardroid.gps.GpsObservable;

public final class GpsPositionParser implements SerialReader.SerialPacketListener<GpsSerialPacket>, GpsObservable {

    private GpsUsbDevice device;
    private long lastPositionTime = 0;
    private GpsPosition position = new GpsPosition();
    private ArrayList<PositionListener> positionListeners = new ArrayList<>();

    @Override
    public void setDevice(@NonNull Device device) {
        this.device = (GpsUsbDevice)device;
    }

    @Nullable
    @Override
    public Device getDevice() {
        return this.device;
    }

    public void addListener(@NonNull PositionListener listener) {
        this.positionListeners.add(listener);
    }

    public void removeListener(@NonNull PositionListener listener) {
        this.positionListeners.remove(listener);
    }

    @Override
    public void onReceivePackets(GpsSerialPacket[] packets) {
        for (GpsSerialPacket packet : packets) {
            // FIXME: It doesn't make sense to hand in the sentence like this, because parse() only returns true for every group of sentences.
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
