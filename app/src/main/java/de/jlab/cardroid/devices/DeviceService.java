package de.jlab.cardroid.devices;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.usb.UsbDeviceDetector;
import de.jlab.cardroid.devices.usb.UsbDeviceIdentificationTask;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDeviceDetector;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDeviceDetector;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDeviceHandler;
import de.jlab.cardroid.gps.GpsDataProvider;
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

    @Override
    public void onCreate() {
        super.onCreate();

        this.variableStore = new VariableStore();

        try {
            this.ruleHandler = new getRulesTask(getApplication()).execute().get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(this.getClass().getSimpleName(), "Error creating RuleHandler", e);
        }

        this.deviceIdentificationTask = new UsbDeviceIdentificationTask(
                this,
                new UsbDeviceDetectionObserver(),
                new GpsUsbDeviceDetector(),
                new CarduinoUsbDeviceDetector()
                );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this) {
            if (intent != null) {
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    this.usbDeviceAttached(device);
                    this.disposeIfEmpty();
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

        this.variableStore.dispose();
        this.ruleHandler = null;
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
        if (provider != null && type.isInstance(provider)) {
            return type.cast(provider);
        }
        return null;
    }

    @NonNull
    public VariableStore getVariableStore() {
        return this.variableStore;
    }

    private void usbDeviceAttached(@NonNull UsbDevice device) {
        this.deviceIdentificationTask.identify(device);
    }

    private void usbDeviceDetached(@NonNull UsbDevice device) {
        int deviceId = device.getDeviceId();
        DeviceHandler handler = this.devices.get(deviceId);
        if (handler != null) {
            handler.disconnectDevice();
            this.stopDataProvider(handler);
        }
        this.devices.remove(deviceId);
    }

    private void disposeIfEmpty() {
        this.stopSelf();
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
            // TODO implement overlay functions
        }

        public void hideOverlay() {
            // TODO implement overlay functions
        }
    }

    private class UsbDeviceDetectionObserver implements UsbDeviceDetector.DetectionObserver {
        @Override
        public void deviceDetected(@NonNull DeviceHandler device) {
            if (device.connectDevice()) {
                DeviceService.this.devices.put(device.getDeviceId(), device);
                DeviceService.this.updateDataProvider(device);
            }
        }

        @Override
        public void detectionFailed() {
            // TODO notify user if the attached device is not supported
        }
    };

    // TODO: Rules are not part of devices. This should find it's own place
    private static class getRulesTask extends AsyncTask<Void, Void, RuleHandler> {

        private Application application;

        public getRulesTask(Application application) {
            this.application = application;
        }

        @Override
        protected RuleHandler doInBackground(Void... voids) {
            EventRepository eventRepo = new EventRepository(this.application);
            List<RuleDefinition> rules = eventRepo.getAllRules();

            RuleHandler ruleHandler = new RuleHandler(this.application);
            ruleHandler.updateRuleDefinitions(rules);
            return ruleHandler;
        }
    }

}
