package de.jlab.cardroid.car;

import de.jlab.cardroid.devices.DeviceDataObservable;

public interface CanObservable extends DeviceDataObservable {

    void addCanListener(CanPacketListener listener);
    void removeCanListener(CanPacketListener listener);

    interface CanPacketListener {
        void onReceive(CanPacket packet);
    }

}
