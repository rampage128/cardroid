package de.jlab.cardroid.devices.identification;

import android.bluetooth.BluetoothDevice;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class DeviceUid {

    private String uid;

    public DeviceUid(@NonNull byte[] bytes) {
        this(new String(bytes));
    }

    public DeviceUid(@NonNull String uid) {
        this.uid = uid;
    }

    public boolean isUnique() {
        return !this.uid.endsWith("!");
    }

    @NonNull
    public static DeviceUid fromUsbDevice(@NonNull UsbDevice device) {
        String uidString = "";

        uidString += String.format("%04x", (short)device.getVendorId()).toUpperCase();
        uidString += String.format("%04x", (short)device.getProductId()).toUpperCase();

        String serialNumber = device.getSerialNumber();
        if (serialNumber != null) {
            uidString += "-" + serialNumber;
        } else {
            uidString += "!";
        }

        return new DeviceUid(uidString);
    }

    @Nullable
    public static DeviceUid fromBluetoothDevice(@NonNull BluetoothDevice device) {
        String address = device.getAddress();

        if (address == null) {
            return null;
        }

        return new DeviceUid(address);
    }

    @NonNull
    @Override
    public String toString() {
        return this.uid;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this ||
                (obj instanceof DeviceUid) && this.equals((DeviceUid)obj);
    }

    public boolean equals(@NonNull DeviceUid other) {
        return this.uid.equals(other.uid);
    }

    @Override
    public int hashCode() {
        return this.uid.hashCode();
    }
}
