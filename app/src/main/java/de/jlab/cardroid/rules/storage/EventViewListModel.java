package de.jlab.cardroid.rules.storage;

import android.app.Application;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class EventViewListModel extends AndroidViewModel {

    private LiveData<List<EventEntity>> allEvents;

    public EventViewListModel(Application application) {
        super(application);
        EventRepository repository = new EventRepository(application);
        this.allEvents = repository.getAll();
    }

    public LiveData<List<EventEntity>> getAll() { return this.allEvents; }

}
