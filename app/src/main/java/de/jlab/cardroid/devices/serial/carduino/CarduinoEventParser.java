package de.jlab.cardroid.devices.serial.carduino;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.Device;

public final class CarduinoEventParser extends CarduinoPacketParser implements EventObservable {

    private Device device;
    private ArrayList<EventListener> listeners = new ArrayList<>();

    @Override
    protected boolean shouldHandlePacket(@NonNull CarduinoSerialPacket packet) {
        return CarduinoPacketType.EVENT.equals(packet.getPacketType());
    }

    @Override
    protected void handlePacket(@NonNull CarduinoSerialPacket packet) {
        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).onEvent(packet.getPacketId(), this.device.getDeviceUid());
        }
    }

    @Override
    public void addListener(@NonNull EventListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(@NonNull EventListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void setDevice(@NonNull Device device) {
        this.device = device;
    }

    @Nullable
    @Override
    public Device getDevice() {
        return this.device;
    }
}
