package de.jlab.cardroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

@Deprecated
public class WifiWatchDog implements WatchDogService.WatchDog {
    private Context context;
    private WifiManager wifiManager;

    public WifiWatchDog(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public Intent watch() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        boolean toggleWifi = prefs.getBoolean("power_toggle_wifi", false);

        if (toggleWifi && !this.wifiManager.isWifiEnabled()) {
            return INTENT_TRIGGER;
        }

        return INTENT_IGNORE;
    }

    @Override
    public void trigger(Intent intent) {
        wifiManager.setWifiEnabled(true);
    }
}
