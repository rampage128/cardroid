package de.jlab.cardroid.car;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import de.jlab.cardroid.usb.CarSystemSerialPacket;

public abstract class CarSystem {

    private HashMap<String, Property> properties = new HashMap<>();

    // TODO potentially make this a HashMap of Lists, so ChangeListeners can register to individual properties
    private ArrayList<ChangeListener> changeListeners = new ArrayList<>();

    public CarSystem() {
        this.registerProperties();
    }

    public final void addChangeListener(ChangeListener listener) {
        this.changeListeners.add(listener);
    }

    public final void removeChangeListener(ChangeListener listener) {
        this.changeListeners.remove(listener);
    }

    public final void addPropertyChangeListener(String key, Property.ChangeListener listener) {
        Property property = this.properties.get(key);
        if (property != null) {
            property.addChangeListener(listener);
        }
    }

    public final void removePropertyChangeListener(String key, Property.ChangeListener listener) {
        Property property = this.properties.get(key);
        if (property != null) {
            property.removeChangeListener(listener);
        }
    }

    public final void updateFromPacket(CarSystemSerialPacket packet) {
        this.updateDataFromPacket(packet);
        for (ChangeListener changeListener : this.changeListeners) {
            changeListener.onChange(this);
        }
    }

    public String[] getPropertyKeys() {
        return this.properties.keySet().toArray(new String[0]);
    }

    public Property get(String key) {
        return this.properties.get(key);
    }


    protected void registerProperty(String key, Object initialValue) {
        this.properties.put(key, new Property(initialValue));
    }

    protected void updateProperty(String key, Object value) {
        Property property = this.properties.get(key);
        assert property != null: "Property " + key + " was not registered!";
        property.set(value);
    }


    protected abstract void registerProperties();
    protected abstract void updateDataFromPacket(CarSystemSerialPacket packet);


    public interface ChangeListener<T extends CarSystem> {
        void onChange(T system);
    }

    public static class Property {

        private Object value;
        private ArrayList<ChangeListener> listeners = new ArrayList<>();

        public Property(Object value) {
            this.value = value;
        }

        public void set(Object value) {
            for (ChangeListener listener : this.listeners) {
                listener.onChange(value, this.value);
            }
            this.value = value;
        }

        public Object get() {
            return this.value;
        }

        public void addChangeListener(ChangeListener listener) {
            this.listeners.add(listener);
        }

        public void removeChangeListener(ChangeListener listener) {
            this.listeners.remove(listener);
        }

        public interface ChangeListener {
            void onChange(Object value, Object previousValue);
        }

        public String toString() {
            return Objects.toString(this.value);
        }
    }
}
