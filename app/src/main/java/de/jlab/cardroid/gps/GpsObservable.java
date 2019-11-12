package de.jlab.cardroid.gps;

import de.jlab.cardroid.devices.ObservableFeature;
import de.jlab.cardroid.devices.serial.gps.GpsPosition;

public interface GpsObservable extends ObservableFeature<GpsObservable.PositionListener> {

    interface PositionListener extends ObservableFeature.Listener {
        // TODO: Remove sentence from callback. That is serial shit that has to go somewhere else later
        void onUpdate(GpsPosition position, String sentence);
    }
}
