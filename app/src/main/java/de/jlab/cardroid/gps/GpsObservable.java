package de.jlab.cardroid.gps;

import de.jlab.cardroid.devices.DeviceDataObservable;
import de.jlab.cardroid.devices.serial.gps.GpsPosition;

public interface GpsObservable extends DeviceDataObservable {

    void addPositionListener(PositionListener listener);
    void removePositionListener(PositionListener listener);

    interface PositionListener {
        void onUpdate(GpsPosition position, String sentence);
    }

}
