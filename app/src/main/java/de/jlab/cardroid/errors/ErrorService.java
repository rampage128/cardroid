package de.jlab.cardroid.errors;

import android.content.Intent;
import android.os.IBinder;
import android.util.SparseArray;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.FeatureObserver;
import de.jlab.cardroid.service.FeatureService;

public final class ErrorService extends FeatureService implements FeatureObserver<ErrorObservable> {

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

        this.errorNotifier.onError(error);
        for (int i = 0; i < this.externalListeners.size(); i++) {
            this.externalListeners.get(i).onError(error);
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        this.errorNotifier = new ErrorNotifier(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onDeviceServiceConnected(DeviceService.DeviceServiceBinder service) {
        service.subscribe(this, ErrorObservable.class);
    }

    @Override
    protected void onDeviceServiceDisconnected() {
        this.stopSelf();
    }

    @Override
    protected ArrayList<Class<? extends Feature>> tieLifecycleToFeatures() {
        return new ArrayList<Class<? extends Feature>>() {{
            add(ErrorObservable.class);
        }};
    }

    @Override
    public void onFeatureAvailable(@NonNull ErrorObservable feature) {
        feature.addListener(errorListener);
    }

    @Override
    public void onFeatureUnavailable(@NonNull ErrorObservable feature) {
        feature.removeListener(errorListener);
    }

    public interface ErrorListener {
        void onError(Error error);
    }

}
