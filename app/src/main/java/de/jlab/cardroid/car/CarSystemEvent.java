package de.jlab.cardroid.car;

import de.jlab.cardroid.usb.SerialCommandPacket;

enum CarSystemEvent {
    CC_AC_BUTTON(0x01),
    CC_AUTO_BUTTON(0x02),
    CC_RECIRCULATION_BUTTON(0x03),
    CC_WSH_BUTTON(0x04),
    CC_RWH_BUTTON(0x05),
    CC_MODE_BUTTON(0x06),
    CC_TEMPERATURE(0x07),
    CC_FAN_LEVEL(0x08);

    private byte command;

    CarSystemEvent(int command) {
        this.command = (byte)command;
    }

    public static SerialCommandPacket serialize(CarSystemEvent event, byte[] payload) {
        return new SerialCommandPacket(event.command, payload);
    }

}
