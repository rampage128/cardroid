package de.jlab.cardroid.gps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.FeatureObserver;
import de.jlab.cardroid.devices.serial.gps.GpsPosition;
import de.jlab.cardroid.service.FeatureService;

/// Class responsible for providing a mock location to the system
public class GpsService extends FeatureService implements FeatureObserver<GpsObservable> {

    private LocationManager locationManager;
    private GpsObservable.PositionListener positionListener = null;
    private ArrayList<GpsObservable> gpsSources = new ArrayList<>();
    private GpsObservable currentGpsSource = null;

    @Override
    public void onCreate() {
        super.onCreate();
        this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (this.locationManager != null && checkGpsPermissions(this)) {
            try {
                // TODO baud rate settings have to be handled on a per device basis in the future.
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
                //String baudRateString = prefs.getString("gps_baud_rate", String.valueOf(this.getDefaultBaudrate()));
                //this.setBaudRate(baudRateString != null ? Integer.valueOf(baudRateString) : this.getDefaultBaudrate());
                this.locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 0, 0);
                this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            } catch (SecurityException e) {
                Log.e(this.getClass().getSimpleName(), "Error adding mock location provider. No permission!", e);
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setTitle(this.getString(R.string.gps_permission_title))
                        .setIcon(R.mipmap.ic_launcher)
                        .setMessage(this.getString(R.string.gps_permission_message))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent actionIntent = new Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
                            actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            this.startActivity(actionIntent);
                        })
                        .create();
                Window window = alertDialog.getWindow();
                if (window != null) {
                    alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                }
                alertDialog.show();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (this.checkGpsPermissions(this) && this.locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            try {
                this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                this.locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
                this.locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
                this.locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } catch (IllegalArgumentException e) {
                Log.e(this.getClass().getSimpleName(), "Error disabling GPS provider", e);
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onDeviceServiceConnected(DeviceService.DeviceServiceBinder service) {
        service.subscribe(this, GpsObservable.class);
    }

    @Override
    protected void onDeviceServiceDisconnected() {
        this.stopSelf();
    }

    @Override
    public void onFeatureAvailable(@NonNull GpsObservable feature) {
        // TODO: we should filter per device (User preferences, default device)
        // For now, we use the tactic of "last connected, wins"
        if (this.currentGpsSource != null) {
            this.currentGpsSource.removeListener(this.positionListener);
        }
        this.positionListener = new GpsObservable.PositionListener() {
            @Override
            public void onUpdate(GpsPosition position, String sentence) {
                if (position.hasValidLocation()) {
                    // FIXME: this can cause a crash if fine location permission is not granted
                    GpsService.this.locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, position.getLocation());
                    GpsService.this.locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
                }
            }
        };
        feature.addListener(this.positionListener);
        this.gpsSources.add(feature);
    }

    @Override
    public void onFeatureUnavailable(@NonNull GpsObservable feature) {
        if (this.gpsSources.contains(feature)) {
            this.gpsSources.remove(feature);
        }
        if (this.gpsSources.size() > 0) {
            GpsObservable nextFeature = this.gpsSources.remove(this.gpsSources.size() - 1);
            onFeatureAvailable(nextFeature);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean checkGpsPermissions(@NonNull GpsService service) {
        return ContextCompat.checkSelfPermission(service, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}
