package de.jlab.cardroid.devices;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;

public final class DeviceStore {

    private ArrayList<DeviceHandler> devices = new ArrayList<>();

    public void add(@NonNull DeviceHandler device) {
        this.devices.add(device);
    }

    public boolean isEmpty() {
        return this.devices.size() < 1;
    }

    @Nullable
    public DeviceHandler get(@NonNull DeviceConnectionId connectionId) {
        for (int i = 0; i < this.devices.size(); i++) {
            DeviceHandler device = this.devices.get(i);
            if (device.isPhysicalDevice(connectionId)) {
                return device;
            }
        }
        return null;
    }

    @Nullable
    public DeviceHandler get(@NonNull DeviceUid uid) {
        for (int i = 0; i < this.devices.size(); i++) {
            DeviceHandler device = this.devices.get(i);
            if (device.isDevice(uid)) {
                return device;
            }
        }
        return null;
    }

    @Nullable
    public DeviceHandler remove(@NonNull DeviceConnectionId connectionId) {
        DeviceHandler device = this.get(connectionId);
        if (device != null) {
            this.devices.remove(device);
        }
        return device;
    }

    @Nullable
    public DeviceHandler remove(@NonNull DeviceUid uid) {
        DeviceHandler device = this.get(uid);
        if (device != null) {
            this.devices.remove(device);
        }
        return device;
    }

    public boolean remove(@NonNull DeviceHandler device) {
        return this.devices.remove(device);
    }

}
