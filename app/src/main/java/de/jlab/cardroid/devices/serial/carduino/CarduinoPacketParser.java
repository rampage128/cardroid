package de.jlab.cardroid.devices.serial.carduino;

import de.jlab.cardroid.devices.serial.SerialReader;

public abstract class CarduinoPacketParser implements SerialReader.SerialPacketListener<CarduinoSerialPacket> {

    @Override
    public final void onReceivePackets(CarduinoSerialPacket[] packets) {
        for (CarduinoSerialPacket packet : packets) {
            if (this.shouldHandlePacket(packet)) {
                this.handlePacket(packet);
            }
        }
    }

    protected abstract boolean shouldHandlePacket(CarduinoSerialPacket packet);
    protected abstract void handlePacket(CarduinoSerialPacket packet);

}
