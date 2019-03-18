package de.jlab.cardroid.rules;

import android.app.Application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.LiveData;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.EventRepository;

public final class KnownEvents {

    private Map<Integer, Event> eventMap = new HashMap<>();

    public KnownEvents(Application application) {
        EventRepository eventRepository = new EventRepository(application);
        List<EventEntity> eventList = eventRepository.getAllSynchronous();
        if (eventList != null) {
            for (EventEntity eventEntity : eventList) {
                this.eventMap.put(eventEntity.identifier, new Event(eventEntity.identifier));
            }
        }
    }

    public void addEvent(Event event) {
        this.eventMap.put(event.getIdentifier(), event);
    }

    public Event getEventFromIdentifier(int identifier) {
        return this.eventMap.get(identifier);
    }

}
