package de.jlab.cardroid.car;

import java.util.ArrayList;

import de.jlab.cardroid.usb.SerialCarButtonEventPacket;

public abstract class ManageableCarSystem extends CarSystem {

    private ArrayList<CarSystemEventListener> eventListeners = new ArrayList<>();

    public void addEventListener(CarSystemEventListener listener) {
        this.eventListeners.add(listener);
    }

    protected void manage(CarSystemEvent event) {
        this.manage(event, null);
    }

    protected void manage(CarSystemEvent event, byte[] payload) {
        for (CarSystemEventListener listener : eventListeners) {
            listener.onTrigger(CarSystemEvent.serialize(event, payload));
        }
    }

    public interface CarSystemEventListener {
        void onTrigger(SerialCarButtonEventPacket packet);
    }

}
