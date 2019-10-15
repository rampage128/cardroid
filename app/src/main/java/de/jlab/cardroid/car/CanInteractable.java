package de.jlab.cardroid.car;

import de.jlab.cardroid.devices.Interactable;

public interface CanInteractable extends Interactable {
    void registerCanId(CanPacketDescriptor descriptor);
    void unregisterCanId(CanPacketDescriptor descriptor);

    void startSniffer();
    void stopSniffer();
}
