package de.jlab.cardroid.devices;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.storage.DeviceEntity;

public final class DeviceConnectionStore {

    private HashMap<DeviceConnectionId, DeviceConnection> entries = new HashMap<>();
    private ArrayList<DeviceConnectionObserver> observers = new ArrayList<>();

    public void addObserver(@NonNull DeviceConnectionObserver observer) {
        this.observers.add(observer);
        for (DeviceConnection deviceConnection : this.entries.values()) {
            observer.onConnectionUpdate(deviceConnection);
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
        this.entries.put(device.getConnectionId(), connection);
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
        DeviceConnection connection = this.entries.get(device.getConnectionId());
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
        return this.entries.get(device.getConnectionId());
    }

    @Nullable
    public DeviceConnection get(@NonNull DeviceEntity descriptor) {
        for (DeviceConnection connection : this.entries.values()) {
            if (connection.isDevice(descriptor)) {
                return connection;
            }
        }

        return null;
    }

    @Nullable
    public DeviceConnection remove(@NonNull DeviceConnectionId connectionId) {
        DeviceConnection connection = this.entries.remove(connectionId);
        if (connection != null) {
            notifyObservers(connection);
        }
        return connection;
    }

    @Nullable
    public DeviceConnection remove(@NonNull DeviceHandler device) {
        DeviceConnection connection = this.entries.remove(device.getConnectionId());
        if (connection != null) {
            notifyObservers(connection);
        }
        return connection;
    }

    public interface DeviceConnectionObserver {
        void onConnectionUpdate(DeviceConnection connection);
    }

}
