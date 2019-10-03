package de.jlab.cardroid.devices.serial.can;

import de.jlab.cardroid.car.CanPacketDescriptor;
import de.jlab.cardroid.devices.DeviceHandler;

public interface CanDeviceHandler extends DeviceHandler {

    void addCanListener(CanPacketListener listener);
    void removeCanListener(CanPacketListener listener);

    void registerCanId(CanPacketDescriptor descriptor);
    void unregisterCanId(CanPacketDescriptor descriptor);

    void startSniffer();
    void stopSniffer();

    interface CanPacketListener {
        void onReceive(CanPacket packet);
    }

}
