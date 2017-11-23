package de.jlab.cardroid.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import de.jlab.cardroid.MainService;

public class UsbConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Bundle bundle = intent.getExtras();
        UsbDevice device = bundle.getParcelable(UsbManager.EXTRA_DEVICE);

        if (device != null) {
            if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                connectDevice(device, context);
            }
            else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                disconnectDevice(device, context);
            }
        }
    }


    private void connectDevice(UsbDevice device, Context context) {
        int deviceVID = device.getVendorId();
        int devicePID = device.getProductId();

        // carduino
        if (deviceVID == 0x1a86 && devicePID == 0x7523) {
            Intent actionIntent = new Intent();
            actionIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
            actionIntent.setClass(context, MainService.class);
            context.startService(actionIntent);
        }
    }

    private void disconnectDevice(UsbDevice device, Context context) {
        int deviceVID = device.getVendorId();
        int devicePID = device.getProductId();

        // carduino
        if (deviceVID == 0x1a86 && devicePID == 0x7523) {
            context.stopService(new Intent(context, MainService.class));
        }
    }
}
