package de.jlab.cardroid.rules;

import android.content.Context;

import androidx.annotation.NonNull;

public class ActionDefinition {
    private Class<? extends Action> actionType = null;
    private String label = null;

    public ActionDefinition(@NonNull Class<? extends Action> actionType, Context context) {
        this.actionType = actionType;
        this.label = getLocalizedNameFromClassName(actionType.getSimpleName(), context);
    }

    public Class<? extends Action> getActionType() {
        return this.actionType;
    }

    @Override
    public String toString() {
        return this.label;
    }

    public static String getLocalizedNameFromClassName(@NonNull String className, Context context) {
        String identifier = className.substring(className.lastIndexOf('.') + 1).replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        int resId = context.getResources().getIdentifier(identifier, "string", context.getPackageName());
        if (resId > 0) {
            return context.getResources().getString(resId);
        }
        return identifier;
    }
}
