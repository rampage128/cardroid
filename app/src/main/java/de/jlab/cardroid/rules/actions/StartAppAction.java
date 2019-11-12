package de.jlab.cardroid.rules.actions;

import android.content.Context;
import android.content.Intent;

import java.util.HashMap;

import de.jlab.cardroid.rules.Action;
import de.jlab.cardroid.rules.properties.ActionPropertyEditor;
import de.jlab.cardroid.rules.properties.AppSelectionPropertyEditor;

public final class StartAppAction extends Action {

    private static final String PREFIX = "action_startapp";

    private static final String PROPERTY_PACKAGE = PREFIX + "_package";

    @Override
    public void execute(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage((String)getPropertyValue(PROPERTY_PACKAGE));
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        // TODO handle case of uninstalled app
    }

    @Override
    protected void setKnownProperties(HashMap<String, ActionPropertyEditor> knownProperties) {
        knownProperties.put(PROPERTY_PACKAGE, new AppSelectionPropertyEditor());
    }
}
