package de.jlab.cardroid.car;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.ObservableFeature;

public interface CanObservable extends ObservableFeature<CanObservable.CanPacketListener> {

    @Override
    default Class<? extends DeviceDataProvider> getProviderClass() {
        return CanDataProvider.class;
    }

    interface CanPacketListener extends ObservableFeature.Listener {
        void onReceive(@NonNull CanPacket packet);
    }

}
