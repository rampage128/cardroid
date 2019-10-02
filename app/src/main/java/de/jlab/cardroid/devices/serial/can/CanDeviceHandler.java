package de.jlab.cardroid.devices.serial.can;

import de.jlab.cardroid.devices.DeviceHandler;

public interface CanDeviceHandler extends DeviceHandler {

    void addCanListener(CanPacketListener listener);

    void removeCanListener(CanPacketListener listener);

    interface CanPacketListener {
        void onReceive(CanPacket packet);
    }

}
