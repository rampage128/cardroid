package de.jlab.cardroid.rules;

import java.util.HashMap;
import java.util.Map;

import de.jlab.cardroid.rules.storage.EventEntity;

public final class KnownEvents {

    private Map<EventEntity, Event> eventMap = new HashMap<>();

    public void addEvent(Event event) {
        this.eventMap.put(event.getDescriptor(), event);
    }

    public Event getEvent(EventEntity descriptor) {
        return this.eventMap.get(descriptor);
    }

}
