package de.jlab.cardroid.devices;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;

public final class DeviceConnection {

    private DeviceEntity descriptor;
    private DeviceHandler device;

    public DeviceConnection(@NonNull DeviceHandler device, @NonNull Context context) {
        this.device = device;
        this.descriptor = new DeviceEntity(null, context.getString(DeviceType.get(device.getClass()).getTypeName()), device.getClass());
    }

    public void updateDescriptor(@NonNull DeviceEntity descriptor) {
        this.descriptor = descriptor;
    }

    public DeviceHandler getDevice() {
        return this.device;
    }

    public DeviceEntity getDescriptor() {
        return this.descriptor;
    }

    public boolean isDevice(@NonNull DeviceEntity descriptor) {
        return this.descriptor.uid == descriptor.uid;
    }

    public boolean isPhysicalDevice(@NonNull DeviceHandler device) {
        return this.device.getConnectionId().equals(device.getConnectionId());
    }

    public boolean isConnected() {
        return this.device.isConnected();
    }

    public void addFeature(@NonNull Class<? extends DeviceDataProvider> feature, @NonNull Application app) {
        this.descriptor.addFeature(feature);

        DeviceRepository repo = new DeviceRepository(app);
        repo.update(this.descriptor);
    }

}
