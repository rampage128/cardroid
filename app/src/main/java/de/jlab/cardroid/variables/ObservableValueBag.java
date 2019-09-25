package de.jlab.cardroid.variables;

import java.util.HashMap;

public class ObservableValueBag {

    private HashMap<String, ObservableValue> variables = new HashMap<>();

    public void put(String name, ObservableValue value) {
        this.variables.put(name, value);
    }

    public ObservableValue get(String name) {
        return this.variables.get(name);
    }

    public ObservableValueBag allFromExpression(String expression) {
        ObservableValueBag bag = new ObservableValueBag();
        for (String name : this.variables.keySet()) {
            if (expression.contains(name)) {
                bag.put(name, this.variables.get(name));
            }
        }
        return bag;
    }

    public String[] getNames() {
        return this.variables.keySet().toArray(new String[0]);
    }

    public static BagBuilder build() {
        return new BagBuilder();
    }

    public static class BagBuilder {
        private ObservableValueBag bag;

        private BagBuilder() {
            this.bag = new ObservableValueBag();
        }

        public BagBuilder addVariable(String name, ObservableValue value) {
            this.bag.put(name, value);
            return this;
        }

        public ObservableValueBag get() {
            return this.bag;
        }
    }

}
