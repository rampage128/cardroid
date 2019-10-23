package de.jlab.cardroid.errors;

import android.util.SparseArray;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.FeatureDataProvider;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.providers.DataProviderService;

public final class ErrorDataProvider extends FeatureDataProvider {

    private SparseArray<Error> errors = new SparseArray<>();

    private ErrorNotifier errorNotifier;

    private ArrayList<ErrorListener> externalListeners = new ArrayList<>();
    private ErrorObservable.ErrorListener errorListener = errorNumber -> {
        Error error = this.errors.get(errorNumber);
        if (error == null) {
            error = new Error(errorNumber);
            this.errors.put(errorNumber, error);
        }
        error.count(errorNumber);

        //Error[] errors = this.errors.values().toArray(new Error[0]);
        //Arrays.sort(errors, (error1, error2) -> (int)(error1.getLastOccurence() - error2.getLastOccurence()));

        for (int i = 0; i < this.externalListeners.size(); i++) {
            this.externalListeners.get(i).onError(error);
        }
    };

    public ErrorDataProvider(@NonNull DataProviderService service) {
        super(service);

        this.errorNotifier = new ErrorNotifier(service);
        this.addErrorListener(this.errorNotifier);
    }

    public void addErrorListener(ErrorListener listener) {
        this.externalListeners.add(listener);
    }

    public void removeErrorListener(ErrorListener listener) {
        this.externalListeners.remove(listener);
    }

    private void deviceRemoved(@NonNull DeviceHandler device) {
        ErrorObservable observable = device.getObservable(ErrorObservable.class);
        if (observable != null) {
            observable.removeErrorListener(this.errorListener);
        }
    }

    private void deviceAdded(@NonNull DeviceHandler device) {
        ErrorObservable observable = device.getObservable(ErrorObservable.class);
        if (observable != null) {
            observable.addErrorListener(this.errorListener);
        }
    }

    @Override
    protected void onUpdate(@NonNull DeviceHandler previousDevice, @NonNull DeviceHandler newDevice, @NonNull DeviceService service) {
        this.deviceRemoved(previousDevice);
        this.deviceAdded(newDevice);
    }

    @Override
    protected void onStop(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        this.deviceRemoved(device);
    }

    @Override
    protected void onStart(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        this.deviceAdded(device);
    }

    public interface ErrorListener {
        void onError(Error error);
    }

}
