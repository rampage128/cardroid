package de.jlab.cardroid.devices;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/* TODO add bluetooth support
 * - create a device wrapper to unify usb and bluetooth devices
 * - create a devicehandler wrapper to unify usb and bluetooth handling
 * - create a serial connection wrapper to unify usb and bluetooth serial connection
 */
public abstract class DeviceHandler {

    private ArrayList<Interactable> interactables = new ArrayList<>();
    private ArrayList<DeviceDataObservable> observables = new ArrayList<>();
    private DeviceObserver observer;

    @Nullable
    public <InteractableType extends Interactable> InteractableType getInteractable(Class<InteractableType> interactableType) {
        for (int i = 0; i < this.interactables.size(); i++) {
            Interactable interactable = this.interactables.get(i);
            if (interactableType.isInstance(interactable)) {
                return interactableType.cast(interactable);
            }
        }
        return null;
    }

    protected void addInteractable(@NonNull Interactable interactable) {
        this.interactables.add(interactable);
        interactable.setDevice(this);
    }

    @Nullable
    public <ObservableType extends DeviceDataObservable> ObservableType getObservable(Class<ObservableType> observableType) {
        for (int i = 0; i < this.observables.size(); i++) {
            DeviceDataObservable observable = this.observables.get(i);
            if (observableType.isInstance(observable)) {
                return observableType.cast(observable);
            }
        }
        return null;
    }

    protected void addObservable(@NonNull DeviceDataObservable observable) {
        this.observables.add(observable);
        observable.setDevice(this);
    }

    public void setDeviceObserver(DeviceObserver observer) {
        this.observer = observer;
    }

    public final void notifyStart() {
        this.observer.onStart(this);
    }

    public final void notifyEnd() {
        this.observer.onEnd(this);
    }

    public final DeviceDataProvider notifyFeatureDetected(@Nullable Class<? extends DeviceDataProvider> feature, @Nullable DeviceDataObservable observable, @Nullable Interactable interactable) {
        if (interactable != null) {
            this.addInteractable(interactable);
        }
        if (observable != null) {
            this.addObservable(observable);
        }
        DeviceDataProvider provider = this.observer.onFeatureDetected(feature, this);
        return provider;
    }

    public abstract int getDeviceId();
    public abstract boolean connectDevice();
    public abstract void disconnectDevice();
    public abstract boolean isConnected();

}
