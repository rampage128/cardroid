package de.jlab.cardroid.devices;

import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.usb.UsbDeviceDetector;
import de.jlab.cardroid.devices.usb.UsbDeviceIdentificationTask;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoLegacyDeviceHandler;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDeviceDetector;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDeviceDetector;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDeviceHandler;
import de.jlab.cardroid.gps.GpsDataProvider;
import de.jlab.cardroid.overlay.OverlayWindow;
import de.jlab.cardroid.rules.RuleHandler;
import de.jlab.cardroid.rules.storage.EventRepository;
import de.jlab.cardroid.rules.storage.RuleDefinition;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.VariableStore;


public final class DeviceService extends Service {

    private DeviceServiceBinder binder = new DeviceServiceBinder();
    private UsbDeviceIdentificationTask deviceIdentificationTask;

    private SparseArray<DeviceHandler> devices = new SparseArray<>();

    private HashMap<Class<? extends DeviceDataProvider>, DeviceDataProvider> dataProviders = new HashMap<>();

    private RuleHandler ruleHandler;
    private VariableStore variableStore;
    private ScriptEngine scriptEngine = new ScriptEngine();
    private OverlayWindow overlay;

    private Timer timer = new Timer();

    private TimerTask disposalTask;
    private Handler uiHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        this.uiHandler = new Handler();

        this.overlay = new OverlayWindow(this);
        this.variableStore = new VariableStore();

        try {
            this.ruleHandler = new getRulesTask().execute(this).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(this.getClass().getSimpleName(), "Error creating RuleHandler", e);
        }

        this.deviceIdentificationTask = new UsbDeviceIdentificationTask(
                this,
                new UsbDeviceDetectionObserver(),
                new GpsUsbDeviceDetector(),
                new CarduinoUsbDeviceDetector()
                );

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
                    this.disposeIfEmpty();
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.overlay.destroy();
        this.variableStore.dispose();
        this.variableStore = null;
        this.ruleHandler.dispose();
        this.ruleHandler = null;

        Log.e(this.getClass().getSimpleName(), "SERVICE DESTROYED");
    }

    @NonNull
    public ScriptEngine getScriptEngine() {
        return this.scriptEngine;
    }

    @NonNull
    public RuleHandler getRuleHandler() {
        return this.ruleHandler;
    }

    @Nullable
    public <ProviderType extends DeviceDataProvider> ProviderType getDeviceProvider(@NonNull Class<ProviderType> type) {
        DeviceDataProvider provider = DeviceService.this.dataProviders.get(type);
        if (provider == null) {
            provider = DeviceDataProvider.createFrom(type, this);
            DeviceService.this.dataProviders.put(type, provider);
        }
        if (type.isInstance(provider)) {
            return type.cast(provider);
        }
        return null;
    }

    @NonNull
    public VariableStore getVariableStore() {
        return this.variableStore;
    }

    public void showOverlay() {
        this.runOnUiThread(() -> this.overlay.create());
    }

    public void hideOverlay() {
        this.runOnUiThread(() -> this.overlay.destroy());
    }

    private void usbDeviceAttached(@NonNull UsbDevice device) {
        Log.e(this.getClass().getSimpleName(), "Device attached " + device.getDeviceId());

        // FIXME: skipping device identification for testing purposes. Change this back for production
        //this.deviceIdentificationTask.identify(device);
        CarduinoLegacyDeviceHandler handler = new CarduinoLegacyDeviceHandler(device, 115200, this);
        handler.connectDevice();
        this.devices.put(device.getDeviceId(), handler);
        this.updateDataProvider(handler);
    }

    private void usbDeviceDetached(@NonNull UsbDevice device) {
        Log.e(this.getClass().getSimpleName(), "Device detached " + device.getDeviceId());
        int deviceId = device.getDeviceId();
        DeviceHandler handler = this.devices.get(deviceId);
        if (handler != null) {
            handler.disconnectDevice();
            this.stopDataProvider(handler);
        }
        this.devices.remove(deviceId);
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
                if (DeviceService.this.devices.size() == 0) {
                    DeviceService.this.stopSelf();
                }
            }
        };
        this.timer.purge();
        this.timer.schedule(this.disposalTask, 20000);

    }

    private void runOnUiThread(Runnable runnable) {
        this.uiHandler.post(runnable);
    }

    private void stopDataProvider(DeviceHandler device) {
        DeviceDataProvider provider = null;

        if (device instanceof GpsUsbDeviceHandler) {
            provider = this.dataProviders.get(GpsDataProvider.class);
        }

        if (provider != null) {
            if (provider.usesDevice(device)) {
                provider.stop();
                // TODO: we might want to restart the provider with a different device?
            }
        }
    }

    private void updateDataProvider(DeviceHandler device) {
        Class[] features = device.getFeatures();
        for (Class feature : features) {
            DeviceDataProvider provider = this.dataProviders.get(feature);
            if (provider == null) {
                provider = DeviceDataProvider.createFrom(feature, this);
                this.dataProviders.put(provider.getClass(), provider);
            }
            provider.start(device);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public class DeviceServiceBinder extends Binder {
        @Nullable
        public <ProviderType extends DeviceDataProvider> ProviderType getDeviceProvider(@NonNull Class<ProviderType> type) {
            return DeviceService.this.getDeviceProvider(type);
        }

        @NonNull
        public VariableStore getVariableStore() {
            return DeviceService.this.variableStore;
        }

        @NonNull
        public RuleHandler getRuleHandler() {
            return DeviceService.this.ruleHandler;
        }

        public void showOverlay() {
            DeviceService.this.overlay.create();
        }

        public void hideOverlay() {
            DeviceService.this.overlay.destroy();
        }
    }

    private class UsbDeviceDetectionObserver implements UsbDeviceDetector.DetectionObserver {
        @Override
        public void deviceDetected(@NonNull DeviceHandler device) {
            if (device.connectDevice()) {
                Log.e(this.getClass().getSimpleName(), "Device \"" + device.getClass().getSimpleName() + "\" detected!");
                DeviceService.this.devices.put(device.getDeviceId(), device);
                DeviceService.this.updateDataProvider(device);
            } else {
                DeviceService.this.disposeIfEmpty();
            }
        }

        @Override
        public void detectionFailed() {
            // TODO notify user if the attached device is not supported
            Log.e(this.getClass().getSimpleName(), "Detection of device failed!");
            DeviceService.this.disposeIfEmpty();
        }
    };

    // TODO: Rules are not part of devices. This should find it's own place
    private static class getRulesTask extends AsyncTask<DeviceService, Void, RuleHandler> {
        @Override
        protected RuleHandler doInBackground(DeviceService... services) {
            DeviceService service = services[0];
            EventRepository eventRepo = new EventRepository(service.getApplication());
            List<RuleDefinition> rules = eventRepo.getAllRules();

            RuleHandler ruleHandler = new RuleHandler(service);
            ruleHandler.updateRuleDefinitions(rules);
            return ruleHandler;
        }
    }

}
