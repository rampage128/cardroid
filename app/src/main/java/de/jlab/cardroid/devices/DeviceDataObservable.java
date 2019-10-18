package de.jlab.cardroid.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface DeviceDataObservable {

    void setDevice(@NonNull DeviceHandler device);
    @Nullable
    DeviceHandler getDevice();

}
