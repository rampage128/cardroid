package de.jlab.cardroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.WindowManager;

public class PowerManagementReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences powerPrefs = context.getSharedPreferences("powermanagement", Context.MODE_PRIVATE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean toggleScreen = prefs.getBoolean("power_toggle_screen", false);
        boolean toggleWifi = prefs.getBoolean("power_toggle_wifi", false);
        boolean resumeMusic = prefs.getBoolean("power_resume_music", false);

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            if (resumeMusic && powerPrefs.getBoolean("was_playing_music", false)) {
                sendMediaEvent(KeyEvent.KEYCODE_MEDIA_PLAY, context);
            }
            if (toggleScreen) {
                toggleScreen(true, powerPrefs, context);
            }
            if (toggleWifi && powerPrefs.getBoolean("was_wifi_on", false)) {
                WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(true);
            }
        }
        else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            powerPrefs.edit().putBoolean("was_playing_music", audioManager.isMusicActive()).apply();


            sendMediaEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, context);
            if (toggleScreen) {
                toggleScreen(false, powerPrefs, context);
            }
            if (toggleWifi) {
                WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                powerPrefs.edit().putBoolean("was_wifi_on", wifiManager.isWifiEnabled()).apply();
                wifiManager.setWifiEnabled(false);
            }
        }
    }

    private void toggleScreen(boolean state, SharedPreferences powerPrefs, Context context) {
        boolean canChangeSettings = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context);
        int defaultScreenTimeout = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 60000);

        if (canChangeSettings) {
            PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

            if (state) {
                int screenTimeout = powerPrefs.getInt(Settings.System.SCREEN_OFF_TIMEOUT, defaultScreenTimeout);
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, screenTimeout);
                PowerManager.WakeLock wl = powerManager.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Cardroid");
                wl.acquire();
            }
            else {
                powerPrefs.edit().putInt(Settings.System.SCREEN_OFF_TIMEOUT, defaultScreenTimeout).apply();
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1000);
            }
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent permissionIntent = new Intent(context, SettingsActivity.class);
            permissionIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.PowerPreferenceFragment.class.getName());
            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(permissionIntent);
        }
    }

    private void sendMediaEvent(int keyCode, Context context) {
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

        KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        audioManager.dispatchMediaKeyEvent(downEvent);

        KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        audioManager.dispatchMediaKeyEvent(upEvent);
    }
}
