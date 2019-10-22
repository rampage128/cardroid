package de.jlab.cardroid.devices;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Iterator;

import androidx.annotation.NonNull;

public final class FeatureConnection implements ServiceConnection {

    private DeviceService.DeviceServiceBinder service;

    private HashMap<Class<? extends ObservableFeature>, Subscriber> subscribers = new HashMap<>();
    private DeviceHandler.Observer deviceStoreObserver = new DeviceHandler.Observer() {
        @Override
        public void onStateChange(@NonNull DeviceHandler device, @NonNull DeviceHandler.State state, @NonNull DeviceHandler.State previous) {

        }

        @Override
        public void onFeatureAvailable(@NonNull Feature feature) {
            for (Iterator<Class<? extends ObservableFeature>> it = subscribers.keySet().iterator(); it.hasNext(); ) {
                Class<? extends ObservableFeature> key = it.next();
                Subscriber subscriber = subscribers.get(key);
                if (key.isInstance(feature) && subscriber != null) {
                    subscriber.onFeatureAvailable(key.cast(feature));
                }
            }
        }

        @Override
        public void onFeatureUnavailable(@NonNull Feature feature) {
            for (Iterator<Class<? extends ObservableFeature>> it = subscribers.keySet().iterator(); it.hasNext(); ) {
                Class<? extends ObservableFeature> key = it.next();
                Subscriber subscriber = subscribers.get(key);
                if (key.isInstance(feature) && subscriber != null) {
                    subscriber.onFeatureUnavailable(key.cast(feature));
                }
            }
        }
    };

    public <FT extends ObservableFeature> void addSubscriber(Subscriber<FT> subscriber, Class<FT> featureClass) {
        this.subscribers.put(featureClass, subscriber);
    }

    public void removeSubscriber(Subscriber subscriber) {
        for (Iterator<Class<? extends ObservableFeature>> it = this.subscribers.keySet().iterator(); it.hasNext(); ) {
            Class<? extends ObservableFeature> key = it.next();
            if (this.subscribers.get(key) == subscriber) {
                it.remove();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.service = (DeviceService.DeviceServiceBinder)service;
        this.service.subscribeDeviceStore(this.deviceStoreObserver);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        this.service.unsubscribeDeviceStore(this.deviceStoreObserver);
        this.service = null;
    }

    public interface Subscriber<ObservableType extends ObservableFeature> {
        void onFeatureAvailable(ObservableType feature);
        void onFeatureUnavailable(ObservableType feature);
    }

}
