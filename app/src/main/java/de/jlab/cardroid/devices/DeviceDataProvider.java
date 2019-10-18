package de.jlab.cardroid.devices;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.devices.serial.carduino.CarduinoEventProvider;
import de.jlab.cardroid.errors.ErrorDataProvider;
import de.jlab.cardroid.gps.GpsDataProvider;

public abstract class DeviceDataProvider {

    private DeviceService service;
    private ArrayList<DeviceHandler> devices = new ArrayList<>();

    public DeviceDataProvider(@NonNull DeviceService service) {
        this.service = service;
    }

    public int getConnectedDeviceCount() {
        int connectedDevices = 0;
        for(int i = 0; i < this.devices.size(); i++) {
            if (this.devices.get(i).getState() == DeviceHandler.State.READY) {
                connectedDevices++;
            }
        }
        return connectedDevices;
    }

    public void start(@NonNull DeviceHandler device) {
        // Device connections are threaded, so if the exact same device (by id) already provides data, we replace it and call update
        for (int i = 0; i < this.devices.size(); i++) {
            DeviceHandler deviceToCheck = this.devices.get(i);
            // FIXME should this check be "isPhysicalDevice(device)" ???
            if (deviceToCheck.equals(device)) {
                this.devices.set(i, device);
                this.onUpdate(deviceToCheck, device, this.service);
                return;
            }
        }

        this.devices.add(device);
        this.onStart(device, this.service);
    }

    public boolean usesDevice(DeviceHandler device) {
        for (int i = 0; i < this.devices.size(); i++) {
            if (this.devices.get(i).equals(device)) {
                return true;
            }
        }
        return false;
    }

    public void stop() {
        for (int i = 0; i < this.devices.size(); i++) {
            this.onStop(this.devices.get(i), this.service);
        }
    }

    protected ArrayList<DeviceHandler> getDevices() {
        return this.devices;
    }

    protected abstract void onUpdate(@NonNull DeviceHandler previousDevice, @NonNull DeviceHandler newDevice, @NonNull DeviceService service);
    protected abstract void onStop(@NonNull DeviceHandler device, @NonNull DeviceService service);
    protected abstract void onStart(@NonNull DeviceHandler device, @NonNull DeviceService service);

    @NonNull
    public static DeviceDataProvider createFrom(@NonNull Class providerType, @NonNull DeviceService service) {
        if (providerType.equals(GpsDataProvider.class)) {
            return new GpsDataProvider(service);
        }
        if (providerType.equals(CanDataProvider.class)) {
            return new CanDataProvider(service);
        }
        if (providerType.equals(CarduinoEventProvider.class)) {
            return new CarduinoEventProvider(service);
        }
        if (providerType.equals(ErrorDataProvider.class)) {
            return new ErrorDataProvider(service);
        }

        throw new IllegalArgumentException("Provider \"" + providerType.getSimpleName() + "\" is not registered in DeviceDataProvider.");
    }

}
