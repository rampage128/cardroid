package de.jlab.cardroid.devices.usb.serial.carduino;

import android.util.Log;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import de.jlab.cardroid.car.CanInteractable;
import de.jlab.cardroid.car.CanPacketDescriptor;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.serial.carduino.CarduinoMetaType;
import de.jlab.cardroid.devices.serial.carduino.CarduinoPacketType;

public final class CarduinoCanInteractable implements CanInteractable {

    private CarduinoUsbDeviceHandler device;

    @Override
    public void registerCanId(@NonNull CanPacketDescriptor descriptor) {
        this.sendCanIdRequest(descriptor.getCanId(), descriptor.getByteMask());
    }

    @Override
    public void unregisterCanId(@NonNull CanPacketDescriptor descriptor) {
        this.sendCanIdRequest(descriptor.getCanId(), (byte)0x00);
    }

    @Override
    public void sendPacket(int canId, byte[] data) {
        ByteBuffer payload = ByteBuffer.allocate(4 + data.length).putInt(canId).put(data);
        this.device.send(CarduinoPacketType.createPacket(CarduinoPacketType.CAN, (byte)0x00, payload.array()));
    }

    @Override
    public void startSniffer() {
        this.device.send(CarduinoMetaType.createPacket(CarduinoMetaType.START_SNIFFING, null));
    }

    @Override
    public void stopSniffer() {
        this.device.send(CarduinoMetaType.createPacket(CarduinoMetaType.STOP_SNIFFING, null));
    }

    @Override
    public void setDevice(@NonNull DeviceHandler device) {
        this.device = (CarduinoUsbDeviceHandler)device;
    }

    @Override
    public DeviceHandler getDevice() {
        return this.device;
    }

    private void sendCanIdRequest(long canId, byte mask) {
        byte[] payload = ByteBuffer.allocate(5).putInt((int)canId).put(mask).array();
        Log.d(this.getClass().getSimpleName(), "Send request " + String.format("%02x", canId) + " to device " + this.device + ".");
        this.device.send(CarduinoMetaType.createPacket(CarduinoMetaType.CAR_DATA_DEFINITION, payload));
    }

}
