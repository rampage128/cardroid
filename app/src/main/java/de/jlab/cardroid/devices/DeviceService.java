package de.jlab.cardroid.devices;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.car.CanReader;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.usb.UsbDeviceDetector;
import de.jlab.cardroid.devices.usb.UsbDeviceIdentificationTask;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceDetector;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoSerialMatcher;
import de.jlab.cardroid.devices.usb.serial.gps.GpsSerialMatcher;
import de.jlab.cardroid.errors.ErrorService;
import de.jlab.cardroid.gps.GpsService;
import de.jlab.cardroid.overlay.OverlayWindow;
import de.jlab.cardroid.rules.RuleService;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.VariableStore;

//TODO: should this be renamed to "MainService"?
public final class DeviceService extends Service {

    private Handler uiHandler;

    // Common storage/handlers/controllers
    private DeviceController deviceController;
    private VariableStore variableStore;
    private ScriptEngine scriptEngine;
    private CanReader canReader;
    private OverlayWindow overlay;
    private RuleService ruleService;
    private ErrorService errorService;
    private GpsService gpsService;

    private DeviceServiceBinder binder = new DeviceServiceBinder();
    private UsbDeviceIdentificationTask deviceIdentificationTask;
    private Timer timer = new Timer();
    private TimerTask disposalTask;
    private DeviceObserver observer = new DeviceObserver();
    private ArrayList<DeviceHandler.Observer> externalObservers = new ArrayList<>();


    @Override
    public void onCreate() {
        super.onCreate();

        this.uiHandler = new Handler();

        // Create device identification task for newly attached devices
        this.deviceIdentificationTask = new UsbDeviceIdentificationTask(
                this,
                new UsbDeviceDetectionObserver(),
                new UsbSerialDeviceDetector(
                        new CarduinoSerialMatcher(),
                        new GpsSerialMatcher()
                ));

        // Initialize common storage/handlers/controllers
        this.deviceController = new DeviceController();
        this.variableStore = new VariableStore();
        this.scriptEngine = new ScriptEngine();
        this.canReader = new CanReader(this.deviceController, this.variableStore, this.scriptEngine);
        this.overlay = new OverlayWindow(this.deviceController, this.variableStore, this);
        this.ruleService = new RuleService(this.deviceController, this.getApplication());
        this.errorService = new ErrorService(this.deviceController, this);
        this.gpsService = new GpsService(this.deviceController, this);

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

        this.canReader.dispose();
        this.variableStore.dispose();
        this.ruleService.dispose();
        this.errorService.dispose();
        this.gpsService.dispose();

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

    private void runOnUiThread(Runnable runnable) {
        this.uiHandler.post(runnable);
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

        public <FT extends Feature> void subscribe(@NonNull FeatureObserver<FT> observer, Class<FT> featureClass) {
            DeviceService.this.deviceController.addSubscriber(observer, featureClass);
        }

        public <FT extends Feature> void unsubscribe(@NonNull FeatureObserver<FT> observer, Class<FT> featureClass) {
            DeviceService.this.deviceController.addSubscriber(observer, featureClass);
        }

        public void addExternalDeviceObserver(DeviceHandler.Observer observer) {
            DeviceService.this.externalObservers.add(observer);
        }

        public void removeExternalDeviceObserver(DeviceHandler.Observer observer) {
            DeviceService.this.externalObservers.remove(observer);
        }

        public VariableStore getVariableStore() {
            return DeviceService.this.variableStore;
        }

        public OverlayWindow getOverlay() {
            return DeviceService.this.overlay;
        }
    }

    private class DeviceObserver implements DeviceHandler.Observer {

        @Override
        public void onStateChange(@NonNull DeviceHandler device, @NonNull DeviceHandler.State state, @NonNull DeviceHandler.State previous) {
            DeviceService.this.disposeIfEmpty();
            for (int i = 0; i < DeviceService.this.externalObservers.size(); i++) {
                DeviceService.this.externalObservers.get(i).onStateChange(device, state, previous);
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DeviceService.this);
            String deviceUid = prefs.getString("overlay_device_uid", null);
            if (deviceUid != null && device.isDevice(new DeviceUid(deviceUid))) {
                DeviceService.this.runOnUiThread(() -> {
                    if (state == DeviceHandler.State.READY) {
                        DeviceService.this.overlay.create();
                    } else if (state == DeviceHandler.State.INVALID) {
                        DeviceService.this.overlay.destroy();
                    }
                });
            }
        }

        @Override
        public void onFeatureAvailable(@NonNull Feature feature) {
            for (int i = 0; i < DeviceService.this.externalObservers.size(); i++) {
                DeviceService.this.externalObservers.get(i).onFeatureAvailable(feature);
            }
        }

        @Override
        public void onFeatureUnavailable(@NonNull Feature feature) {
            for (int i = 0; i < DeviceService.this.externalObservers.size(); i++) {
                DeviceService.this.externalObservers.get(i).onFeatureUnavailable(feature);
            }
        }
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
