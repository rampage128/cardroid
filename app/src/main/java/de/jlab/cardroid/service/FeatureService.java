package de.jlab.cardroid.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleService;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.FeatureObserver;

abstract public class FeatureService extends LifecycleService {

    private DeviceService.DeviceServiceBinder service;
    private ArrayList<Feature> features = new ArrayList<>();
    private Timer timer = new Timer();
    private TimerTask timeout;

    private ServiceConnection featureConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FeatureService.this.service = (DeviceService.DeviceServiceBinder)service;
            FeatureService.this.tieLifecycle();
            FeatureService.this.onDeviceServiceConnected((DeviceService.DeviceServiceBinder)service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            FeatureService.this.service = null;
            FeatureService.this.onDeviceServiceDisconnected();
            FeatureService.this.stopSelf();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.getApplicationContext().bindService(new Intent(this.getApplicationContext(), DeviceService.class), this.featureConnection, Context.BIND_AUTO_CREATE);
        Log.e(this.getClass().getSimpleName(), "FEATURE SERVICE CREATED");
    }

    @Override
    public void onDestroy() {
        this.getApplicationContext().unbindService(this.featureConnection);
        Log.e(this.getClass().getSimpleName(), "FEATURE SERVICE DESTROYED");
        super.onDestroy();
    }

    private void checkAlive() {
        if (this.features.size() == 0) {
            this.stopSelf();
        }
    }

    private void cancelTimeout() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
            this.timeout = null;
        }
    }

    private void tieLifecycle() {
        // 5 seconds initial timeout for the features to arrive
        this.timeout = new TimerTask() {
            @Override
            public void run() {
                FeatureService.this.stopSelf();
            }
        };
        this.timer.purge();
        this.timer.schedule(this.timeout, 5000);

        ArrayList<Class<? extends Feature>> featureClasses = this.tieLifecycleToFeatures();
        for (int i=0; i<featureClasses.size(); i++) {
            this.service.subscribe(new FeatureObserver() {
                @Override
                public void onFeatureAvailable(@NonNull Feature feature) {
                    FeatureService.this.features.add(feature);
                    FeatureService.this.cancelTimeout();
                }

                @Override
                public void onFeatureUnavailable(@NonNull Feature feature) {
                    FeatureService.this.features.remove(feature);
                    FeatureService.this.checkAlive();
                }
            }, featureClasses.get(i));
        }

    }

    abstract protected void onDeviceServiceConnected(DeviceService.DeviceServiceBinder service);
    abstract protected void onDeviceServiceDisconnected();
    abstract protected ArrayList<Class<? extends Feature>> tieLifecycleToFeatures();
}
