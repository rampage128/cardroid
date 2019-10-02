package de.jlab.cardroid.devices.serial;

public interface SerialConnectionListener {
    void onConnect();
    void onDisconnect();
}
