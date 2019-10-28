package de.jlab.cardroid.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ObservableFeature<LT extends ObservableFeature.Listener> extends Feature {

    void addListener(@NonNull LT listener);
    void removeListener(@NonNull LT listener);

    void setDevice(@NonNull Device device);
    @Nullable
    Device getDevice();

    interface Listener {}

}
