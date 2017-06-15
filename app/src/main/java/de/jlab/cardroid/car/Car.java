package de.jlab.cardroid.car;

import java.util.HashMap;

import de.jlab.cardroid.usb.CarSystemSerialPacket;
import de.jlab.cardroid.usb.SerialPacket;

public class Car {

    private HashMap<CarSystemFactory, CarSystem> systemLookup = new HashMap<>();

    public void updateFromSerialPacket(SerialPacket packet) throws UnknownCarSystemException {
        if (packet instanceof CarSystemSerialPacket) {
            this.updateSystemFromPacket((CarSystemSerialPacket)packet);
        }
    }

    private void updateSystemFromPacket(CarSystemSerialPacket packet) throws UnknownCarSystemException {
        CarSystemFactory systemType = CarSystemFactory.getType(packet);
        CarSystem system = getCarSystem(systemType);
        system.updateFromPacket(packet);
    }

    public CarSystem getCarSystem(CarSystemFactory systemType) {
        CarSystem system = this.systemLookup.get(systemType);
        if (system == null) {
            system = CarSystemFactory.getCarSystem(systemType);
            this.systemLookup.put(systemType, system);
        }
        return system;
    }

}
