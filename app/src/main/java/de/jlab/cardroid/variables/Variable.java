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

    public static Variable createFromExpression(@NonNull String name, @NonNull String expressionString, @NonNull ObservableValue value, @NonNull ObservableValue maxValue, @NonNull ScriptEngine engine) {
        ObservableValue targetValue = value;
        if (expressionString.trim().length() > 0 && !expressionString.trim().equals("value")) {
            Expression expression = engine.createExpression(expressionString, null, name);
            expression.addVariable("value", value);
            expression.addVariable("max", maxValue);
            targetValue = expression;
        }

        return new Variable(name, targetValue);
    }

    public static Variable createPlain(@NonNull String name, @NonNull ObservableValue value) {
        return new Variable(name, value);
    }

    @Override
    public void onChange(Object oldValue, Object newValue) {
        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).onChange(oldValue, newValue, this.name);
        }
    }

    public interface VariableChangeListener {
        void onChange(Object oldValue, Object newValue, String variableName);
    }

}
