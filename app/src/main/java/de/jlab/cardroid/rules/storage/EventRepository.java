package de.jlab.cardroid.rules.storage;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

import androidx.lifecycle.LiveData;
import de.jlab.cardroid.storage.CardroidDatabase;

public class EventRepository {

    private EventDao eventDao;

    public EventRepository(Application application) {
        CardroidDatabase db = CardroidDatabase.getDatabase(application);
        this.eventDao = db.eventDao();
    }

    public LiveData<List<EventEntity>> getAll() {
        return this.eventDao.getAll();
    }

    public List<EventEntity> getAllSynchronous() {
        return this.eventDao.getAllSynchronous();
    }

    public LiveData<EventEntity> get(int identifier) {
        return this.eventDao.get(identifier);
    }

    public List<RuleDefinition> getAllRulesSynchronous() {
        return this.eventDao.getAllRulesSynchronous();
    }

    public LiveData<List<RuleDefinition>> getAllRules() {
        return this.eventDao.getAllRules();
    }

    public LiveData<RuleDefinition> getAsRule(int identifier) {
        return this.eventDao.getAsRule(identifier);
    }

    public void insert(EventEntity... eventEntities) {
        new insertAsyncTask(this.eventDao).execute(eventEntities);
    }

    public void update(EventEntity... eventEntities) {
        new updateAsyncTask(this.eventDao).execute(eventEntities);
    }

    private static class insertAsyncTask extends AsyncTask<EventEntity, Void, Void> {
        private EventDao asyncTaskDao;

        insertAsyncTask(EventDao dao) {
            this.asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final EventEntity... eventEntities) {
            this.asyncTaskDao.insert(eventEntities);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<EventEntity, Void, Void> {
        private EventDao asyncTaskDao;

        updateAsyncTask(EventDao dao) {
            this.asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final EventEntity... eventEntities) {
            this.asyncTaskDao.update(eventEntities);
            return null;
        }
    }

}
