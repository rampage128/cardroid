package de.jlab.cardroid.devices.serial.carduino;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceDataObservable;

public interface EventObservable extends DeviceDataObservable {

    void addEventListener(@NonNull EventListener listener);
    void removeEventListener(@NonNull EventListener listener);

    interface EventListener {
        void onEvent(int eventNum);
    }

}
