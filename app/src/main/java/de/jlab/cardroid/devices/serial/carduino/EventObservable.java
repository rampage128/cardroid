package de.jlab.cardroid.devices.serial.carduino;

import de.jlab.cardroid.devices.ObservableFeature;

public interface EventObservable extends ObservableFeature<EventObservable.EventListener> {

    interface EventListener extends ObservableFeature.Listener {
        void onEvent(int eventNum);
    }
}
