package de.jlab.cardroid.variables;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class Variable implements ObservableValue.ValueObserver {

    private String name;
    private ObservableValue value;

    private ArrayList<VariableChangeListener> listeners = new ArrayList<>();

    public Variable(@NonNull String name) {
        this(name, new ObservableValue(0));
    }

    public Variable(@NonNull String name, @NonNull ObservableValue value) {
        this.name = name;
        this.value = value;
        value.addObserver(this);
    }

    public String getName() {
        return this.name;
    }

    public ObservableValue getValue() {
        return this.value;
    }

    public void setObservableValue(@NonNull ObservableValue value) {
        this.value.removeObserver(this);
        this.value = value;
        value.addObserver(this);
    }

    public void addChangeListener(@NonNull VariableChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeChangeListener(@NonNull VariableChangeListener listener) {
        this.listeners.remove(listener);
    }

    public void dispose() {
        this.listeners.clear();
    }

    @Override
    public void onChange(Object oldValue, Object newValue) {
        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).onChange(oldValue, newValue);
        }
    }

    public interface VariableChangeListener {
        void onChange(Object oldValue, Object newValue);
    }

}
