package de.jlab.cardroid.car;

import de.jlab.cardroid.devices.Feature;

public interface CanInteractable extends Feature {
    void registerCanId(CanPacketDescriptor descriptor);
    void unregisterCanId(CanPacketDescriptor descriptor);
    void sendPacket(int canId, byte[] data);

    void startSniffer();
    void stopSniffer();
}
