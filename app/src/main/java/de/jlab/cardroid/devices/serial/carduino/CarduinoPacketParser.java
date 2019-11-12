package de.jlab.cardroid.devices.serial.carduino;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.serial.SerialReader;

public abstract class CarduinoPacketParser implements SerialReader.SerialPacketListener<CarduinoSerialPacket> {

    @Override
    public final void onReceivePackets(@NonNull CarduinoSerialPacket[] packets) {
        for (CarduinoSerialPacket packet : packets) {
            if (this.shouldHandlePacket(packet)) {
                this.handlePacket(packet);
            }
        }
    }

    protected abstract boolean shouldHandlePacket(@NonNull CarduinoSerialPacket packet);
    protected abstract void handlePacket(@NonNull CarduinoSerialPacket packet);

}
