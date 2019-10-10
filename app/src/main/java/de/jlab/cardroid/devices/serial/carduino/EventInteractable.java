package de.jlab.cardroid.devices.serial.carduino;

import de.jlab.cardroid.devices.Interactable;

public interface EventInteractable extends Interactable {

    void sendEvent(byte eventId, byte[] payload);

}
