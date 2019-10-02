package de.jlab.cardroid.errors;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.carduino.CarduinoErrorParser;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDeviceHandler;

public final class ErrorDataProvider extends DeviceDataProvider<CarduinoUsbDeviceHandler> {

    private LinkedHashMap<Integer, Error> errors = new LinkedHashMap<>();

    private ErrorNotifier errorNotifier;

    private ArrayList<ErrorListener> externalListeners = new ArrayList<>();
    private CarduinoErrorParser.ErrorListener errorListener = errorNumber -> {
        Error error = this.errors.get(errorNumber);
        if (error == null) {
            error = new Error(errorNumber);
            this.errors.put(errorNumber, error);
        }
        error.count(errorNumber);

        for (int i = 0; i < this.externalListeners.size(); i++) {
            this.externalListeners.get(i).onError(error);
        }
    };

    public void addErrorListener(ErrorListener listener) {
        this.externalListeners.add(listener);
    }

    public void removeErrorListener(ErrorListener listener) {
        this.externalListeners.remove(listener);
    }

    public ErrorDataProvider(@NonNull DeviceService service) {
        super(service);

        this.errorNotifier = new ErrorNotifier(service);
    }

    @Override
    protected void onUpdate(@NonNull CarduinoUsbDeviceHandler previousDevice, @NonNull CarduinoUsbDeviceHandler newDevice, @NonNull DeviceService service) {
        previousDevice.removeErrorListener(this.errorListener);
        newDevice.addErrorListener(this.errorListener);
    }

    @Override
    protected void onStop(@NonNull CarduinoUsbDeviceHandler device, @NonNull DeviceService service) {
        device.removeErrorListener(this.errorListener);
        this.removeErrorListener(this.errorNotifier);
    }

    @Override
    protected void onStart(@NonNull CarduinoUsbDeviceHandler device, @NonNull DeviceService service) {
        device.addErrorListener(this.errorListener);
        this.addErrorListener(this.errorNotifier);
    }

    public interface ErrorListener {
        void onError(Error error);
    }

}
