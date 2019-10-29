package de.jlab.cardroid.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.identification.DeviceUid;

public final class FeatureFilter<FT extends Feature> implements FeatureObserver<FT> {

    private Class<FT> featureType;
    private DeviceUid deviceUid;

    private FeatureAvailableListener<FT> featureAvailableListener;
    private FeatureUnavailableListener<FT> featureUnavailableListener;

    public FeatureFilter(@NonNull Class<FT> featureType, @Nullable DeviceUid deviceUid, @Nullable FeatureAvailableListener<FT> featureAvailableListener, @Nullable FeatureUnavailableListener<FT> featureUnavailableListener) {
        this.featureType = featureType;
        this.deviceUid = deviceUid;
        this.featureAvailableListener = featureAvailableListener;
        this.featureUnavailableListener = featureUnavailableListener;
    }

    @Override
    public void onFeatureAvailable(@NonNull FT feature) {
        if (this.featureType.isInstance(feature) && (this.deviceUid == null || feature.getDevice().isDevice(this.deviceUid))) {
            this.featureAvailableListener.onFeatureAvailable(this.featureType.cast(feature));
        }
    }

    @Override
    public void onFeatureUnavailable(@NonNull FT feature) {
        if (this.featureType.isInstance(feature) && (this.deviceUid == null || feature.getDevice().isDevice(this.deviceUid)) && this.featureUnavailableListener != null) {
            this.featureUnavailableListener.onFeatureUnavailable(this.featureType.cast(feature));
        }
    }

    public interface FeatureAvailableListener<FT extends Feature> {
        void onFeatureAvailable(FT feature);
    }

    public interface FeatureUnavailableListener<FT extends Feature> {
        void onFeatureUnavailable(FT feature);
    }

}
