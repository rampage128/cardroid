package de.jlab.cardroid.providers;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import de.jlab.cardroid.devices.FeatureDataProvider;
import de.jlab.cardroid.devices.ObservableFeature;
import de.jlab.cardroid.overlay.OverlayWindow;
import de.jlab.cardroid.rules.RuleHandler;
import de.jlab.cardroid.rules.storage.EventRepository;
import de.jlab.cardroid.rules.storage.RuleDefinition;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.VariableStore;

public class DataProviderService extends Service {

    private HashMap<Class<? extends FeatureDataProvider>, FeatureDataProvider> dataProviders = new HashMap<>();
    private RuleHandler ruleHandler;
    private VariableStore variableStore;
    private ScriptEngine scriptEngine = new ScriptEngine();
    private DataProviderServiceBinder binder = new DataProviderServiceBinder();
    private Handler uiHandler;
    private OverlayWindow overlay;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.uiHandler = new Handler();
        this.overlay = new OverlayWindow(this);
        this.variableStore = new VariableStore();
        try {
            this.ruleHandler = new DataProviderService.getRulesTask().execute(this).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(this.getClass().getSimpleName(), "Error creating RuleHandler", e);
        }
        Log.e(this.getClass().getSimpleName(), "SERVICE CREATED");
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

    private void runOnUiThread(Runnable runnable) {
        this.uiHandler.post(runnable);
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

    @NonNull
    public ScriptEngine getScriptEngine() {
        return this.scriptEngine;
    }

    @NonNull
    public RuleHandler getRuleHandler() {
        return this.ruleHandler;
    }

    @Nullable
    public <ProviderType extends FeatureDataProvider> ProviderType getDeviceProvider(@NonNull Class<ProviderType> type) {
        FeatureDataProvider provider = DataProviderService.this.dataProviders.get(type);
        if (provider == null) {
            provider = FeatureDataProvider.createFrom(type, this);
            DataProviderService.this.dataProviders.put(type, provider);
        }
        if (type.isInstance(provider)) {
            return type.cast(provider);
        }
        return null;
    }

    private void stopDataProvider(@NonNull ObservableFeature<? extends ObservableFeature.Listener> feature) {
        Class<? extends FeatureDataProvider> providerClass = feature.getProviderClass();
        if (providerClass != null) {
            FeatureDataProvider provider = this.getDeviceProvider(providerClass);
            if (provider != null) {
                provider.stop(feature);
            }
        }
    }

    private void startDataProvider(@NonNull ObservableFeature<? extends ObservableFeature.Listener> feature) {
        Class<? extends FeatureDataProvider> providerClass = feature.getProviderClass();
        if (providerClass != null) {
            FeatureDataProvider provider = this.getDeviceProvider(providerClass);
            if (provider != null) {
                provider.start(feature);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public class DataProviderServiceBinder extends Binder {
        @Nullable
        public <ProviderType extends FeatureDataProvider> ProviderType getDeviceProvider(@NonNull Class<ProviderType> type) {
            return DataProviderService.this.getDeviceProvider(type);
        }

        @NonNull
        public VariableStore getVariableStore() {
            return DataProviderService.this.variableStore;
        }

        @NonNull
        public RuleHandler getRuleHandler() {
            return DataProviderService.this.ruleHandler;
        }

        public void showOverlay() {
            DataProviderService.this.overlay.create();
        }

        public void hideOverlay() {
            DataProviderService.this.overlay.destroy();
        }
    }

    // TODO: Rules may be implemented as a service on their own./
    private static class getRulesTask extends AsyncTask<DataProviderService, Void, RuleHandler> {
        @Override
        protected RuleHandler doInBackground(DataProviderService... services) {
            DataProviderService service = services[0];
            EventRepository eventRepo = new EventRepository(service.getApplication());
            List<RuleDefinition> rules = eventRepo.getAllRules();

            RuleHandler ruleHandler = new RuleHandler(service);
            ruleHandler.updateRuleDefinitions(rules);
            return ruleHandler;
        }
    }
}
