package de.jlab.cardroid.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import de.jlab.cardroid.MainService;
import de.jlab.cardroid.R;

public class UsbStatusActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION  = "de.jlab.cardroid.USB_PERMISSION";
    private static final String LOG_TAG = "UsbStatus";

    private TextView statusText;

    private final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            connectDevice(device);
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
        handleEvents();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleEvents();
    }

    private void handleEvents() {
        Intent intent = this.getIntent();

        if (intent != null) {
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();
            UsbDevice device = bundle.getParcelable(UsbManager.EXTRA_DEVICE);

            if (device != null) {
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
                    if (usbManager.hasPermission(device)) {
                        connectDevice(device);
                    }
                    else {
                        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                        this.getApplicationContext().registerReceiver(this.permissionReceiver, filter);
                        usbManager.requestPermission(device, permissionIntent);
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    disconnectDevice(device);
                }
            }

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    UsbStatusActivity.this.finish();
                }
            }, 3000);
        }
    }

    private void connectDevice(UsbDevice device) {
        this.statusText.setText(R.string.usb_status_connecting);

        int deviceVID = device.getVendorId();
        int devicePID = device.getProductId();

        // carduino
        if (deviceVID == 0x1a86 && devicePID == 0x7523) {
            Intent actionIntent = new Intent();
            actionIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
            actionIntent.setClass(this, MainService.class);
            this.startService(actionIntent);
        }
    }

    private void disconnectDevice(UsbDevice device) {
        this.statusText.setText(R.string.usb_status_disconnecting);

        int deviceVID = device.getVendorId();
        int devicePID = device.getProductId();

        // carduino
        if (deviceVID == 0x1a86 && devicePID == 0x7523) {
            this.stopService(new Intent(this, MainService.class));
        }
    }
}
