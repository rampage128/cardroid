package de.jlab.cardroid.variables;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class ScriptEngine {

    private Context context;
    private Scriptable scope;

    public ScriptEngine() {
        this.context = Context.enter();
        this.context.setOptimizationLevel(-1); // prevent JVM bytecode generation
        this.scope = context.initStandardObjects();
    }

    public Expression createGlobalExpression(String expression, ObservableValueBag variables, String contextName) {
        return new Expression(expression, variables, contextName, this.scope, this);
    }

    public Expression createExpression(String expression, ObservableValueBag variables, String contextName) {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        return new Expression(expression, variables, contextName, scope, this);
    }

    Object evaluate(String expression, String contextName, Scriptable scope) {
        return context.evaluateString(scope, expression, contextName, 1, null);
    }

}
