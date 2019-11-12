package de.jlab.cardroid.devices;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;

public final class DeviceServiceConnection {

    public enum Action {
        BOUND,
        UNBOUND,
        DISCONNECTED
    }

    private boolean isBound = false;
    private DeviceService.DeviceServiceBinder service;
    private BindingActionConsumer onBindingAction;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DeviceServiceConnection.this.service = (DeviceService.DeviceServiceBinder)service;
            DeviceServiceConnection.this.onBindingAction.consume(DeviceServiceConnection.this.service, Action.BOUND);
            DeviceServiceConnection.this.isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            DeviceServiceConnection.this.onBindingAction.consume(DeviceServiceConnection.this.service, Action.DISCONNECTED);
            DeviceServiceConnection.this.isBound = false;
            DeviceServiceConnection.this.service = null;
        }
    };

    public DeviceServiceConnection(@NonNull BindingActionConsumer onBindingAction) {
        this.onBindingAction = onBindingAction;
    }

    public void unbind(@NonNull Context context) {
        if (this.isBound) {
            this.onBindingAction.consume(this.service, Action.UNBOUND);
            context.unbindService(this.serviceConnection);
            this.service = null;
        }
    }

    public void bind(@NonNull Context context) {
        context.bindService(new Intent(context, DeviceService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public interface BindingActionConsumer {
        void consume(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull Action action);
    }

}
