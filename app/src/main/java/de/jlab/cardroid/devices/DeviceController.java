package de.jlab.cardroid.devices;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;

public final class DeviceController {

    private NewDeviceWaitingList newDeviceWaitingList = new NewDeviceWaitingList();

    private ArrayList<Device> devices = new ArrayList<>();
    private HashMap<Class<? extends Feature>, ArrayList<FeatureObserver>> subscribers = new HashMap<>();
    private ArrayList<Device.StateObserver> stateSubscribers = new ArrayList<>();

    private Device.StateObserver stateObserver = this::onDeviceStateChange;

    private FeatureObserver<Feature> featureObserver = new FeatureObserver<Feature>() {

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

    public void addStateSubscriber(@NonNull Device.StateObserver subscriber) {
        this.stateSubscribers.add(subscriber);
        for (int i = 0; i < this.devices.size(); i++) {
            Device device = this.devices.get(i);
            subscriber.onStateChange(device, device.getState(), device.getState());
        }
    }

    public void removeStateSubscriber(@NonNull Device.StateObserver subscriber) {
        this.stateSubscribers.remove(subscriber);
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

    public void add(@NonNull DeviceConnectionRequest connectionRequest) {
        if (connectionRequest.hasIdentity()) {
            for (Device activeDevice : this.devices) {
                if (connectionRequest.shouldYieldFor(activeDevice)) {
                    Log.e(this.getClass().getSimpleName(), "Device " + activeDevice + " already in list. Queueing up request from " + connectionRequest);
                    this.newDeviceWaitingList.add(connectionRequest);
                    return;
                }
            }
        }

        Log.e(this.getClass().getSimpleName(), "Opening Device " + connectionRequest);
        this.open(connectionRequest.getDevice());
    }

    public void close(@NonNull DeviceConnectionId connectionId) {
        Device device = this.get(connectionId);
        if (device != null) {
            device.close();
            this.remove(device);
        }
    }

    public void close(@NonNull DeviceUid uid) {
        Device device = this.get(uid);
        if (device != null) {
            device.close();
            this.remove(device);
        }
    }

    private void open(@NonNull Device device) {
        synchronized (this.devices) {
            this.devices.add(device);
            device.addObserver(this.stateObserver);
            device.addFeatureObserver(this.featureObserver);
            device.open();
        }
    }

    private void remove(@NonNull Device device) {
        this.devices.remove(device);
        device.removeObserver(this.stateObserver);
        device.removeFeatureObserver(this.featureObserver);

        DeviceConnectionRequest matchingRequest = this.newDeviceWaitingList.pick(device);
        if (matchingRequest != null) {
            Log.e(this.getClass().getSimpleName(), "Opening device " + matchingRequest + " from queue");
            this.open(matchingRequest.getDevice());
        }
    }

    private void onDeviceStateChange(@NonNull Device device, @NonNull Device.State state, @NonNull Device.State previous) {
        for (int i = 0; i < this.stateSubscribers.size(); i++) {
            this.stateSubscribers.get(i).onStateChange(device, state, previous);
        }

        if (state == Device.State.INVALID) {
            DeviceController.this.remove(device);
        }
    }

    private static class NewDeviceWaitingList {

        private static final long REQUEST_TIMEOUT = 5000;

        private ArrayList<DeviceConnectionRequest> requests = new ArrayList<>();
        private Timer timer = null;

        public void add(@NonNull DeviceConnectionRequest connectionRequest) {
            this.requests.add(connectionRequest);
            this.startSchedule();
        }

        @Nullable
        public DeviceConnectionRequest pick(@NonNull Device device) {
            for (Iterator<DeviceConnectionRequest> it = this.requests.iterator(); it.hasNext(); ) {
                DeviceConnectionRequest request = it.next();
                if (request.shouldYieldFor(device)) {
                    this.stopScheduleIfEmpty();
                    it.remove();
                    return request;
                }
            }

            return null;
        }

        private void stopScheduleIfEmpty() {
            if (this.requests.size() < 1) {
                this.stopSchedule();
            }
        }

        private void stopSchedule() {
            if (this.timer != null) {
                this.timer.cancel();
                this.timer = null;
            }
        }

        private void startSchedule() {
            if (this.timer == null) {
                this.timer = new Timer();
                this.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        for (Iterator<DeviceConnectionRequest> it = NewDeviceWaitingList.this.requests.iterator(); it.hasNext(); ) {
                            DeviceConnectionRequest request = it.next();
                            if (request.hasTimedOut(REQUEST_TIMEOUT)) {
                                it.remove();
                            }
                        }
                        NewDeviceWaitingList.this.stopScheduleIfEmpty();
                    }
                }, REQUEST_TIMEOUT, REQUEST_TIMEOUT);
            }
        }

    }

}
