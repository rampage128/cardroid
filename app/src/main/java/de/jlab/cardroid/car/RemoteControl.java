package de.jlab.cardroid.car;

import de.jlab.cardroid.usb.CarSystemSerialPacket;

@Deprecated
public class RemoteControl extends CarSystem {
    private int buttonId;

    @Override
    protected void updateDataFromPacket(CarSystemSerialPacket packet) {
        this.buttonId = packet.readUnsignedByte(0);
    }

    public int getButtonId() {
        return this.buttonId;
    }
}
