package de.jlab.cardroid.devices;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;

public final class DeviceController {

    private ArrayList<DeviceHandler> devices = new ArrayList<>();
    private ArrayList<FeatureObserver> observers = new ArrayList<>();
    private DeviceHandler.Observer deviceObserver = new DeviceHandler.Observer() {
        @Override
        public void onStateChange(@NonNull DeviceHandler device, @NonNull DeviceHandler.State state, @NonNull DeviceHandler.State previous) {
            if (state == DeviceHandler.State.INVALID) {
                DeviceController.this.remove(device);
            }
        }

        @Override
        public void onFeatureAvailable(@NonNull Feature feature) {
            for (int i = 0; i < observers.size(); i++) {
                observers.get(i).onFeatureAvailable(feature);
            }
        }

        @Override
        public void onFeatureUnavailable(@NonNull Feature feature) {
            for (int i = 0; i < observers.size(); i++) {
                observers.get(i).onFeatureUnavailable(feature);
            }
        }
    };

    public void add(@NonNull DeviceHandler device) {
        this.devices.add(device);
        device.addObserver(this.deviceObserver);
        device.open();
    }

    public boolean isEmpty() {
        return this.devices.size() < 1;
    }

    public void subscribe(@NonNull FeatureObserver observer) {
        this.observers.add(observer);
        for (int i = 0; i < this.devices.size(); i++) {
             for (Feature f: this.devices.get(i).getFeatures()) {
                 observer.onFeatureAvailable(f);
             }
        }
    }

    public void unsubscribe(@NonNull FeatureObserver observer) {
        this.observers.remove(observer);
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
            device.removeObserver(this.deviceObserver);
        }
        return device;
    }

    @Nullable
    public DeviceHandler remove(@NonNull DeviceUid uid) {
        DeviceHandler device = this.get(uid);
        if (device != null) {
            this.devices.remove(device);
            device.removeObserver(this.deviceObserver);
        }
        return device;
    }

    public boolean remove(@NonNull DeviceHandler device) {
        device.removeObserver(this.deviceObserver);
        return this.devices.remove(device);
    }

    public interface FeatureObserver {
        void onFeatureAvailable(@NonNull Feature feature);
        void onFeatureUnavailable(@NonNull Feature feature);
    }

}
