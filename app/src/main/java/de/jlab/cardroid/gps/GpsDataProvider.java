package de.jlab.cardroid.gps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.gps.GpsPosition;
import de.jlab.cardroid.devices.serial.gps.GpsPositionParser;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDeviceHandler;


public final class GpsDataProvider extends DeviceDataProvider {

    private LocationManager locationManager;
    private ArrayList<GpsPositionParser.PositionListener> externalListeners = new ArrayList<>();

    private GpsPositionParser.PositionListener positionListener = (position, packet) -> {
        updatePosition(position);
        for (int i = 0; i < this.externalListeners.size(); i++) {
            this.externalListeners.get(i).onUpdate(position, packet);
        }
    };

    public GpsDataProvider(@NonNull DeviceService service) {
        super(service);
    }

    public void addExternalListener(@NonNull GpsPositionParser.PositionListener externalListener) {
        this.externalListeners.add(externalListener);
    }

    public void removeExternalListener(@NonNull GpsPositionParser.PositionListener externalListener) {
        this.externalListeners.remove(externalListener);
    }

    private void deviceAdded(@NonNull DeviceHandler device) {
        GpsObservable observable = device.getObservable(GpsObservable.class);
        if (observable != null) {
            observable.addPositionListener(this.positionListener);
        }
    }

    private void deviceRemoved(@NonNull DeviceHandler device) {
        GpsObservable observable = device.getObservable(GpsObservable.class);
        if (observable != null) {
            observable.removePositionListener(this.positionListener);
        }
    }

    @Override
    protected void onUpdate(@NonNull DeviceHandler previousDevice, @NonNull DeviceHandler newDevice, @NonNull DeviceService service) {
        this.deviceRemoved(previousDevice);
        this.deviceAdded(newDevice);
    }

    @Override
    protected void onStop(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        this.deviceRemoved(device);
        if (this.getConnectedDeviceCount() < 1) {
            if (this.checkGpsPermissions(service) && this.locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
                this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                this.locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
                this.locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
                this.locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        }
    }

    @Override
    protected void onStart(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        if (this.getConnectedDeviceCount() == 1) {
            this.locationManager = (LocationManager) service.getSystemService(Context.LOCATION_SERVICE);

            if (this.locationManager != null && checkGpsPermissions(service)) {
                try {
                    // TODO baud rate settings have to be handled on a per device basis in the future.
                    //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
                    //String baudRateString = prefs.getString("gps_baud_rate", String.valueOf(this.getDefaultBaudrate()));
                    //this.setBaudRate(baudRateString != null ? Integer.valueOf(baudRateString) : this.getDefaultBaudrate());
                    this.locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 0, 0);
                    this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
                } catch (SecurityException e) {
                    Log.e(this.getClass().getSimpleName(), "Error adding mock location provider. No permission!", e);
                    AlertDialog alertDialog = new AlertDialog.Builder(service)
                            .setTitle(service.getString(R.string.gps_permission_title))
                            .setIcon(R.mipmap.ic_launcher)
                            .setMessage(service.getString(R.string.gps_permission_message))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                Intent actionIntent = new Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
                                actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                service.startActivity(actionIntent);
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
        this.deviceAdded(device);
    }

    private void updatePosition(@NonNull GpsPosition position) {
        if (position.hasValidLocation()) {
            GpsDataProvider.this.locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, position.getLocation());
            GpsDataProvider.this.locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
        }
    }

    private boolean checkGpsPermissions(@NonNull DeviceService service) {
        return ContextCompat.checkSelfPermission(service, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}
