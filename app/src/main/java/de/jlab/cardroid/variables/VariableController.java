package de.jlab.cardroid.variables;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;

public final class VariableController {

    private HashMap<String, Variable> variables = new HashMap<>();
    private HashMap<String, ArrayList<Variable.VariableChangeListener>> subscribers = new HashMap<>();

    public void registerVariable(@NonNull Variable variable) {
        String name = variable.getName();
        if (this.variables.containsKey(name)) {
            this.variables.get(name).dispose();
        }
        this.variables.put(name, variable);

        ArrayList<Variable.VariableChangeListener> variableSubscribers = this.getOrCreateSubscriberList(variable.getName());
        for (Variable.VariableChangeListener subscriber : variableSubscribers) {
            variable.addChangeListener(subscriber);
        }
    }

    public void unregisterVariable(@NonNull String name) {
        Variable variable = this.variables.remove(name);
        if (variable != null) {
            variable.dispose();
        }

        this.subscribers.remove(name);
    }

    public void subscribe(@NonNull String variableName, @NonNull Variable.VariableChangeListener subscriber) {
        ArrayList<Variable.VariableChangeListener> variableSubscribers = this.getOrCreateSubscriberList(variableName);
        variableSubscribers.add(subscriber);

        Variable variable = this.variables.get(variableName);
        if (variable != null) {
            variable.addChangeListener(subscriber);
        }
    }

    public void unsubscribe(@NonNull String variableName, @NonNull Variable.VariableChangeListener subscriber) {
        ArrayList<Variable.VariableChangeListener> variableSubscribers = this.subscribers.get(variableName);
        if (variableSubscribers != null) {
            variableSubscribers.remove(subscriber);
        }

        Variable variable = this.variables.get(variableName);
        if (variable != null) {
            variable.removeChangeListener(subscriber);
        }
    }

    private ArrayList<Variable.VariableChangeListener> getOrCreateSubscriberList(@NonNull String variableName) {
        ArrayList<Variable.VariableChangeListener> variableSubscribers = this.subscribers.get(variableName);
        if (variableSubscribers == null) {
            variableSubscribers = new ArrayList<>();
            this.subscribers.put(variableName, variableSubscribers);
        }
        return variableSubscribers;
    }

    public Variable[] getAll() {
        return this.variables.values().toArray(new Variable[0]);
    }

    public Variable getVariable(@NonNull String name) {
        return this.variables.get(name);
    }

    public void dispose() {
        for (Variable variable : this.variables.values()) {
            variable.dispose();
        }
        this.variables.clear();
        this.subscribers.clear();
    }

}
