package de.jlab.cardroid.car;

import de.jlab.cardroid.usb.carduino.SerialCarButtonEventPacket;

enum CarSystemEvent {
    CC_OFF_BUTTON(0x01),
    CC_AC_BUTTON(0x02),
    CC_AUTO_BUTTON(0x03),
    CC_RECIRCULATION_BUTTON(0x04),
    CC_WSH_BUTTON(0x05),
    CC_RWH_BUTTON(0x06),
    CC_MODE_BUTTON(0x07),
    CC_TEMPERATURE(0x08),
    CC_FAN_LEVEL(0x09);

    private byte command;

    CarSystemEvent(int command) {
        this.command = (byte)command;
    }

    public static SerialCarButtonEventPacket serialize(CarSystemEvent event, byte[] payload) {
        return new SerialCarButtonEventPacket(event.command, payload);
    }

}
