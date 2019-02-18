package de.jlab.cardroid.car;

import de.jlab.cardroid.usb.CarSystemSerialPacket;

public class GearBox extends CarSystem {

    private int gear;
    private boolean isSynchroRev;

    public int getGear() {
        return this.gear;
    }

    public boolean isSynchroRev() {
        return this.isSynchroRev;
    }

    @Override
    public void updateDataFromPacket(CarSystemSerialPacket packet) {
        this.gear           = packet.readByte(0);
        this.isSynchroRev   = packet.readFlag(1, 7);
    }
}
