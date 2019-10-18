package de.jlab.cardroid.devices.storage;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.storage.CardroidDatabase;

public final class DeviceRepository {

    private DeviceDao deviceDao;

    public DeviceRepository(Application application) {
        CardroidDatabase db = CardroidDatabase.getDatabase(application);
        this.deviceDao = db.deviceDao();
    }

    public LiveData<List<DeviceEntity>> getAll() {
        return this.deviceDao.getAll();
    }

    public List<DeviceEntity> getAllSynchronous() {
        return this.deviceDao.getAllSynchronous();
    }

    public LiveData<DeviceEntity> get(int identifier) {
        return this.deviceDao.get(identifier);
    }

    public List<DeviceEntity> getSynchronous(@NonNull DeviceUid deviceUid) {
        return this.deviceDao.getSynchronous(deviceUid.toString());
    }

    public void insert(DeviceEntity... deviceEntities) {
        new insertAsyncTask(this.deviceDao).execute(deviceEntities);
    }

    public void update(DeviceEntity... deviceEntities) {
        new updateAsyncTask(this.deviceDao).execute(deviceEntities);
    }

    public void delete(DeviceEntity... deviceEntities) {
        new deleteAsyncTask(this.deviceDao).execute(deviceEntities);
    }

    private static class insertAsyncTask extends AsyncTask<DeviceEntity, Void, Void> {
        private DeviceDao asyncTaskDao;

        insertAsyncTask(DeviceDao dao) {
            this.asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DeviceEntity... deviceEntities) {
            this.asyncTaskDao.insert(deviceEntities);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<DeviceEntity, Void, Void> {
        private DeviceDao asyncTaskDao;

        updateAsyncTask(DeviceDao dao) {
            this.asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DeviceEntity... deviceEntities) {
            this.asyncTaskDao.update(deviceEntities);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<DeviceEntity, Void, Void> {
        private DeviceDao asyncTaskDao;

        deleteAsyncTask(DeviceDao dao) { this.asyncTaskDao = dao; }

        @Override
        protected Void doInBackground(DeviceEntity... deviceEntities) {
            this.asyncTaskDao.delete(deviceEntities);
            return null;
        }
    }

}
