package de.jlab.cardroid.devices;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import de.jlab.cardroid.R;

public final class DeviceConnectionActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION  = "de.jlab.cardroid.USB_PERMISSION";
    private static final String LOG_TAG = "UsbStatus";

    //private static final int PERMISSION_GPS = 1;

    private TextView statusText;

    private final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if(device != null){
                        Intent actionIntent = DeviceConnectionActivity.this.createServiceIntent(device, UsbManager.ACTION_USB_DEVICE_ATTACHED);
                        if (actionIntent != null) {
                            DeviceConnectionActivity.this.getApplicationContext().startService(actionIntent);
                        }
                        DeviceConnectionActivity.this.finish();
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

        this.statusText = this.findViewById(R.id.statusText);
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
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String action = intent.getAction();
                    Intent actionIntent = null;
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
                        this.getApplicationContext().startService(actionIntent);
                        DeviceConnectionActivity.this.finish();
                    }
                }
            }
        }
    }

    private Intent createServiceIntent(UsbDevice device, String action) {
        Intent actionIntent = new Intent(this.getApplicationContext(), DeviceService.class);
        actionIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
        actionIntent.setAction(action);
        return actionIntent;
    }

    /*
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
    */

}
