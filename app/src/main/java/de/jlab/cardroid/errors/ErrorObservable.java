package de.jlab.cardroid.errors;

import de.jlab.cardroid.devices.ObservableFeature;

public interface ErrorObservable extends ObservableFeature<ErrorObservable.ErrorListener> {

    interface ErrorListener extends ObservableFeature.Listener {
        void onError(int errorNumber);
    }

}
