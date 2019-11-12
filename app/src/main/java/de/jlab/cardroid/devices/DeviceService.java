package de.jlab.cardroid.devices;

import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.car.CanController;
import de.jlab.cardroid.devices.detection.CarduinoSerialMatcher;
import de.jlab.cardroid.devices.detection.GpsSerialMatcher;
import de.jlab.cardroid.devices.detection.UsbDeviceDetectionController;
import de.jlab.cardroid.devices.detection.UsbSerialDeviceDetector;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.errors.ErrorController;
import de.jlab.cardroid.gps.GpsController;
import de.jlab.cardroid.overlay.OverlayController;
import de.jlab.cardroid.rules.RuleController;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.VariableController;

//TODO: should this be renamed to "MainService"?
public final class DeviceService extends Service {

    private Handler uiHandler;

    // Common storage/handlers/controllers
    private DeviceController deviceController;
    private VariableController variableController;
    private ScriptEngine scriptEngine;
    private CanController canController;
    private OverlayController overlayController;
    private RuleController ruleController;
    private ErrorController errorController;
    private GpsController gpsController;
    private UsbDeviceDetectionController detectionController;

    private DeviceServiceBinder binder = new DeviceServiceBinder();
    private Timer timer = new Timer();
    private TimerTask disposalTask;
    private Device.StateObserver deviceStateObserver = this::onDeviceStateChange;


    @Override
    public void onCreate() {
        super.onCreate();

        this.uiHandler = new Handler();

        // Initialize common storage/handlers/controllers
        this.deviceController = new DeviceController();
        this.variableController = new VariableController();
        this.scriptEngine = new ScriptEngine();
        this.canController = new CanController(this.deviceController, this.variableController, this.scriptEngine);
        this.overlayController = new OverlayController(this.variableController, this.deviceController, this);
        this.ruleController = new RuleController(this.deviceController, this.getApplication());
        this.errorController = new ErrorController(this.deviceController, this);
        this.gpsController = new GpsController(this.deviceController, this);

        // Initialize DeviceDetectionController for newly attached devices
        this.detectionController = new UsbDeviceDetectionController(
                this,
                this::deviceDetected,
                this::deviceDetectionFailed,
                new UsbSerialDeviceDetector(
                        new CarduinoSerialMatcher(),
                        new GpsSerialMatcher()
                ));

        this.deviceController.subscribeState(this.deviceStateObserver);

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

        this.canController.dispose();
        this.variableController.dispose();
        this.ruleController.dispose();
        this.errorController.dispose();
        this.gpsController.dispose();
        this.overlayController.dispose();

        this.deviceController.unsubscribeState(this.deviceStateObserver);

        Log.e(this.getClass().getSimpleName(), "SERVICE DESTROYED");
    }

    private void usbDeviceAttached(@NonNull UsbDevice device) {
        Log.e(this.getClass().getSimpleName(), "Device attached " + device.getDeviceId());

        this.detectionController.identify(device);
    }

    private void usbDeviceDetached(@NonNull UsbDevice usbDevice) {
        Log.e(this.getClass().getSimpleName(), "Device detached " + usbDevice.getDeviceId());
        this.deviceController.close(DeviceConnectionId.fromUsbDevice(usbDevice));
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

    private void deviceDetected(@NonNull DeviceConnectionRequest connectionRequest) {
        this.deviceController.add(connectionRequest);
    }

    private void deviceDetectionFailed() {
        Log.w(this.getClass().getSimpleName(), "Detection of device failed!");
        this.disposeIfEmpty();
    }

    private void onDeviceStateChange(@NonNull Device device, @NonNull Device.State state, @NonNull Device.State previous) {
        DeviceService.this.disposeIfEmpty();
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
        public Device getDevice(@NonNull DeviceUid uid) {
            return DeviceService.this.deviceController.get(uid);
        }

        public <FT extends Feature> void subscribeFeature(@NonNull Device.FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass, @NonNull DeviceUid... deviceUids) {
            DeviceService.this.deviceController.subscribeFeature(observer, featureClass, deviceUids);
        }

        public <FT extends Feature> void unsubscribeFeature(@NonNull Device.FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass) {
            DeviceService.this.deviceController.unsubscribeFeature(observer, featureClass);
        }

        public void subscribeDeviceState(@NonNull Device.StateObserver observer, @NonNull DeviceUid... deviceUids) {
            DeviceService.this.deviceController.subscribeState(observer, deviceUids);
        }

        public void unsubscribeDeviceState(@NonNull Device.StateObserver observer) {
            DeviceService.this.deviceController.unsubscribeState(observer);
        }

        public VariableController getVariableStore() {
            return DeviceService.this.variableController;
        }

        public OverlayController getOverlayController() {
            return DeviceService.this.overlayController;
        }
    }

}
