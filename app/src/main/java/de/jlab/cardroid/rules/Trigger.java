package de.jlab.cardroid.rules;

public final class Trigger {

    private Event event;

    public Trigger(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return this.event;
    }

    public boolean triggers(Event otherEvent) {
        return this.event.equals(otherEvent);
    }

}
