package de.jlab.cardroid.devices.serial.carduino;

import android.util.Log;

import java.util.ArrayList;

import de.jlab.cardroid.devices.DeviceDataObservable;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.Interactable;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDeviceHandler;

public final class CarduinoFeatureDetector extends CarduinoPacketParser {

    private ArrayList<Byte> knownPacketTypes = new ArrayList<>();
    private CarduinoUsbDeviceHandler device;
    private CarduinoSerialReader reader;

    public CarduinoFeatureDetector(CarduinoUsbDeviceHandler device, CarduinoSerialReader reader) {
        this.device = device;
        this.reader = reader;
    }

    @Override
    protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
        return !this.knownPacketTypes.contains(packet.getPacketType());
    }

    @Override
    protected void handlePacket(CarduinoSerialPacket packet) {
        byte rawPacketType = packet.getPacketType();
        this.knownPacketTypes.add(rawPacketType);
        CarduinoPacketType type = CarduinoPacketType.getFromPacket(packet);
        if (type != null) {
            Class<? extends DeviceDataProvider> providerType = type.getProviderType();
            DeviceDataObservable observable = type.getObservable();
            Interactable interactable = type.getInteractable();
            if (providerType != null) {
                this.device.notifyFeatureDetected(providerType, observable, interactable);
            }
            if (observable instanceof CarduinoPacketParser) {
                this.device.addPacketParser((CarduinoPacketParser)observable, this.reader);
            }
        } else {
            Log.e(this.getClass().getSimpleName(), "Device \"" + this.device.getConnectionId() + "\" registered unknown feature: " + new String(new byte[] { rawPacketType }));
        }
    }
}
