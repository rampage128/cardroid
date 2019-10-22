package de.jlab.cardroid.devices;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.devices.serial.carduino.CarduinoEventProvider;
import de.jlab.cardroid.errors.ErrorDataProvider;
import de.jlab.cardroid.gps.GpsDataProvider;

public abstract class DeviceDataProvider<FT extends ObservableFeature> {

    private DeviceService service;
    private ArrayList<FT> features = new ArrayList<>();
    private Class<FT> featureType;

    public DeviceDataProvider(@NonNull DeviceService service) {
        this.service = service;
        this.featureType = (Class<FT>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public int getFeatureCount() {
        return this.features.size();
    }

    public void start(@NonNull FT feature) {
        if (!this.featureType.isInstance(feature)) {
            return;
        }

        if (this.features.contains(feature)) {
            return;
        }

        this.features.add(feature);

        // TODO: get device config for this provider from DB and decide if feature should be used
        this.onStart(feature, this.service);

        if (this.features.size() == 1) {
            this.onCreate(this.service);
        }
    }

    public void stop(@NonNull FT feature) {
        if (!this.featureType.isInstance(feature)) {
            return;
        }

        if (this.features.remove(feature)) {
            this.onStop(feature, this.service);
        }

        if (this.features.isEmpty()) {
            this.onDispose(this.service);
        }
    }

    protected abstract void onStop(@NonNull FT feature, @NonNull DeviceService service);
    protected abstract void onStart(@NonNull FT feature, @NonNull DeviceService service);
    protected abstract void onCreate(@NonNull DeviceService service);
    protected abstract void onDispose(@NonNull DeviceService service);

    // TODO: Move this to a ProviderType enum or maybe even better FeatureType?
    @NonNull
    public static DeviceDataProvider createFrom(@NonNull Class providerType, @NonNull DeviceService service) {
        if (providerType.equals(GpsDataProvider.class)) {
            return new GpsDataProvider(service);
        }
        if (providerType.equals(CanDataProvider.class)) {
            return new CanDataProvider(service);
        }
        if (providerType.equals(CarduinoEventProvider.class)) {
            return new CarduinoEventProvider(service);
        }
        if (providerType.equals(ErrorDataProvider.class)) {
            return new ErrorDataProvider(service);
        }

        throw new IllegalArgumentException("Provider \"" + providerType.getSimpleName() + "\" is not registered in DeviceDataProvider.");
    }

}
