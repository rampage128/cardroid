package de.jlab.cardroid.devices.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;

public final class DeviceDetailViewModel extends AndroidViewModel {

    private DeviceRepository repository;

    public DeviceDetailViewModel(@NonNull Application application) {
        super(application);
        this.repository = new DeviceRepository(application);

    }

    public LiveData<DeviceEntity> getDeviceEntity(int entityId) {
        return this.repository.get(entityId);
    }

    public void update(DeviceEntity entity) {
        this.repository.update(entity);
    }

}
