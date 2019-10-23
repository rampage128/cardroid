package de.jlab.cardroid.car;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.jlab.cardroid.car.nissan370z.CarCanController;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.FeatureObserver;
import de.jlab.cardroid.devices.serial.carduino.EventInteractable;
import de.jlab.cardroid.overlay.OverlayWindow;
import de.jlab.cardroid.service.FeatureService;

public final class CanService extends FeatureService {

    private Handler uiHandler;
    private OverlayWindow overlay;
    private CarCanController carCanController;
    private CanServiceBinder binder = new CanServiceBinder();

    private ArrayList<EventInteractable> eventInteractables = new ArrayList<>();
    private ArrayList<CanInteractable>   canInteractables   = new ArrayList<>();
    private ArrayList<CanObservable>     canObservables     = new ArrayList<>();

    private FeatureObserver<EventInteractable> eventObserver = new FeatureObserver<EventInteractable>() {
        @Override
        public void onFeatureAvailable(@NonNull EventInteractable feature) {
            CanService.this.eventInteractables.add(feature);
            CanService.this.checkReady();
        }

        @Override
        public void onFeatureUnavailable(@NonNull EventInteractable feature) {
            CanService.this.eventInteractables.remove(feature);
            CanService.this.checkReady();
        }
    };

    private FeatureObserver<CanInteractable> canInteractableObserver = new FeatureObserver<CanInteractable>() {
        @Override
        public void onFeatureAvailable(@NonNull CanInteractable feature) {
            CanService.this.canInteractables.add(feature);
            CanService.this.checkReady();
        }

        @Override
        public void onFeatureUnavailable(@NonNull CanInteractable feature) {
            CanService.this.canInteractables.remove(feature);
            CanService.this.checkReady();
        }
    };

    private FeatureObserver<CanObservable> canObservableObserver = new FeatureObserver<CanObservable>() {
        @Override
        public void onFeatureAvailable(@NonNull CanObservable feature) {
            CanService.this.canObservables.add(feature);
            CanService.this.checkReady();
        }

        @Override
        public void onFeatureUnavailable(@NonNull CanObservable feature) {
            CanService.this.canObservables.remove(feature);
            CanService.this.checkReady();
        }
    };

    @Override
    protected void onDeviceServiceConnected(DeviceService.DeviceServiceBinder service) {
        service.subscribe(eventObserver, EventInteractable.class);
        service.subscribe(canInteractableObserver, CanInteractable.class);
        service.subscribe(canObservableObserver, CanObservable.class);
    }

    @Override
    protected void onDeviceServiceDisconnected() {
        this.stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.uiHandler = new Handler();
        this.overlay = new OverlayWindow(this);
    }

    private void checkReady() {
        if (this.canObservables.size()    > 0 &&
            this.canInteractables.size()  > 0) {
            createCarController();
            showOverlay();
        } else {
            hideOverlay();
            destroyCarController();
        }
    }

    public CarCanController getCarCanController() {
        return carCanController;
    }

    private void createCarController() {
        this.carCanController = new CarCanController(this.getCanInteractable(), this.getCanObservable());
    }

    private void destroyCarController() {
        this.carCanController.dispose();
        this.carCanController = null;
    }

    public void showOverlay() {
        this.runOnUiThread(() -> this.overlay.create());
    }

    public void hideOverlay() {
        this.runOnUiThread(() -> this.overlay.destroy());
    }

    private void runOnUiThread(Runnable runnable) {
        uiHandler.post(runnable);
    }

    public EventInteractable getEventInteractable() {
        //TODO: filter by preferences which one to serve
        return eventInteractables.size() > 0 ? eventInteractables.get(0): null;
    }

    public CanInteractable getCanInteractable() {
        return canInteractables.size() > 0 ? canInteractables.get(0): null;
    }

    public CanObservable getCanObservable() {
        return canObservables.size() > 0 ? canObservables.get(0): null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public class CanServiceBinder extends Binder {
        public CarCanController getCarController() {
            return CanService.this.carCanController;
        }
        public void showOverlay() {
            CanService.this.showOverlay();
        }

        public void hideOverlay() {
            CanService.this.hideOverlay();
        }
    }

}
