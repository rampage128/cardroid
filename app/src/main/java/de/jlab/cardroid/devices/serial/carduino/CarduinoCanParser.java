package de.jlab.cardroid.devices.serial.carduino;

import java.util.ArrayList;

import androidx.collection.LongSparseArray;
import de.jlab.cardroid.devices.serial.can.CanDeviceHandler;
import de.jlab.cardroid.devices.serial.can.CanPacket;

public final class CarduinoCanParser extends CarduinoPacketParser {

    private static final int CANID_OFFSET = 0;
    private static final int DATA_OFFSET = 4;

    private ArrayList<CanDeviceHandler.CanPacketListener> listeners = new ArrayList<>();
    private LongSparseArray<CanPacket> canPackets = new LongSparseArray<>();

    public void addCanListener(CanDeviceHandler.CanPacketListener listener) {
        this.listeners.add(listener);
    }

    public void removeCanListener(CanDeviceHandler.CanPacketListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
        return CarduinoPacketType.CAN.equals(packet.getPacketId());
    }

    @Override
    protected void handlePacket(CarduinoSerialPacket packet) {
        long canId = packet.readDWord(CANID_OFFSET);
        byte[] data = packet.readBytes(DATA_OFFSET);
        CanPacket canPacket = this.canPackets.get(canId);
        if (canPacket == null) {
            canPacket = new CanPacket(canId, data);
            this.canPackets.put(canId, canPacket);
        } else {
            canPacket.updateData(data);
        }

        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).onReceive(canPacket);
        }
    }

}
