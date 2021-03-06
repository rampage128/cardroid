package de.jlab.cardroid.devices.detection;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceConnectionRequest;
import de.jlab.cardroid.devices.DeviceService;

public final class UsbSerialDeviceDetector extends UsbDeviceDetector {

    @Override
    protected boolean startIdentification(@NonNull UsbDevice device, @NonNull DeviceService service) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service);
        long delay = Long.parseLong(prefs.getString("device_detection_delay", "1000"));
        long timeout = Long.parseLong(prefs.getString("device_detection_timeout", "1000"));

        UsbManager usbManager = (UsbManager)service.getApplication().getSystemService(Context.USB_SERVICE);
        int[] baudRates = service.getResources().getIntArray(R.array.serial_detection_baud_rates);

        UsbSerialDeviceDetectionTask detectionTask = new UsbSerialDeviceDetectionTask(device, service, this);
        detectionTask.detect(usbManager, timeout, baudRates);

        return true;
    }

    protected void deviceDetected(@NonNull DeviceConnectionRequest connectionRequest) {
        Log.e(this.getClass().getSimpleName(), "Serial device detected " + connectionRequest);
        super.deviceDetected(connectionRequest);
    }

    protected void detectionFailed(@NonNull UsbDevice device) {
        Log.e(this.getClass().getSimpleName(), "Serial device detection failed");
        super.detectionFailed(device);
    }

    public interface SerialMatcher {
        @Nullable
        DeviceConnectionRequest detect(@NonNull byte[] data, @NonNull UsbDevice device, int baudRate, @NonNull Application app);

        void clear();
    }

}
