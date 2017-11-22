package de.jlab.cardroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.view.WindowManager;

public class PowerManagementReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            PowerManager.WakeLock wl = powerManager.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Cardroid");
            wl.acquire();
        }
        else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {

        }
    }
}
