package de.jlab.cardroid.devices;

import androidx.annotation.NonNull;

/* TODO add bluetooth support
 * - create a device wrapper to unify usb and bluetooth devices
 * - create a devicehandler wrapper to unify usb and bluetooth handling
 * - create a serial connection wrapper to unify usb and bluetooth serial connection
 */
public interface DeviceHandler {

    int getDeviceId();
    boolean connectDevice();
    void disconnectDevice();
    boolean isConnected();
    @NonNull
    Class<?>[] getFeatures();

}
