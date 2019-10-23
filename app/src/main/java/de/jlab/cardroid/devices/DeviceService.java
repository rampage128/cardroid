package de.jlab.cardroid.devices;

import android.app.Service;
import android.bluetooth.BluetoothClass;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.usb.UsbDeviceDetector;
import de.jlab.cardroid.devices.usb.UsbDeviceIdentificationTask;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceDetector;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoSerialMatcher;
import de.jlab.cardroid.devices.usb.serial.gps.GpsSerialMatcher;


public final class DeviceService extends Service {

    private DeviceServiceBinder binder = new DeviceServiceBinder();
    private UsbDeviceIdentificationTask deviceIdentificationTask;
    private DeviceController deviceController = new DeviceController();
    private Timer timer = new Timer();
    private TimerTask disposalTask;
    private DeviceObserver observer = new DeviceObserver();

    @Override
    public void onCreate() {
        super.onCreate();
        this.deviceIdentificationTask = new UsbDeviceIdentificationTask(
                this,
                new UsbDeviceDetectionObserver(),
                new UsbSerialDeviceDetector(
                        new CarduinoSerialMatcher(),
                        new GpsSerialMatcher()
                ));
        Log.e(this.getClass().getSimpleName(), "SERVICE CREATED");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this) {
            this.cancelDisposal();
            if (intent != null) {
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    this.usbDeviceAttached(device);
                }
                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    this.usbDeviceDetached(device);
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(this.getClass().getSimpleName(), "SERVICE DESTROYED");
    }

    private void usbDeviceAttached(@NonNull UsbDevice device) {
        Log.e(this.getClass().getSimpleName(), "Device attached " + device.getDeviceId());

        this.deviceIdentificationTask.identify(device);
    }

    private void usbDeviceDetached(@NonNull UsbDevice usbDevice) {
        Log.e(this.getClass().getSimpleName(), "Device detached " + usbDevice.getDeviceId());
        DeviceHandler device = this.deviceController.remove(DeviceConnectionId.fromUsbDevice(usbDevice));
        if (device != null) {
            device.close();
        }
    }

    private void cancelDisposal() {
        if (this.disposalTask != null) {
            this.disposalTask.cancel();
        }
    }

    private void disposeIfEmpty() {
        this.disposalTask = new TimerTask() {
            @Override
            public void run() {
                if (DeviceService.this.deviceController.isEmpty()) {
                    DeviceService.this.stopSelf();
                }
            }
        };
        this.timer.purge();
        this.timer.schedule(this.disposalTask, 20000);
    }

    private void deviceDetected(@NonNull DeviceHandler device) {
        device.addObserver(this.observer);
        deviceController.add(device);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public class DeviceServiceBinder extends Binder {

        @Nullable
        public DeviceHandler getDevice(@NonNull DeviceUid uid) {
            return DeviceService.this.deviceController.get(uid);
        }

        public void subscribeToFeatures(@NonNull DeviceController.FeatureObserver observer) {
            DeviceService.this.deviceController.subscribe(observer);
        }

        public void unsubscribeFromFeatures(@NonNull DeviceController.FeatureObserver observer) {
            DeviceService.this.deviceController.unsubscribe(observer);
        }
    }

    // FIXME: no need to be a device observer for all devices just for monitoring service lifecycle
    // This should be communicated back to the service back from the DeviceController
    private class DeviceObserver implements DeviceHandler.Observer {

        @Override
        public void onStateChange(@NonNull DeviceHandler device, @NonNull DeviceHandler.State state, @NonNull DeviceHandler.State previous) {
            DeviceService.this.disposeIfEmpty();
        }

        @Override
        public void onFeatureAvailable(@NonNull Feature feature) {}

        @Override
        public void onFeatureUnavailable(@NonNull Feature feature) {}
    }

    private class UsbDeviceDetectionObserver implements UsbDeviceDetector.DetectionObserver {
        @Override
        public void deviceDetected(@NonNull DeviceHandler device) {
            DeviceService.this.deviceDetected(device);
        }

        @Override
        public void detectionFailed() {
            // TODO: notify user if the attached device is not supported
            Log.e(this.getClass().getSimpleName(), "Detection of device failed!");
            DeviceService.this.disposeIfEmpty();
        }
    };


}
