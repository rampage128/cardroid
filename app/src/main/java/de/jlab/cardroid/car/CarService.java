package de.jlab.cardroid.car;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.jlab.cardroid.car.nissan370z.CarCanController;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.FeatureObserver;
import de.jlab.cardroid.service.FeatureService;

public final class CarService extends FeatureService {

    private CarCanController carCanController;
    private CarServiceBinder binder = new CarServiceBinder();

    private ArrayList<CanInteractable>   canInteractables   = new ArrayList<>();
    private ArrayList<CanObservable>     canObservables     = new ArrayList<>();


    private FeatureObserver<CanInteractable> canInteractableObserver = new FeatureObserver<CanInteractable>() {
        @Override
        public void onFeatureAvailable(@NonNull CanInteractable feature) {
            CarService.this.canInteractables.add(feature);
            CarService.this.checkReady();
        }

        @Override
        public void onFeatureUnavailable(@NonNull CanInteractable feature) {
            CarService.this.canInteractables.remove(feature);
            CarService.this.checkReady();
        }
    };

    private FeatureObserver<CanObservable> canObservableObserver = new FeatureObserver<CanObservable>() {
        @Override
        public void onFeatureAvailable(@NonNull CanObservable feature) {
            CarService.this.canObservables.add(feature);
            CarService.this.checkReady();
        }

        @Override
        public void onFeatureUnavailable(@NonNull CanObservable feature) {
            CarService.this.canObservables.remove(feature);
            CarService.this.checkReady();
        }
    };

    @Override
    protected void onDeviceServiceConnected(DeviceService.DeviceServiceBinder service) {
        service.subscribe(canInteractableObserver, CanInteractable.class);
        service.subscribe(canObservableObserver, CanObservable.class);
    }

    @Override
    protected void onDeviceServiceDisconnected() {
        this.stopSelf();
    }

    @Override
    protected ArrayList<Class<? extends Feature>> tieLifecycleToFeatures() {
        return new ArrayList<Class<? extends Feature>>() {{
            add(CanInteractable.class);
            add(CanObservable.class);
        }};
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void checkReady() {
        if (this.canObservables.size() > 0) {
            createCarController();
        } else {
            destroyCarController();
        }
    }

    private void createCarController() {
        CanObservable observable = this.getCanObservable();
        this.carCanController = new CarCanController(this.getCanObservable());
        for (int i=0; i<this.canInteractables.size(); i++) {
            if (observable.getDevice().equals(this.canInteractables.get(i).getDevice())) {
                this.carCanController.setInteractable(this.canInteractables.get(i));
            }
        }
    }

    private void destroyCarController() {
        if (this.carCanController != null) {
            this.carCanController.dispose();
            this.carCanController = null;
        }
    }

    public CanObservable getCanObservable() {
        return canObservables.size() > 0 ? canObservables.get(0): null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public class CarServiceBinder extends Binder {
        public CarCanController getCarController() {
            return CarService.this.carCanController;
        }
    }

}
