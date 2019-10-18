package de.jlab.cardroid.devices.serial.carduino;

import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDeviceHandler;

public final class CarduinoEventInteractable implements EventInteractable {

    private CarduinoUsbDeviceHandler device;

    @Override
    public void sendEvent(byte eventId, byte[] payload) {
        this.device.send(CarduinoPacketType.createPacket(CarduinoPacketType.EVENT, eventId, payload));
    }

    @Override
    public void setDevice(DeviceHandler device) {
        this.device = (CarduinoUsbDeviceHandler)device;
    }

    @Override
    public DeviceHandler getDevice() {
        return this.device;
    }
}
