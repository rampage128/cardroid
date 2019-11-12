package de.jlab.cardroid.devices.serial.carduino;

import de.jlab.cardroid.devices.Feature;

public interface EventInteractable extends Feature {

    void sendEvent(byte eventId, byte[] payload);

}
