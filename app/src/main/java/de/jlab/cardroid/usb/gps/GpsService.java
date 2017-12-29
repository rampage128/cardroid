package de.jlab.cardroid.usb.gps;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.SerialConnectionManager;
import de.jlab.cardroid.usb.UsbService;

public class GpsService extends UsbService {
    private static final String LOG_TAG = "GpsService";

    private LocationManager locationManager;
    private SerialConnectionManager gpsManager;

    private GPSSerialReader.PositionListener positionListener = new GPSSerialReader.PositionListener() {
        @Override
        public void onUpdate(GpsPosition position) {
            if (position.isValid()) {
                GpsService.this.locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, position);
                GpsService.this.locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
            }
        }

        @Override
        public void onError() {
            Log.e(LOG_TAG, "GPS ERROR");
            //gpsManager.reconnect();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "Creating gps service.");

        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    protected boolean isConnected() {
        return this.gpsManager != null && this.gpsManager.isConnected();
    }

    protected boolean connectDevice(UsbDevice device) {
        if (this.locationManager != null) {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                int baudRate = Integer.valueOf(prefs.getString("gps_baud_rate", "4800"));

                this.locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 0, 0);
                this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
                GPSSerialReader gpsReader = new GPSSerialReader();
                gpsReader.addPositionListener(this.positionListener);
                this.gpsManager = new SerialConnectionManager(this);
                this.gpsManager.addConnectionListener(gpsReader);
                this.gpsManager.connect(device, baudRate);
                return true;
            } catch (SecurityException e3) {
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle(getString(R.string.gps_permission_title)).setIcon(R.mipmap.ic_launcher).setMessage(getString(R.string.gps_permission_message)).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent actionIntent = new Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
                        actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        GpsService.this.startActivity(actionIntent);
                    }
                }).create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dialog.show();
            }
        }
        return false;
    }

    protected void disconnectDevice() {
        if (this.gpsManager != null) {
            this.gpsManager.disconnect();
        }
        if (this.locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
            this.locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
            this.locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
            this.locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
