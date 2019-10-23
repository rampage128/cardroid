package de.jlab.cardroid.devices.serial.carduino;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.FeatureDataProvider;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.providers.DataProviderService;

public final class CarduinoEventProvider extends FeatureDataProvider {

    private ArrayList<CarduinoEventParser.EventListener> externalListeners = new ArrayList<>();

    private CarduinoEventParser.EventListener listener = eventNum -> {
        for (int i = 0; i < this.externalListeners.size(); i++) {
            this.externalListeners.get(i).onEvent(eventNum);
        }
    };

    public CarduinoEventProvider(@NonNull DataProviderService service) {
        super(service);
    }

    public void sendEvent(@NonNull CarduinoEventType event, @Nullable byte[] payload) {
        ArrayList<DeviceHandler> devices = this.getFeature();
        for (int i = 0; i < devices.size(); i++) {
            DeviceHandler device = devices.get(i);
            EventInteractable interactable = device.getFeature(EventInteractable.class);
            if (interactable != null) {
                interactable.sendEvent(event.getCommand(), payload);
            }
        }
    }

    public void subscribe(CarduinoEventParser.EventListener listener) {
        this.externalListeners.add(listener);
    }

    public void unsubscribe(CarduinoEventParser.EventListener listener) {
        this.externalListeners.remove(listener);
    }

    private void deviceRemoved(@NonNull DeviceHandler device) {
        EventObservable observable = device.getObservable(EventObservable.class);
        if (observable != null) {
            observable.removeEventListener(this.listener);
        }
    }

    private void deviceAdded(@NonNull DeviceHandler device) {
        EventObservable observable = device.getObservable(EventObservable.class);
        if (observable != null) {
            observable.addEventListener(this.listener);
        }
    }

    @Override
    protected void onUpdate(@NonNull DeviceHandler previousDevice, @NonNull DeviceHandler newDevice, @NonNull DeviceService service) {
        this.deviceRemoved(previousDevice);
        this.deviceAdded(newDevice);
    }

    @Override
    protected void onStop(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        this.deviceRemoved(device);
    }

    @Override
    protected void onStart(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        this.deviceAdded(device);
    }

}
