package de.jlab.cardroid.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

public final class DeviceDisconnectedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
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
        Intent intent = new Intent(context, DeviceConnectionActivity.class);
        intent.putExtra(UsbManager.EXTRA_DEVICE, device);
        intent.setAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
