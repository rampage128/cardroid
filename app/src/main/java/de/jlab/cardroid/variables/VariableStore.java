package de.jlab.cardroid.variables;

import java.util.HashMap;

import androidx.annotation.NonNull;

public class VariableStore {

    private HashMap<String, Variable> variables = new HashMap<>();

    public void registerVariable(@NonNull Variable variable) {
        String name = variable.getName();
        if (this.variables.containsKey(name)) {
            this.variables.get(name).dispose();
        }
        this.variables.put(name, variable);
    }

    public void unregisterVariable(@NonNull String name) {
        Variable variable = this.variables.remove(name);
        if (variable != null) {
            variable.dispose();
        }
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
    }

}
