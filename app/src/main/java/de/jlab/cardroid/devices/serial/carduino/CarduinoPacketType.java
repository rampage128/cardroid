package de.jlab.cardroid.devices.serial.carduino;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.devices.DeviceDataObservable;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.Interactable;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoCanInteractable;
import de.jlab.cardroid.errors.ErrorDataProvider;

public enum CarduinoPacketType {
    META(0x61, null, null, null),
    CAN(0x62, CanDataProvider.class, new CarduinoCanParser(), new CarduinoCanInteractable()),
    EVENT(0x63, CarduinoEventProvider.class, new CarduinoEventParser(), new CarduinoEventInteractable()),
    ERROR(0x65, ErrorDataProvider.class, new CarduinoErrorParser(), null);

    private byte type;
    private Class<? extends DeviceDataProvider> providerType;
    private CarduinoPacketParser parser;
    private Interactable interactable;

    CarduinoPacketType(int type, @Nullable Class<? extends DeviceDataProvider> providerType, @Nullable CarduinoPacketParser parser, @Nullable Interactable interactable) {
        this.type = (byte)type;
        this.providerType = providerType;
        this.parser = parser;
        this.interactable = interactable;
    }

    @Nullable
    public CarduinoPacketParser getParser() {
        return this.parser;
    }

    public byte getType() {
        return this.type;
    }

    @Nullable
    public DeviceDataObservable getObservable() {
        if (this.parser instanceof DeviceDataObservable) {
            return (DeviceDataObservable)this.parser;
        }
        return null;
    }

    @Nullable
    public Interactable getInteractable() {
        return this.interactable;
    }

    @Nullable
    public Class<? extends DeviceDataProvider> getProviderType() {
        return this.providerType;
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
