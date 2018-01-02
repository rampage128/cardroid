package de.jlab.cardroid.usb;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Iterator;

import de.jlab.cardroid.WatchDogService;

public abstract class UsbWatchDog implements WatchDogService.WatchDog {
    private Context context;

    private UsbService.UsbServiceBinder serviceBinder = null;

    private UsbServiceConnection serviceConnection = new UsbServiceConnection();
    private class UsbServiceConnection implements ServiceConnection {
        private boolean connected = false;
        private final Object lock = new Object();

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            this.connected = true;
            UsbWatchDog.this.serviceBinder = (UsbService.UsbServiceBinder)binder;

            synchronized (this.lock) {
                this.lock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.connected = false;
            UsbWatchDog.this.serviceBinder = null;
        }

        public void waitUntilConnected() throws InterruptedException {
            if (!this.connected) {
                synchronized (this.lock) {
                    this.lock.wait();
                }
            }
        }
    }

    public UsbWatchDog(Context context) {
        this.context = context;
    }

    protected abstract boolean shouldWatchDevice(UsbDevice device);
    protected abstract Class<? extends UsbService> getServiceClass();

    @Override
    final public Intent watch() {
        Intent intent = new Intent(ACTION_IGNORE);

        Class<? extends UsbService> serviceClass = this.getServiceClass();

        UsbManager usbManager = (UsbManager)this.context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();
        for (Iterator<UsbDevice> iterator = deviceMap.values().iterator(); iterator.hasNext();) {
            UsbDevice device = iterator.next();
            if (this.shouldWatchDevice(device)) {
                this.context.bindService(new Intent(this.context, serviceClass), this.serviceConnection, Context.BIND_AUTO_CREATE);
                try {
                    this.serviceConnection.waitUntilConnected();
                } catch (InterruptedException e) { /* Intentionally left blank */ }

                if (this.serviceBinder != null && !this.serviceBinder.isConnected()) {
                    intent.setAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
                    intent.putExtra(UsbManager.EXTRA_DEVICE, device);
                    intent.setClass(this.context.getApplicationContext(), serviceClass);
                    return intent;
                }
                this.context.unbindService(this.serviceConnection);
            }
        }

        return intent;
    }

    @Override
    final public void trigger(Intent intent) {
        this.serviceBinder.disconnect();
        this.context.getApplicationContext().startService(intent);
        this.context.unbindService(this.serviceConnection);
    }

    private boolean isServiceRunning(Class<? extends UsbService> serviceClass) {
        ActivityManager manager = (ActivityManager) this.context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
