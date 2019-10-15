package de.jlab.cardroid.devices;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.identification.DeviceUid;

public interface DeviceObserver {
    void onStart(@NonNull DeviceHandler device);
    void deviceUidReceived(@NonNull DeviceUid uid, @NonNull DeviceHandler device);
    DeviceDataProvider onFeatureDetected(@NonNull Class<? extends DeviceDataProvider> feature, @NonNull DeviceHandler device);
    void onEnd(@NonNull DeviceHandler device);
}
