package de.jlab.cardroid.rules.storage;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class ActionViewDetailModel extends AndroidViewModel {

    private ActionRepository repository;

    public ActionViewDetailModel(Application application) {
        super(application);
        repository = new ActionRepository(application);
    }

    public LiveData<ActionEntity> get(int uid) { return repository.get(uid); }

    public void insert(ActionEntity actionEntity) {
        this.repository.insert(actionEntity);
    }

    public void update(ActionEntity actionEntity) {
        this.repository.update(actionEntity);
    }

}
