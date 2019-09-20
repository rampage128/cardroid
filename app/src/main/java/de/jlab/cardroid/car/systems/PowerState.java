package de.jlab.cardroid.car.systems;

import de.jlab.cardroid.car.CarSystem;
import de.jlab.cardroid.usb.carduino.serial.CarSystemSerialPacket;

public class PowerState extends CarSystem {

    public static final String IS_ACCESSORY_ON = "is_accessory_on";
    public static final String IS_IGNITION_ON = "is_ignition_on";

    @Override
    protected void registerProperties() {
        registerProperty(IS_ACCESSORY_ON, false);
        registerProperty(IS_IGNITION_ON, false);
    }

    @Override
    protected void updateDataFromPacket(CarSystemSerialPacket packet) {
        updateProperty(IS_ACCESSORY_ON, packet.readFlag(0, 7));
        updateProperty(IS_IGNITION_ON, packet.readFlag(0, 6));
    }
}
