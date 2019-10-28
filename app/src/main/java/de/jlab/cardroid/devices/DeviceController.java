package de.jlab.cardroid.devices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;

public final class DeviceController {

    private ArrayList<Device> devices = new ArrayList<>();
    private HashMap<Class<? extends Feature>, ArrayList<FeatureObserver>> subscribers = new HashMap<>();
    private Device.Observer deviceObserver = new Device.Observer() {
        @Override
        public void onStateChange(@NonNull Device device, @NonNull Device.State state, @NonNull Device.State previous) {
            if (state == Device.State.INVALID) {
                DeviceController.this.remove(device);
            }
        }

        @Override
        public void onFeatureAvailable(@NonNull Feature feature) {
            synchronized (subscribers) {
                for (Iterator<Class<? extends Feature>> it = subscribers.keySet().iterator(); it.hasNext(); ) {
                    Class<? extends Feature> key = it.next();
                    ArrayList<FeatureObserver> observers = subscribers.get(key);
                    for (int i = 0; i < observers.size(); i++) {
                        FeatureObserver observer = observers.get(i);
                        if (key.isInstance(feature) && observer != null) {
                            observer.onFeatureAvailable(key.cast(feature));
                        }
                    }

                }
            }
        }

        @Override
        public void onFeatureUnavailable(@NonNull Feature feature) {
            synchronized (subscribers) {
                for (Iterator<Class<? extends Feature>> it = subscribers.keySet().iterator(); it.hasNext(); ) {
                    Class<? extends Feature> key = it.next();
                    ArrayList<FeatureObserver> observers = subscribers.get(key);
                    for (int i = 0; i < observers.size(); i++) {
                        FeatureObserver observer = observers.get(i);
                        if (key.isInstance(feature) && observer != null) {
                            observer.onFeatureUnavailable(key.cast(feature));
                        }
                    }
                }
            }
        }
    };

    public void add(@NonNull Device device) {
        synchronized (this.devices) {
            this.devices.add(device);
            device.addObserver(this.deviceObserver);
            device.open();
        }
    }

    public boolean isEmpty() {
        return this.devices.size() < 1;
    }


    public <FT extends Feature> void addSubscriber(FeatureObserver<FT> subscriber, Class<FT> featureClass) {
        synchronized (subscribers) {
            if (this.subscribers.get(featureClass) == null) {
                this.subscribers.put(featureClass, new ArrayList<>());
            }
            ArrayList<FeatureObserver> observers = this.subscribers.get(featureClass);
            observers.add(subscriber);
            synchronized (this.devices) {
                for (int i = 0; i < this.devices.size(); i++) {
                    for (Feature f : this.devices.get(i).getFeatures()) {
                        if (featureClass.isInstance(f)) {
                            subscriber.onFeatureAvailable((FT) f);
                        }
                    }
                }
            }
        }
    }

    public <FT extends Feature> void removeSubscriber(FeatureObserver<FT> subscriber) {
        synchronized (subscribers) {
            for (Iterator<Class<? extends Feature>> it = this.subscribers.keySet().iterator(); it.hasNext(); ) {
                Class<? extends Feature> key = it.next();
                ArrayList<FeatureObserver> observers = subscribers.get(key);
                if (observers.contains(subscriber)) {
                    observers.remove(subscriber);
                }
            }
        }
    }

    @Nullable
    public Device get(@NonNull DeviceConnectionId connectionId) {
        for (int i = 0; i < this.devices.size(); i++) {
            Device device = this.devices.get(i);
            if (device.isPhysicalDevice(connectionId)) {
                return device;
            }
        }
        return null;
    }

    @Nullable
    public Device get(@NonNull DeviceUid uid) {
        for (int i = 0; i < this.devices.size(); i++) {
            Device device = this.devices.get(i);
            if (device.isDevice(uid)) {
                return device;
            }
        }
        return null;
    }

    @Nullable
    public Device remove(@NonNull DeviceConnectionId connectionId) {
        Device device = this.get(connectionId);
        if (device != null) {
            device.close();
            this.devices.remove(device);
            device.removeObserver(this.deviceObserver);
        }
        return device;
    }

    @Nullable
    public Device remove(@NonNull DeviceUid uid) {
        Device device = this.get(uid);
        if (device != null) {
            device.close();
            this.devices.remove(device);
            device.removeObserver(this.deviceObserver);
        }
        return device;
    }

    public boolean remove(@NonNull Device device) {
        device.removeObserver(this.deviceObserver);
        return this.devices.remove(device);
    }

}
