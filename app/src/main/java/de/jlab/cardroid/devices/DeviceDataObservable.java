package de.jlab.cardroid.devices;

import androidx.annotation.NonNull;

public interface DeviceDataObservable {

    void setDevice(@NonNull DeviceHandler device);
    long getDeviceId();

}
