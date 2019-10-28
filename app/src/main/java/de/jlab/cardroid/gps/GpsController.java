package de.jlab.cardroid.gps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceController;
import de.jlab.cardroid.devices.FeatureFilter;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.serial.gps.GpsPosition;

/// Class responsible for providing a mock location to the system
public class GpsController {

    private Context context;
    private DeviceController deviceController;
    private LocationManager locationManager;
    private FeatureFilter<GpsObservable> gpsFilter = new FeatureFilter<>(GpsObservable.class, null, this::onFeatureAvailable, this::onFeatureUnavailable);
    private GpsObservable.PositionListener positionListener = this::updatePosition;

    public GpsController(@NonNull DeviceController deviceController, @NonNull Context context) {
        this.deviceController = deviceController;
        this.context = context;

        this.deviceController.addSubscriber(this.gpsFilter, GpsObservable.class);

        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (this.locationManager != null && checkGpsPermissions()) {
            try {
                // TODO baud rate settings have to be handled on a per device basis in the future.
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
                //String baudRateString = prefs.getString("gps_baud_rate", String.valueOf(this.getDefaultBaudrate()));
                //this.setBaudRate(baudRateString != null ? Integer.valueOf(baudRateString) : this.getDefaultBaudrate());
                this.locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 0, 0);
                this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            } catch (SecurityException e) {
                Log.e(this.getClass().getSimpleName(), "Error adding mock location provider. No permission!", e);
                AlertDialog alertDialog = new AlertDialog.Builder(this.context)
                        .setTitle(this.context.getString(R.string.gps_permission_title))
                        .setIcon(R.mipmap.ic_launcher)
                        .setMessage(this.context.getString(R.string.gps_permission_message))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent actionIntent = new Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
                            actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            this.context.startActivity(actionIntent);
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

    public void dispose() {
        if (this.checkGpsPermissions() && this.locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            try {
                this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                this.locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
                this.locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
                this.locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } catch (IllegalArgumentException e) {
                Log.e(this.getClass().getSimpleName(), "Error disabling GPS provider", e);
            }
        }

        this.deviceController.removeSubscriber(this.gpsFilter);
    }

    private void updatePosition(@NonNull GpsPosition position, String sentence) {
        if (position.hasValidLocation()) {
            // FIXME: this can cause a crash if fine location permission is not granted
            GpsController.this.locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, position.getLocation());
            GpsController.this.locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
        }
    }

    private void onFeatureAvailable(@NonNull GpsObservable feature) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        String deviceUid = prefs.getString("gps_device_uid", null);
        if (deviceUid != null && feature.getDevice() != null && feature.getDevice().isDevice(new DeviceUid(deviceUid))) {
            feature.addListener(this.positionListener);
        }
    }

    private void onFeatureUnavailable(@NonNull GpsObservable feature) {
        feature.removeListener(this.positionListener);
    }

    private boolean checkGpsPermissions() {
        return ContextCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}
