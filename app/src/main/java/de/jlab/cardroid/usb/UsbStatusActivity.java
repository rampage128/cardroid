package de.jlab.cardroid.usb;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.jlab.cardroid.usb.carduino.CarduinoService;
import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.gps.GpsService;

public class UsbStatusActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION  = "de.jlab.cardroid.USB_PERMISSION";
    private static final String LOG_TAG = "UsbStatus";

    private static final int PERMISSION_GPS = 1;

    private TextView statusText;

    private final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if(device != null){
                        Intent actionIntent = UsbStatusActivity.this.createServiceIntent(device, UsbManager.ACTION_USB_DEVICE_ATTACHED);
                        if (actionIntent != null) {
                            UsbStatusActivity.this.startService(actionIntent);
                        }
                        UsbStatusActivity.this.finish();
                    }
                }
                else {
                    Log.d(LOG_TAG, "permission denied for device " + device);
                }
            }
        }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_status);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        this.statusText = (TextView)this.findViewById(R.id.statusText);
        this.setFinishOnTouchOutside(false);
        handleEvents(this.getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleEvents(intent);
    }

    private void handleEvents(Intent intent) {
        synchronized (this) {
            if (intent != null) {
                Intent actionIntent = null;
                String action = intent.getAction();
                Bundle bundle = intent.getExtras();
                UsbDevice device = bundle.getParcelable(UsbManager.EXTRA_DEVICE);

                if (device != null) {
                    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
                        if (usbManager.hasPermission(device)) {
                            actionIntent = this.createServiceIntent(device, action);
                        } else {
                            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                            this.getApplicationContext().registerReceiver(this.permissionReceiver, filter);
                            usbManager.requestPermission(device, permissionIntent);
                        }
                    } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                        actionIntent = this.createServiceIntent(device, action);
                    }
                }

                if (actionIntent != null) {
                    this.startService(actionIntent);
                    UsbStatusActivity.this.finish();
                }
            }
        }
    }

    private Intent createServiceIntent(UsbDevice device, String action) {
        Class<? extends Service> serviceClass = getServiceClassFromDevice(device);
        if (serviceClass == null) {
            return null;
        }

        Intent actionIntent = new Intent(this, serviceClass);
        actionIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
        actionIntent.setAction(action);
        return actionIntent;
    }

    private Class<? extends Service> getServiceClassFromDevice(UsbDevice device) {
        // TODO add real device detection logic
        int deviceVID = device.getVendorId();
        int devicePID = device.getProductId();
        // carduino
        if (deviceVID == 0x1a86 && devicePID == 0x7523 || deviceVID == 0x16C0 && devicePID == 0x0487) {
            return CarduinoService.class;
        }
        // GPS
        else if (deviceVID == 0x067B && devicePID == 0x2303) {
            if (checkGpsPermissions()) {
                return GpsService.class;
            }
        }
        return null;
    }

    private boolean checkGpsPermissions() {
        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_GPS);
        }
        return hasPermission;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_GPS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Intentionally left blank
                }
                this.finish();
                return;
            }
        }
    }
}
