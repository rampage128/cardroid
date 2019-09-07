package de.jlab.cardroid.car.systems;

import de.jlab.cardroid.car.CarSystem;
import de.jlab.cardroid.usb.carduino.serial.CarSystemSerialPacket;

public class Doors extends CarSystem {

    public static final String IS_FRONT_LEFT_OPEN = "is_front_left_open";
    public static final String IS_FRONT_RIGHT_OPEN = "is_front_right_open";
    public static final String IS_REAR_LEFT_OPEN = "is_rear_left_open";
    public static final String IS_REAR_RIGHT_OPEN = "is_rear_right_open";
    public static final String IS_TRUNK_OPEN = "is_trunk_open";
    public static final String IS_HOOD_OPEN = "is_hood_open";

    @Override
    protected void registerProperties() {
        registerProperty(IS_FRONT_LEFT_OPEN, false);
        registerProperty(IS_FRONT_RIGHT_OPEN, false);
        registerProperty(IS_REAR_LEFT_OPEN, false);
        registerProperty(IS_REAR_RIGHT_OPEN, false);
        registerProperty(IS_TRUNK_OPEN, false);
        registerProperty(IS_HOOD_OPEN, false);
    }

    @Override
    protected void updateDataFromPacket(CarSystemSerialPacket packet) {
        updateProperty(IS_FRONT_LEFT_OPEN, packet.readFlag(0, 7));
        updateProperty(IS_FRONT_RIGHT_OPEN, packet.readFlag(0, 6));
        updateProperty(IS_REAR_LEFT_OPEN, packet.readFlag(0, 5));
        updateProperty(IS_REAR_RIGHT_OPEN, packet.readFlag(0, 4));
        updateProperty(IS_TRUNK_OPEN, packet.readFlag(0, 3));
        updateProperty(IS_HOOD_OPEN, packet.readFlag(0, 2));
    }
}
