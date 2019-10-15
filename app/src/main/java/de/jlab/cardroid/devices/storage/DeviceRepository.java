package de.jlab.cardroid.devices.storage;

import android.app.Application;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
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

    public List<DeviceEntity> getSynchronous(String deviceUid) {
        try {
            return new getAsyncTask(this.deviceDao).execute(deviceUid).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void insert(DeviceEntity... eventEntities) {
        new insertAsyncTask(this.deviceDao).execute(eventEntities);
    }

    public void update(DeviceEntity... eventEntities) {
        new updateAsyncTask(this.deviceDao).execute(eventEntities);
    }

    private static class getAsyncTask extends AsyncTask<String, Void, List<DeviceEntity>> {
        private DeviceDao asyncTaskDao;

        getAsyncTask(DeviceDao dao) {
            this.asyncTaskDao = dao;
        }

        protected List<DeviceEntity> doInBackground(final String... deviceUids) {
            return this.asyncTaskDao.getSynchronous(deviceUids[0]);
        }

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

}
