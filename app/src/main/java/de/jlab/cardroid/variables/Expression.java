package de.jlab.cardroid.variables;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.ArrayList;

public final class Expression extends ObservableValue {

    private ArrayList<VariableHandler> variableHandlers = new ArrayList<>();

    private String expression;
    private String contextName;

    private ScriptEngine engine;
    private Scriptable scope;

    public Expression(String expression, ObservableValueBag variables, String contextName, Scriptable scope, ScriptEngine engine) {
        super(0);
        this.expression = expression;
        this.contextName = contextName;
        this.scope = scope;
        this.engine = engine;
        if (variables != null) {
            String[] variableNames = variables.getNames();
            for (String name : variableNames) {
                if (expression.contains(name)) {
                    this.addVariable(name, variables.get(name));
                }
            }
        }
    }

    public void addVariable(String name, ObservableValue value) {
        this.variableHandlers.add(new VariableHandler(name, value));
    }

    public String getExpression() {
        return this.expression;
    }

    private Object evaluate() {
        return this.engine.evaluate(this.expression, this.contextName, this.scope);
    }

    private class VariableHandler {
        private ObservableValue.ValueObserver listener;
        private String name;

        VariableHandler(String name, ObservableValue value) {
            this.name = name;
            this.listener = (oldValue, newValue) -> {
                ScriptableObject.putProperty(Expression.this.scope, this.name, newValue);
                Expression.this.change(Expression.this.evaluate());
            };
            value.addObserver(this.listener);
        }
    }

}
