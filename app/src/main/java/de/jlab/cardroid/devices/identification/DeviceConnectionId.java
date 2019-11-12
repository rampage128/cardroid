package de.jlab.cardroid.devices.identification;

import android.bluetooth.BluetoothDevice;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class DeviceConnectionId {

    private String connectionId;

    private DeviceConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public static DeviceConnectionId fromUsbDevice(@NonNull UsbDevice device) {
        return new DeviceConnectionId(device.getDeviceName());
    }

    public static DeviceConnectionId fromBluetoothDevice(@NonNull BluetoothDevice device) {
        return new DeviceConnectionId(device.getAddress());
    }

    public boolean equals(@Nullable DeviceConnectionId other) {
        return other != null && this.connectionId.equals(other.connectionId);
    }

    @Override
    public int hashCode() {
        return this.connectionId.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return this.connectionId;
    }
}
