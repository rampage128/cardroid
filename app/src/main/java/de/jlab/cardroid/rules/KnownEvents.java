package de.jlab.cardroid.rules;

import java.util.HashMap;
import java.util.Map;

public final class KnownEvents {

    private Map<Integer, Event> eventMap = new HashMap<>();

    public void addEvent(Event event) {
        this.eventMap.put(event.getIdentifier(), event);
    }

    public Event getEventFromIdentifier(int identifier) {
        return this.eventMap.get(identifier);
    }

}
