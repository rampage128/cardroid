package de.jlab.cardroid.overlay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import de.jlab.cardroid.car.CanInteractable;
import de.jlab.cardroid.car.CarService;
import de.jlab.cardroid.car.nissan370z.CarCanController;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.FeatureObserver;
import de.jlab.cardroid.errors.ErrorObservable;
import de.jlab.cardroid.service.FeatureService;

public class OverlayService extends FeatureService implements FeatureObserver<CanInteractable> {

    private OverlayServiceBinder binder = new OverlayServiceBinder();
    private Handler uiHandler;
    private OverlayWindow overlay;
    private ArrayList<CanInteractable> canInteractables = new ArrayList<>();

    private CarService.CarServiceBinder carService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            OverlayService.this.carService = (CarService.CarServiceBinder)service;
            OverlayService.this.showOverlay();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            OverlayService.this.carService = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        this.uiHandler = new Handler();
        this.overlay = new OverlayWindow(this);
        this.getApplicationContext().bindService(new Intent(this.getApplicationContext(), CarService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        this.overlay.destroy();
        this.getApplicationContext().unbindService(this.serviceConnection);
        super.onDestroy();
    }

    @Override
    protected void onDeviceServiceConnected(DeviceService.DeviceServiceBinder service) {
        service.subscribe(this, CanInteractable.class);
    }

    @Override
    protected void onDeviceServiceDisconnected() {
        this.stopSelf();
    }

    public void showOverlay() {
        if (this.carService != null && this.getInteractable() != null) {
            this.runOnUiThread(() -> this.overlay.create());
        }
    }

    public void hideOverlay() {
        this.runOnUiThread(() -> this.overlay.destroy());
    }

    public CanInteractable getInteractable() {
        return this.canInteractables.size() > 0 ? this.canInteractables.get(0) : null;
    }

    public CarCanController getCarCanController() {
        return this.carService != null ? this.carService.getCarController() : null;
    }

    private void runOnUiThread(Runnable runnable) {
        uiHandler.post(runnable);
    }

    @Override
    public void onFeatureAvailable(@NonNull CanInteractable feature) {
        this.canInteractables.add(feature);
        this.showOverlay();
    }

    @Override
    public void onFeatureUnavailable(@NonNull CanInteractable feature) {
        this.canInteractables.remove(feature);
        if (this.canInteractables.size() == 0) {
            this.hideOverlay();
        }
    }

    @Override
    protected ArrayList<Class<? extends Feature>> tieLifecycleToFeatures() {
        return new ArrayList<Class<? extends Feature>>() {{
            add(CanInteractable.class);
        }};
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public class OverlayServiceBinder extends Binder {

        public void showOverlay() {
            OverlayService.this.showOverlay();
        }

        public void hideOverlay() {
            OverlayService.this.hideOverlay();
        }
    }
}
