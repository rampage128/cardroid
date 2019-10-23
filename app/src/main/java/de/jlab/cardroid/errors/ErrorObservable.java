package de.jlab.cardroid.errors;

import de.jlab.cardroid.devices.FeatureDataProvider;
import de.jlab.cardroid.devices.ObservableFeature;

public interface ErrorObservable extends ObservableFeature<ErrorObservable.ErrorListener> {

    @Override
    default Class<? extends FeatureDataProvider> getProviderClass() {
        return ErrorDataProvider.class;
    }

    interface ErrorListener extends ObservableFeature.Listener {
        void onError(int errorNumber);
    }

}
