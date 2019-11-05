package de.jlab.cardroid.devices;

public interface Feature {

    enum State {
        AVAILABLE,
        UNAVAILABLE
    }

    void setDevice(Device device);
    Device getDevice();

}
