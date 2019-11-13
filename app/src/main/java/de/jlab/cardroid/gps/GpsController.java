package de.jlab.cardroid.gps;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceController;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.serial.gps.GpsPosition;
import de.jlab.cardroid.utils.permissions.MockLocationPermission;
import de.jlab.cardroid.utils.permissions.Permission;
import de.jlab.cardroid.utils.permissions.PermissionReceiver;
import de.jlab.cardroid.utils.permissions.PermissionRequest;

/// Class responsible for providing a mock location to the system
public class GpsController {

    private Context context;
    private DeviceController deviceController;
    private LocationManager locationManager;
    private Device.FeatureChangeObserver<GpsObservable> gpsFilter = this::onGpsFeatureStateChange;
    private GpsObservable.PositionListener positionListener = this::updatePosition;

    private boolean shouldBeMockingLocation = false;
    private boolean isMockingLocation = false;

    private PermissionReceiver permissionReceiver;

    public GpsController(@NonNull DeviceController deviceController, @NonNull Context context) {
        this.deviceController = deviceController;
        this.context = context;
        this.permissionReceiver = new PermissionReceiver(context, this.getClass(), this::onLocationPermissionGranted);
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        DeviceUid deviceUid = getDeviceUid(context);
        if (deviceUid != null) {
            this.deviceController.subscribeFeature(this.gpsFilter, GpsObservable.class, deviceUid);
        }
    }

    public void dispose() {
        this.stopMocking();

        this.permissionReceiver.dispose();
        this.deviceController.unsubscribeFeature(this.gpsFilter, GpsObservable.class);
        this.context = null;
    }

    @Nullable
    private static DeviceUid getDeviceUid(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceUid = prefs.getString("gps_device_uid", null);
        return deviceUid != null ? new DeviceUid(deviceUid) : null;
    }

    private void updatePosition(@NonNull GpsPosition position, String sentence) {
        if (this.isMockingLocation && position.hasValidLocation()) {
            GpsController.this.locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, position.getLocation());
            GpsController.this.locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
        }
    }

    private void startMocking() {
        this.shouldBeMockingLocation = true;

        if (this.isMockingLocation) {
            return;
        }

        boolean hasPermission = this.permissionReceiver.requestPermissions(
                this.context,
                new PermissionRequest(Manifest.permission.ACCESS_FINE_LOCATION, Permission.Constraint.REQUIRED, R.string.location_permission_reason),
                new PermissionRequest(MockLocationPermission.PERMISSION_KEY, Permission.Constraint.REQUIRED, R.string.location_permission_reason)
        );

        if (hasPermission) {
            this.locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 0, 0);
            this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            this.isMockingLocation = true;
        }
    }

    private void stopMocking() {
        this.shouldBeMockingLocation = false;

        if (!this.isMockingLocation) {
            return;
        }

        this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
        this.locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
        this.locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
        this.locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        this.isMockingLocation = false;
    }

    private void onLocationPermissionGranted() {
        if (this.shouldBeMockingLocation && !this.isMockingLocation) {
            this.startMocking();
        }
    }

    private void onGpsFeatureStateChange(@NonNull GpsObservable feature, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            this.startMocking();
            feature.addListener(this.positionListener);
        } else {
            feature.removeListener(this.positionListener);
            this.stopMocking();
        }
    }

}
