package de.jlab.cardroid.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import de.jlab.cardroid.devices.DeviceService;

abstract public class FeatureService extends Service {

    private DeviceService.DeviceServiceBinder service;

    private ServiceConnection featureConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FeatureService.this.service = (DeviceService.DeviceServiceBinder)service;
            FeatureService.this.onDeviceServiceConnected((DeviceService.DeviceServiceBinder)service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            FeatureService.this.service = null;
            FeatureService.this.onDeviceServiceDisconnected();
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
    }

    @Override
    public void onDestroy() {
        this.getApplicationContext().unbindService(this.featureConnection);
        super.onDestroy();
    }

    abstract protected void onDeviceServiceConnected(DeviceService.DeviceServiceBinder service);
    abstract protected void onDeviceServiceDisconnected();
}
