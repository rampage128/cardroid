package de.jlab.cardroid.devices;

import androidx.annotation.NonNull;

public interface DeviceObserver {
    void onStart(@NonNull DeviceHandler device);
    DeviceDataProvider onFeatureDetected(@NonNull Class<? extends DeviceDataProvider> feature, @NonNull DeviceHandler device);
    void onEnd(@NonNull DeviceHandler device);
}
