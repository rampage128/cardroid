package de.jlab.cardroid.devices.ui;

import android.app.Application;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;

public final class DeviceListViewModel extends AndroidViewModel {

    private LiveData<List<DeviceEntity>> devices;

    public DeviceListViewModel(Application application) {
        super(application);
        DeviceRepository repository = new DeviceRepository(application);
        this.devices = repository.getAll();
    }

    public LiveData<List<DeviceEntity>> getAll() { return this.devices; }

}
