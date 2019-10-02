package de.jlab.cardroid.devices.serial.carduino;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum CarduinoPacketType {
    META(0x61),
    CAN(0x62),
    EVENT(0x63),
    ERROR(0x65);

    private byte type;

    CarduinoPacketType(int type) {
        this.type = (byte)type;
    }

    public boolean equals(byte type) {
        return this.type == type;
    }

    public static CarduinoSerialPacket createPacket(@NonNull CarduinoPacketType type, byte id, @Nullable byte[] payload) {
        return CarduinoSerialPacket.create(type.type, id, payload);
    }
}
