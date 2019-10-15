package de.jlab.cardroid.errors;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceDataObservable;

public interface ErrorObservable extends DeviceDataObservable {

    void addErrorListener(@NonNull ErrorListener listener);
    void removeErrorListener(@NonNull ErrorListener listener);

    interface ErrorListener {
        void onError(int errorNumber);
    }

}
