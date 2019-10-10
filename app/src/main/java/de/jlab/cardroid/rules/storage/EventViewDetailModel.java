package de.jlab.cardroid.rules.storage;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class EventViewDetailModel extends AndroidViewModel {

    private EventRepository repository;

    public EventViewDetailModel(Application application) {
        super(application);
        repository = new EventRepository(application);
    }

    public LiveData<RuleDefinition> getAsRule(int eventIdentifier) { return repository.getAsRule(eventIdentifier); }

    public void updateEvent(EventEntity eventEntity) { this.repository.update(eventEntity); }

}
