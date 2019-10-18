package de.jlab.cardroid.devices.serial.carduino;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import de.jlab.cardroid.car.CanObservable;
import de.jlab.cardroid.car.CanPacket;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDeviceHandler;

public final class CarduinoCanParser extends CarduinoPacketParser implements CanObservable {

    private static final int CANID_OFFSET = 0;
    private static final int DATA_OFFSET = 4;

    private CarduinoUsbDeviceHandler device;
    private ArrayList<CanObservable.CanPacketListener> listeners = new ArrayList<>();
    private LongSparseArray<CanPacket> canPackets = new LongSparseArray<>();

    public void addCanListener(CanObservable.CanPacketListener listener) {
        this.listeners.add(listener);
    }

    public void removeCanListener(CanObservable.CanPacketListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void setDevice(@NonNull DeviceHandler device) {
        this.device = (CarduinoUsbDeviceHandler)device;
    }

    @Nullable
    @Override
    public DeviceHandler getDevice() {
        return this.device;
    }

    @Override
    protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
        return CarduinoPacketType.CAN.equals(packet.getPacketType());
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
