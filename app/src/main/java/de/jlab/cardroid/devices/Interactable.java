package de.jlab.cardroid.devices;

import de.jlab.cardroid.devices.identification.DeviceConnectionId;

public interface Interactable {

    void setDevice(DeviceHandler device);
    DeviceConnectionId getConnectionId();

}
