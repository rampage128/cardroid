package de.jlab.cardroid.car;

import java.util.ArrayList;

import de.jlab.cardroid.usb.CarSystemSerialPacket;

public abstract class CarSystem {

    private ArrayList<ChangeListener> changeListeners = new ArrayList<>();

    public final void addChangeListener(ChangeListener listener) {
        this.changeListeners.add(listener);
    }

    public final void removeChangeListener(ChangeListener listener) {
        this.changeListeners.remove(listener);
    }

    public final void updateFromPacket(CarSystemSerialPacket packet) {
        this.updateDataFromPacket(packet);
        for (ChangeListener changeListener : this.changeListeners) {
            changeListener.onChange(this);
        }
    }

    protected abstract void updateDataFromPacket(CarSystemSerialPacket packet);

    public interface ChangeListener<T extends CarSystem> {
        void onChange(T system);
    }
}
