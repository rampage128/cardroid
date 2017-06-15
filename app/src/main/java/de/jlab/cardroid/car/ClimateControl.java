package de.jlab.cardroid.car;

import de.jlab.cardroid.usb.CarSystemSerialPacket;

public class ClimateControl extends ManageableCarSystem {

    private boolean isAcOn = false;
    private boolean isAuto = false;
    private boolean isRecirculation = false;
    private boolean isWindshieldHeating = false;
    private boolean isRearWindowHeating = false;
    private boolean isDuctFeetActive = false;
    private boolean isDuctFaceActive = false;
    private boolean isDuctWindshieldActive = false;
    private byte fanLevel = 0;
    private float temperature = 0;

    @Override
    public void updateDataFromPacket(CarSystemSerialPacket packet) {
        this.fanLevel = packet.readByte(1);
        this.temperature = packet.readByte(2) / 2f;

        this.isAuto = packet.readFlag(0, 6);
        this.isAcOn = packet.readFlag(0, 7);
        this.isRecirculation = packet.readFlag(0, 0);
        this.isWindshieldHeating = packet.readFlag(0, 2);
        this.isRearWindowHeating = packet.readFlag(0, 1);

        this.isDuctWindshieldActive = packet.readFlag(0, 5);
        this.isDuctFaceActive = packet.readFlag(0, 4);
        this.isDuctFeetActive = packet.readFlag(0, 3);
    }

    public boolean isAcOn() {
        return isAcOn;
    }

    public boolean isAuto() {
        return isAuto;
    }

    public boolean isRecirculation() {
        return isRecirculation;
    }

    public boolean isWindshieldHeating() {
        return isWindshieldHeating;
    }

    public boolean isRearWindowHeating() {
        return isRearWindowHeating;
    }

    public boolean isDuctFeetActive() {
        return isDuctFeetActive;
    }

    public boolean isDuctFaceActive() {
        return isDuctFaceActive;
    }

    public boolean isDuctWindshieldActive() {
        return isDuctWindshieldActive;
    }

    public byte getFanLevel() {
        return fanLevel;
    }

    public float getTemperature() {
        return temperature;
    }


    public void pushAcButton() {
        this.manage(CarSystemEvent.CC_AC_BUTTON);
    }

    public void pushAutoButton() {
        this.manage(CarSystemEvent.CC_AUTO_BUTTON);
    }

    public void pushRecirculationButton() {
        this.manage(CarSystemEvent.CC_RECIRCULATION_BUTTON);
    }

    public void pushWindshieldHeatingButton() {
        this.manage(CarSystemEvent.CC_WSH_BUTTON);
    }

    public void pushRearWindowHeatingButton() {
        this.manage(CarSystemEvent.CC_RWH_BUTTON);
    }

    public void pushModeButton() {
        this.manage(CarSystemEvent.CC_MODE_BUTTON);
    }

    public void setTemperature(float temperature) {
        this.manage(CarSystemEvent.CC_TEMPERATURE, new byte[] { (byte)(temperature * 2) });
    }

    public void setFanLevel(byte level) {
        this.manage(CarSystemEvent.CC_FAN_LEVEL, new byte[] { level });
    }
}
