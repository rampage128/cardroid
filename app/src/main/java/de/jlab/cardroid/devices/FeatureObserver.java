package de.jlab.cardroid.devices;


import androidx.annotation.NonNull;

public interface FeatureObserver<FT extends Feature> {
    void onFeatureAvailable(@NonNull FT feature);
    void onFeatureUnavailable(@NonNull FT feature);
}
