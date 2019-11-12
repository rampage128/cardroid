package de.jlab.cardroid.devices.serial.carduino;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.ObservableFeature;
import de.jlab.cardroid.devices.identification.DeviceUid;

public interface EventObservable extends ObservableFeature<EventObservable.EventListener> {

    interface EventListener extends ObservableFeature.Listener {
        void onEvent(int eventNum, @NonNull DeviceUid deviceUid);
    }
}
