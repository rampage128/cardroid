package de.jlab.cardroid.devices;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.SparseArray;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.storage.DeviceEntity;

public final class DeviceConnectionStore {

    private SparseArray<DeviceConnection> entries = new SparseArray<>();
    private ArrayList<DeviceConnectionObserver> observers = new ArrayList<>();

    public void addObserver(@NonNull DeviceConnectionObserver observer) {
        this.observers.add(observer);
        for (int i = 0; i < this.entries.size(); i++) {
            observer.onConnectionUpdate(this.entries.valueAt(i));
        }
    }

    public void removeObserver(@NonNull DeviceConnectionObserver observer) {
        this.observers.remove(observer);
    }

    private void notifyObservers(@NonNull DeviceConnection connection) {
        for (int i = 0; i < this.observers.size(); i++) {
            this.observers.get(i).onConnectionUpdate(connection);
        }
    }

    public void connect(@NonNull DeviceHandler device, @NonNull Context context) {
        DeviceConnection connection = new DeviceConnection(device, context);
        this.entries.put(device.getDeviceId(), connection);
        notifyObservers(connection);
    }

    public boolean disconnect(@NonNull DeviceEntity descriptor) {
        DeviceConnection connection = this.get(descriptor);
        if (connection == null) {
            return false;
        }
        connection.getDevice().disconnectDevice();
        notifyObservers(connection);
        return true;
    }

    public void hydrate(@NonNull DeviceHandler device, @NonNull DeviceEntity descriptor) {
        DeviceConnection connection = this.entries.get(device.getDeviceId());
        if (connection != null) {
            connection.updateDescriptor(descriptor);
            notifyObservers(connection);
        }
    }

    public boolean isEmpty() {
        return this.entries.size() < 1;
    }

    @Nullable
    public DeviceConnection get(@NonNull DeviceHandler device) {
        return this.entries.get(device.getDeviceId());
    }

    @Nullable
    public DeviceConnection get(@NonNull DeviceEntity descriptor) {
        for (int i = 0; i < this.entries.size(); i++) {
            DeviceConnection connection = this.entries.get(this.entries.keyAt(i));
            if (connection.isDevice(descriptor)) {
                return connection;
            }
        }

        return null;
    }

    // FIXME: This will clash with BT devices. We should replace int getDeviceId() with String getConnectionId()
    // String getConnectionId() could return the deviceName for usb devices and the mac-address for bt devices.
    @Nullable
    public DeviceConnection remove(@NonNull UsbDevice device) {
        int deviceId = device.getDeviceId();
        DeviceConnection connection = this.entries.get(deviceId);
        this.entries.remove(deviceId);
        if (connection != null) {
            notifyObservers(connection);
        }
        return connection;
    }

    @Nullable
    public DeviceConnection remove(@NonNull DeviceHandler device) {
        int deviceId = device.getDeviceId();
        DeviceConnection connection = this.entries.get(deviceId);
        this.entries.remove(deviceId);
        if (connection != null) {
            notifyObservers(connection);
        }
        return connection;
    }

    public interface DeviceConnectionObserver {
        void onConnectionUpdate(DeviceConnection connection);
    }

}
