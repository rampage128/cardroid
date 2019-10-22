package de.jlab.cardroid.devices.serial.carduino;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoCanInteractable;

public enum CarduinoPacketType {
    META(0x61),
    // TODO: Split CarduinoCanInteractable into CanMessageInteractable and CanFilterInteractable
    CAN(0x62, new CarduinoCanParser(), new CarduinoCanInteractable()),
    EVENT(0x63, new CarduinoEventParser(), new CarduinoEventInteractable()),
    ERROR(0x65, new CarduinoErrorParser());

    private byte type;
    private Feature[] features;

    CarduinoPacketType(int type, @Nullable Feature... features) {
        this.type = (byte)type;
        this.features = features;
    }

    public byte getType() {
        return this.type;
    }

    @Nullable
    public Feature[] getFeatures() {
        return this.features;
    }

    public boolean equals(byte type) {
        return this.type == type;
    }

    public static CarduinoPacketType getFromPacket(CarduinoSerialPacket packet) {
        for (CarduinoPacketType type : CarduinoPacketType.values()) {
            if (type.equals(packet.getPacketType())) {
                return type;
            }
        }
        return null;
    }

    @NonNull
    public static CarduinoSerialPacket createPacket(@NonNull CarduinoPacketType type, byte id, @Nullable byte[] payload) {
        return CarduinoSerialPacket.create(type.type, id, payload);
    }
}
