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
import de.jlab.cardroid.utils.MultiMap;

public final class DeviceController {

    private NewDeviceWaitingList newDeviceWaitingList = new NewDeviceWaitingList();
    private ArrayList<Device> devices = new ArrayList<>();
    private HashMap<DeviceUid, DeviceSubscriber> deviceSubscribers = new HashMap<>();
    private Device.StateObserver stateObserver = this::onDeviceStateChange;

    public boolean isEmpty() {
        return this.devices.size() < 1;
    }

    public <FT extends Feature> void subscribeFeature(@NonNull Device.FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass, @NonNull DeviceUid... deviceUids) {
        if (deviceUids.length > 0) {
            for (DeviceUid uid : deviceUids) {
                this.subscribeFeatureInternal(observer, featureClass, uid);
            }
        } else {
            this.subscribeFeatureInternal(observer, featureClass, null);
        }
    }

    private <FT extends Feature> void subscribeFeatureInternal(@NonNull Device.FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass, @Nullable DeviceUid uid) {
        DeviceSubscriber subscriber = this.getOrCreateSubscriber(uid);
        subscriber.addFeatureSubscription(observer, featureClass);
        for (int i = 0; i < this.devices.size(); i++) {
            Device device = this.devices.get(i);
            if (uid == null || device.isDevice(uid)) {
                device.addFeatureObserver(observer, featureClass);
            }
        }
    }

    public <FT extends Feature> void unsubscribeFeature(@NonNull Device.FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass) {
        for (DeviceSubscriber subscriber : this.deviceSubscribers.values()) {
            subscriber.removeFeatureSubscription(observer, featureClass);
            for (int i = 0; i < this.devices.size(); i++) {
                this.devices.get(i).removeFeatureObserver(observer, featureClass);
            }
        }
    }

    public void subscribeState(@NonNull Device.StateObserver observer, @NonNull DeviceUid... deviceUids) {
        if (deviceUids.length > 0) {
            for (DeviceUid uid : deviceUids) {
                this.subscribeStateInternal(observer, uid);
            }
        } else {
            this.subscribeStateInternal(observer, null);
        }
    }

    private void subscribeStateInternal(@NonNull Device.StateObserver observer, @Nullable DeviceUid uid) {
        DeviceSubscriber subscriber = this.getOrCreateSubscriber(uid);
        subscriber.addStateSubscription(observer);
        for (int i = 0; i < this.devices.size(); i++) {
            Device device = this.devices.get(i);
            if (uid == null || device.isDevice(uid)) {
                device.addStateObserver(observer);
            }
        }
    }

    public void unsubscribeState(@NonNull Device.StateObserver observer) {
        for (DeviceSubscriber subscriber : this.deviceSubscribers.values()) {
            subscriber.removeStateSubscription(observer);
            for (int i = 0; i < this.devices.size(); i++) {
                this.devices.get(i).removeStateObserver(observer);
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
            device.addStateObserver(this.stateObserver);
            device.open();
        }
    }

    private void remove(@NonNull Device device) {
        this.devices.remove(device);

        DeviceConnectionRequest matchingRequest = this.newDeviceWaitingList.pick(device);
        if (matchingRequest != null) {
            Log.e(this.getClass().getSimpleName(), "Opening device " + matchingRequest + " from queue");
            this.open(matchingRequest.getDevice());
        }
    }

    private void onDeviceStateChange(@NonNull Device device, @NonNull Device.State state, @NonNull Device.State previous) {
        if (state == Device.State.READY) {
            DeviceSubscriber[] subscribers = new DeviceSubscriber[] { this.deviceSubscribers.get(device.getDeviceUid()), this.deviceSubscribers.get(null) };
            for (DeviceSubscriber subscriber : subscribers) {
                if (subscriber != null) {
                    subscriber.subscribe(device);
                }
            }
        } else if (state == Device.State.INVALID) {
            DeviceController.this.remove(device);
        }
    }

    private DeviceSubscriber getOrCreateSubscriber(@Nullable DeviceUid deviceUid) {
        DeviceSubscriber subscriber = this.deviceSubscribers.get(deviceUid);
        if (subscriber == null) {
            subscriber = new DeviceSubscriber(deviceUid);
            this.deviceSubscribers.put(deviceUid, subscriber);
        }
        return subscriber;
    }

    private static class DeviceSubscriber {
        private MultiMap<Class<? extends Feature>, Device.FeatureChangeObserver> featureSubscribers = new MultiMap<>();
        private ArrayList<Device.StateObserver> stateSubscribers = new ArrayList<>();
        private DeviceUid deviceUid;

        public DeviceSubscriber(@Nullable DeviceUid deviceUid) {
            this.deviceUid = deviceUid;
        }

        public <FT extends Feature> void addFeatureSubscription(@NonNull Device.FeatureChangeObserver<FT> subscriber, @NonNull Class<FT> featureClass) {
            this.featureSubscribers.put(featureClass, subscriber);
        }

        public <FT extends Feature> void removeFeatureSubscription(@NonNull Device.FeatureChangeObserver<FT> subscriber, @NonNull Class<FT> featureClass) {
            this.featureSubscribers.remove(featureClass, subscriber);
        }

        public void addStateSubscription(@NonNull Device.StateObserver onStateChange) {
            this.stateSubscribers.add(onStateChange);
        }

        public void removeStateSubscription(@NonNull Device.StateObserver onStateChange) {
            this.stateSubscribers.remove(onStateChange);
        }

        public void subscribe(@NonNull Device device) {
            if (this.deviceUid == null || device.isDevice(this.deviceUid)) {
                for (Class<? extends Feature> featureClass : this.featureSubscribers.keySet()) {
                    ArrayList<Device.FeatureChangeObserver> observers = this.featureSubscribers.get(featureClass);
                    for (Device.FeatureChangeObserver observer : observers) {
                        device.addFeatureObserver(observer, featureClass);
                    }
                }

                for (int i = 0; i < this.stateSubscribers.size(); i++) {
                    device.addStateObserver(stateSubscribers.get(i));
                }
            }
        }

        public void unsubscribe(@NonNull Device device) {
            if (this.deviceUid == null || device.isDevice(this.deviceUid)) {
                for (Class<? extends Feature> featureClass : this.featureSubscribers.keySet()) {
                    ArrayList<Device.FeatureChangeObserver> observers = this.featureSubscribers.get(featureClass);
                    for (Device.FeatureChangeObserver observer : observers) {
                        device.removeFeatureObserver(observer, featureClass);
                    }
                }

                for (int i = 0; i < this.stateSubscribers.size(); i++) {
                    device.removeStateObserver(stateSubscribers.get(i));
                }
            }
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
