package de.jlab.cardroid.rules.actions;

import android.app.ActivityManager;
import android.content.Context;

import java.util.HashMap;

import de.jlab.cardroid.rules.Action;
import de.jlab.cardroid.rules.properties.ActionPropertyEditor;
import de.jlab.cardroid.rules.properties.AppSelectionPropertyEditor;

public final class KillAppAction extends Action {

    private static final String PREFIX = "action_killapp";

    private static final String PROPERTY_PACKAGE = PREFIX + "_package";

    @Override
    public void execute(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        am.restartPackage(PROPERTY_PACKAGE);
        am.killBackgroundProcesses((String)getPropertyValue(PROPERTY_PACKAGE));
    }

    @Override
    protected void setKnownProperties(HashMap<String, ActionPropertyEditor> knownProperties) {
        knownProperties.put(PROPERTY_PACKAGE, new AppSelectionPropertyEditor());
    }
}
