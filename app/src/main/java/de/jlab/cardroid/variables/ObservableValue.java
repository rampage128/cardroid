package de.jlab.cardroid.variables;

import java.util.ArrayList;
import java.util.Objects;

public class ObservableValue {

    private ArrayList<ValueObserver> observers = new ArrayList<>();
    private Object value;

    public ObservableValue(Object value) {
        this.value = value;
    }

    protected void change(Object value) {
        if (!Objects.equals(this.value, value)) {
            this.notifyChange(this.value, value);
        }
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }

    private void notifyChange(Object oldValue, Object newValue) {
        for (int i = 0; i < this.observers.size(); i++) {
            this.observers.get(i).onChange(oldValue, newValue);
        }
    }

    public void addObserver(ValueObserver observer) {
        this.observers.add(observer);
    }

    public void removeObserver(ValueObserver observer) {
        this.observers.remove(observer);
    }


    public interface ValueObserver {
        void onChange(Object oldValue, Object newValue);
    }

}
