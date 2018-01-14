package de.jlab.cardroid;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import de.jlab.cardroid.usb.carduino.CarduinoWatchDog;
import de.jlab.cardroid.usb.gps.GpsWatchDog;

public class WatchDogService extends Service {
    private static final String LOG_TAG = "WatchDogService";

    private PowerManager.WakeLock screenLock;

    private TimerTask timerTaskAsync;
    private Timer timerAsync;
    private ArrayList<WatchDog> watchDogs = new ArrayList<>();

    public WatchDogService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.timerAsync = new Timer();
        this.timerTaskAsync = new TimerTask() {
            @Override
            public void run() {
                for (WatchDog watchDog : WatchDogService.this.watchDogs) {
                    Intent intent = watchDog.watch();
                    if (!Objects.equals(intent.getAction(), WatchDog.ACTION_IGNORE)) {
                        Log.i(LOG_TAG, "Triggering watchdog: " + watchDog.getClass().getSimpleName());
                        watchDog.trigger(intent);
                    }
                }
            }
        };
    }

    @SuppressLint("WakelockTimeout")
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.timerAsync.schedule(this.timerTaskAsync, 3000, 5000);
        Log.i(LOG_TAG, this.watchDogs.size() + " watchdogs are watching now.");

        this.watchDogs.clear();
        this.watchDogs.add(new GpsWatchDog(this));
        this.watchDogs.add(new CarduinoWatchDog(this));
        this.watchDogs.add(new WifiWatchDog(this));

        PowerManager powerManager = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            this.screenLock = powerManager.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Cardroid");
            this.screenLock.acquire();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.timerAsync.cancel();
        Log.i(LOG_TAG, this.watchDogs.size() + " watchdogs go to sleep.");

        this.screenLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public interface WatchDog {
        String ACTION_IGNORE = "IGNORE";
        String ACTION_TRIGGER = "TRIGGER";

        Intent INTENT_TRIGGER = new Intent(WatchDogService.WatchDog.ACTION_TRIGGER);
        Intent INTENT_IGNORE = new Intent(WatchDogService.WatchDog.ACTION_IGNORE);

        Intent watch();
        void trigger(Intent intent);
    }

}
