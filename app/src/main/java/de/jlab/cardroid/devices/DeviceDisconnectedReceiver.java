package de.jlab.cardroid.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

public final class DeviceDisconnectedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(this.getClass().getSimpleName(), "Device detached!");
        String action = intent.getAction();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            UsbDevice device = bundle.getParcelable(UsbManager.EXTRA_DEVICE);

            if (device != null && UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                disconnectDevice(device, context);
            }
        }
    }

    private void disconnectDevice(UsbDevice device, Context context) {
        /* Broadcastreceiver can not start background service if app has no visible activities
        Intent actionIntent = new Intent(context.getApplicationContext(), DeviceService.class);
        actionIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
        actionIntent.setAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.getApplicationContext().startService(actionIntent);
         */

        Intent intent = new Intent(context.getApplicationContext(), DeviceConnectionActivity.class);
        intent.putExtra(UsbManager.EXTRA_DEVICE, device);
        intent.setAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.getApplicationContext().startActivity(intent);
    }
}
