package de.jlab.cardroid.variables;

import android.util.Log;

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
        Scriptable scope = this.context.initStandardObjects();
        return new Expression(expression, variables, contextName, scope, this);
    }

    public Object evaluate(String expression, String contextName, Scriptable scope) {
        Context context = Context.enter();
        context.setOptimizationLevel(-1); // prevent JVM bytecode generation
        Object result = 0L;
        try {
            result = context.evaluateString(scope, expression, contextName, 1, null);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error evaluating \"" + expression + "\"", e);
        } finally {
            Context.exit();
        }

        return result;
    }

}
