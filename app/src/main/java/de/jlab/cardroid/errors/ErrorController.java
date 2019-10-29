package de.jlab.cardroid.errors;

import android.content.Context;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceController;
import de.jlab.cardroid.devices.FeatureFilter;

public final class ErrorController {

    private SparseArray<Error> errors = new SparseArray<>();

    private DeviceController deviceController;
    private ErrorNotifier errorNotifier;

    private FeatureFilter<ErrorObservable> errorFilter = new FeatureFilter<>(ErrorObservable.class, null, this::onFeatureAvailable, this::onFeatureUnavailable);
    private ErrorObservable.ErrorListener errorListener = this::onError;


    public ErrorController(@NonNull DeviceController deviceController, @NonNull Context context) {
        this.deviceController = deviceController;
        this.errorNotifier = new ErrorNotifier(context);
        deviceController.addSubscriber(this.errorFilter, ErrorObservable.class);
    }

    public void dispose() {
        this.deviceController.removeSubscriber(this.errorFilter);
    }

    private void onError(int errorNumber) {
        Error error = this.errors.get(errorNumber);
        if (error == null) {
            error = new Error(errorNumber);
            this.errors.put(errorNumber, error);
        }
        error.count(errorNumber);
        this.errorNotifier.onError(error);
    }

    private void onFeatureAvailable(@NonNull ErrorObservable feature) {
        feature.addListener(this.errorListener);
    }

    private void onFeatureUnavailable(@NonNull ErrorObservable feature) {
        feature.removeListener(this.errorListener);
    }

}
