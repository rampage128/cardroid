package de.jlab.cardroid.utils.permissions;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import de.jlab.cardroid.BuildConfig;

public final class MockLocationPermission extends Permission {

    public static final String PERMISSION_KEY = "android.permission.ACCESS_MOCK_LOCATION";

    public MockLocationPermission(@NonNull Constraint constraint, int usage) {
        super(PERMISSION_KEY, constraint, usage);
    }

    @Override
    public boolean isGranted(@NonNull Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AppOpsManager opsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = opsManager.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID);
            return mode == AppOpsManager.MODE_ALLOWED;
        } else {
            // TODO: Find a better way to check if the app is set as mock location provider for android versions prior to M
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            try {
                locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 0, 0);
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
                locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
                return true;
            } catch (SecurityException e) {
                // Ignore all exceptions since we return false
            }
            return false;
        }
    }

    @Override
    public void request(@NonNull Activity activity) {
        Intent actionIntent = new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivityForResult(actionIntent, 0);
    }
}
