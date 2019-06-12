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
import android.view.WindowManager;

import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.SerialConnectionManager;
import de.jlab.cardroid.usb.UsageStatistics;
import de.jlab.cardroid.usb.UsbService;

public class GpsService extends UsbService {
    private LocationManager locationManager;
    private SerialConnectionManager gpsManager;
    private GPSSerialReader gpsReader;

    private GPSSerialReader.PositionListener positionListener = new GPSSerialReader.PositionListener() {
        @Override
        public void onUpdate(GpsPosition position, String rawData) {
            if (position.hasValidLocation()) {
                GpsService.this.locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, position.getLocation());
                GpsService.this.locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        this.gpsReader = new GPSSerialReader();
        this.gpsReader.addPositionListener(this.positionListener);

        this.gpsManager = new SerialConnectionManager(this);
        this.gpsManager.addConnectionListener(gpsReader);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.gpsReader.removePositionListener(this.positionListener);
        this.gpsManager.removeConnectionListener(this.gpsReader);
    }

    @Override
    protected boolean isConnected() {
        return this.gpsManager != null && this.gpsManager.isConnected();
    }

    @Override
    protected boolean isConnected(UsbDevice device) {
        return this.gpsManager != null && this.gpsManager.isConnected(device);
    }

    protected boolean connectDevice(UsbDevice device) {
        if (this.locationManager != null) {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                int baudRate = Integer.valueOf(prefs.getString("gps_baud_rate", "4800"));
                this.locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 0, 0);
                this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
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
        this.gpsManager.disconnect();
        if (this.locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            this.locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
            this.locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
            this.locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
            this.locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        }
    }

    public class GpsServiceBinder extends UsbServiceBinder {
        public void addPositionListener(GPSSerialReader.PositionListener listener) {
            GpsService.this.gpsReader.addPositionListener(listener);
        }

        public void removePositionListener(GPSSerialReader.PositionListener listener) {
            GpsService.this.gpsReader.addPositionListener(listener);
        }

        public void addBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            GpsService.this.gpsManager.addBandwidthStatisticsListener(listener);
        }

        public void removeBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            GpsService.this.gpsManager.removeBandwidthStatisticsListener(listener);
        }

        public void addSentenceStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            GpsService.this.gpsReader.addSentenceStatisticListener(listener);
        }

        public void removeSentenceStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            GpsService.this.gpsReader.removeSentenceStatisticListener(listener);
        }

        public void addUpdateStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            GpsService.this.gpsReader.addUpdateStatisticListener(listener);
        }

        public void removeUpdateStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
            GpsService.this.gpsReader.removeUpdateStatisticListener(listener);
        }

        public int getBaudRate() {
            return GpsService.this.gpsManager.getBaudRate();
        }
    }

    private final IBinder binder = new GpsServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }
}
