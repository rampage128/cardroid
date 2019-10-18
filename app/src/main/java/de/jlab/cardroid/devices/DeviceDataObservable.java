package de.jlab.cardroid.devices;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;

public interface DeviceDataObservable {

    void setDevice(@NonNull DeviceHandler device);
    DeviceConnectionId getConnectionId();

}
