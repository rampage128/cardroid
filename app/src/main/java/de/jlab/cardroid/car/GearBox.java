package de.jlab.cardroid.car;

import de.jlab.cardroid.usb.CarSystemSerialPacket;

public class GearBox extends CarSystem {

    public static final String GEAR = "gear";
    public static final String SYNCHRO_REV = "synchro_rev";

    @Override
    protected void registerProperties() {
        registerProperty(GEAR, (byte)0);
        registerProperty(SYNCHRO_REV, false);
    }

    @Override
    protected void updateDataFromPacket(CarSystemSerialPacket packet) {
        updateProperty(GEAR, packet.readByte(0));
        updateProperty(SYNCHRO_REV, packet.readFlag(1, 7));
    }

 }
