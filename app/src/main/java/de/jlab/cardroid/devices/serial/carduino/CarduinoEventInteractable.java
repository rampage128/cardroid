package de.jlab.cardroid.devices.serial.carduino;

import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDevice;

public final class CarduinoEventInteractable implements EventInteractable {

    private CarduinoUsbDevice device;

    @Override
    public void sendEvent(byte eventId, byte[] payload) {
        this.device.send(CarduinoPacketType.createPacket(CarduinoPacketType.EVENT, eventId, payload));
    }

    @Override
    public void setDevice(Device device) {
        this.device = (CarduinoUsbDevice)device;
    }

    @Override
    public Device getDevice() {
        return this.device;
    }
}
