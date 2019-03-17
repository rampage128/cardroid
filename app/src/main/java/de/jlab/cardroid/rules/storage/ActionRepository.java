package de.jlab.cardroid.rules.storage;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

public class ActionRepository {

    private ActionDao actionDao;

    public ActionRepository(Application application) {
        RulesDatabase db = RulesDatabase.getDatabase(application);
        this.actionDao = db.actionDao();
    }

    public LiveData<ActionEntity> get(int identifier) {
        return this.actionDao.get(identifier);
    }

    public void update(ActionEntity actionEntity) {
        new ActionRepository.updateAsyncTask(this.actionDao).execute(actionEntity);
    }

    public void insert(ActionEntity actionEntity) {
        new ActionRepository.insertAsyncTask(this.actionDao).execute(actionEntity);
    }

    public void delete(ActionEntity... uids) {
        new ActionRepository.deleteAsyncTask(this.actionDao).execute(uids);
    }


    private static class deleteAsyncTask extends AsyncTask<ActionEntity, Void, Void> {

        private ActionDao asyncTaskDao;

        deleteAsyncTask(ActionDao dao) {
            this.asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final ActionEntity... uids) {
            this.asyncTaskDao.delete(uids);
            return null;
        }
    }

    private static class insertAsyncTask extends AsyncTask<ActionEntity, Void, Void> {

        private ActionDao asyncTaskDao;

        insertAsyncTask(ActionDao dao) {
            this.asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final ActionEntity... actionEntities) {
            this.asyncTaskDao.insert(actionEntities);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<ActionEntity, Void, Void> {

        private ActionDao asyncTaskDao;

        updateAsyncTask(ActionDao dao) {
            this.asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final ActionEntity... actionEntities) {
            this.asyncTaskDao.update(actionEntities);
            return null;
        }
    }

}
