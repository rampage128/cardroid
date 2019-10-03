package de.jlab.cardroid.devices.serial.carduino;

import java.util.ArrayList;

public final class CarduinoEventParser extends CarduinoPacketParser {

    private ArrayList<EventListener> listeners = new ArrayList<>();

    @Override
    protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
        return CarduinoPacketType.EVENT.equals(packet.getPacketType());
    }

    @Override
    protected void handlePacket(CarduinoSerialPacket packet) {
        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).onEvent(packet.getPacketId());
        }
    }

    public void addEventListener(EventListener listener) {
        this.listeners.add(listener);
    }

    public void removeEventListener(EventListener listener) {
        this.listeners.remove(listener);
    }

    public interface EventListener {
        void onEvent(int eventNum);
    }

}
