package de.jlab.cardroid.car;

import android.util.Log;

import java.util.HashMap;

import androidx.annotation.NonNull;
import de.jlab.cardroid.usb.carduino.serial.CarSystemSerialPacket;
import de.jlab.cardroid.usb.carduino.CarduinoService;
import de.jlab.cardroid.usb.carduino.serial.SerialPacket;

public class Car implements CarduinoService.PacketHandler<CarSystemSerialPacket> {

    private HashMap<CarSystemFactory, CarSystem> systemLookup = new HashMap<>();

    @Override
    public void handleSerialPacket(@NonNull CarSystemSerialPacket packet) {
        try {
            CarSystemFactory systemType = CarSystemFactory.getType(packet);
            CarSystem system = getCarSystem(systemType);
            system.updateFromPacket(packet);
        } catch (UnknownCarSystemException e) {
            Log.e("CarSystem", "Can not update car system!", e);
        }
    }

    @Override
    public boolean shouldHandlePacket(@NonNull SerialPacket packet) {
        return packet instanceof CarSystemSerialPacket;
    }

    public CarSystem getCarSystem(CarSystemFactory systemType) {
        CarSystem system = this.systemLookup.get(systemType);
        if (system == null) {
            system = CarSystemFactory.getCarSystem(systemType);
            this.systemLookup.put(systemType, system);
        }
        return system;
    }

    public int getCarSystemCount() {
        return this.systemLookup.size();
    }

}
