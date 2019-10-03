package de.jlab.cardroid.devices.serial.carduino;

import android.util.Log;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDeviceHandler;

public final class CarduinoEventProvider extends DeviceDataProvider<CarduinoUsbDeviceHandler> {

    private ArrayList<CarduinoEventParser.EventListener> externalListeners = new ArrayList<>();

    private CarduinoEventParser.EventListener listener = eventNum -> {
        for (int i = 0; i < this.externalListeners.size(); i++) {
            this.externalListeners.get(i).onEvent(eventNum);
        }
    };

    public CarduinoEventProvider(@NonNull DeviceService service) {
        super(service);
    }

    public void sendEvent(@NonNull CarduinoEventType event, @Nullable byte[] payload) {
        CarduinoSerialPacket eventPacket = CarduinoEventType.createPacket(event, payload);

        ArrayList<CarduinoUsbDeviceHandler> devices = this.getDevices();
        for (int i = 0; i < devices.size(); i++) {
            CarduinoUsbDeviceHandler device = devices.get(i);
            try {
                device.send(eventPacket);
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Error sending event \"" + event.getClass().getSimpleName() + "\" to device \"" + device.getDeviceId() + "\".");
            }
        }
    }

    public void subscribe(CarduinoEventParser.EventListener listener) {
        this.externalListeners.add(listener);
    }

    public void unsubscribe(CarduinoEventParser.EventListener listener) {
        this.externalListeners.remove(listener);
    }

    @Override
    protected void onUpdate(@NonNull CarduinoUsbDeviceHandler previousDevice, @NonNull CarduinoUsbDeviceHandler newDevice, @NonNull DeviceService service) {
        previousDevice.removeEventListener(this.listener);
        newDevice.addEventListener(this.listener);
    }

    @Override
    protected void onStop(@NonNull CarduinoUsbDeviceHandler device, @NonNull DeviceService service) {
        device.removeEventListener(this.listener);
    }

    @Override
    protected void onStart(@NonNull CarduinoUsbDeviceHandler device, @NonNull DeviceService service) {
        device.addEventListener(this.listener);
    }

}
