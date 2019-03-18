package de.jlab.cardroid.rules.actions;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.util.HashMap;

import de.jlab.cardroid.rules.Action;
import de.jlab.cardroid.rules.properties.ActionPropertyEditor;
import de.jlab.cardroid.rules.properties.OptionPropertyEditor;
import de.jlab.cardroid.rules.properties.PropertyValue;

public final class WifiToggleAction extends Action {

    private static final String PREFIX = "action_wifitoggle";

    private static final String PROPERTY_STATE = PREFIX + "_state";

    @Override
    public void execute(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled((Boolean)this.getPropertyValue(PROPERTY_STATE));
    }

    @Override
    protected void setKnownProperties(HashMap<String, ActionPropertyEditor> knownProperties) {
        knownProperties.put(PROPERTY_STATE, new OptionPropertyEditor(new PropertyValue[] {
                new PropertyValue<>(true, PREFIX + "_on"),
                new PropertyValue<>(false, PREFIX + "_off")
        }, 0));
    }

}
