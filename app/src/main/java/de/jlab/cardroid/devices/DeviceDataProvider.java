package de.jlab.cardroid.devices;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.gps.GpsDataProvider;

public abstract class DeviceDataProvider<DeviceType extends DeviceHandler> {

    private DeviceService service;
    private ArrayList<DeviceType> devices = new ArrayList<>();

    public DeviceDataProvider(@NonNull DeviceService service) {
        this.service = service;
    }

    public int getConnectedDeviceCount() {
        int connectedDevices = 0;
        for(int i = 0; i < this.devices.size(); i++) {
            if (this.devices.get(i).isConnected()) {
                connectedDevices++;
            }
        }
        return connectedDevices;
    }

    /*
    public boolean isConnected() {
        return this.device != null && this.device.isConnected();
    }
     */

    public void start(@NonNull DeviceType device) {
        // Device connections are threaded, so if the exact same device (by id) already provides data, we replace it and call update
        for (int i = 0; i < this.devices.size(); i++) {
            DeviceType deviceToCheck = this.devices.get(i);
            if (deviceToCheck.getDeviceId() == device.getDeviceId()) {
                this.devices.set(i, deviceToCheck);
                this.onUpdate(deviceToCheck, device, this.service);
                return;
            }
        }

        this.devices.add(device);
        this.onStart(device, this.service);
    }

    public boolean usesDevice(DeviceHandler device) {
        for (int i = 0; i < this.devices.size(); i++) {
            if (this.devices.get(i).getDeviceId() == device.getDeviceId()) {
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

    protected ArrayList<DeviceType> getDevices() {
        return this.devices;
    }

    protected abstract void onUpdate(@NonNull DeviceType previousDevice, @NonNull DeviceType newDevice, @NonNull DeviceService service);
    protected abstract void onStop(@NonNull DeviceType device, @NonNull DeviceService service);
    protected abstract void onStart(@NonNull DeviceType device, @NonNull DeviceService service);

    @NonNull
    public static DeviceDataProvider createFrom(@NonNull Class providerType, @NonNull DeviceService service) {
        if (providerType.equals(GpsDataProvider.class)) {
            return new GpsDataProvider(service);
        }
        if (providerType.equals(CanDataProvider.class)) {
            return new CanDataProvider(service);
        }

        throw new IllegalArgumentException("Provider \"" + providerType.getSimpleName() + "\" is not registered in DeviceDataProvider.");
    }

}
